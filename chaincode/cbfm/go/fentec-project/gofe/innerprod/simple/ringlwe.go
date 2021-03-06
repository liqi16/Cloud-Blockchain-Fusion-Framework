/*
 * Copyright (c) 2018 XLAB d.o.o
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simple

import (
	"math/big"

	"fmt"

	"github.com/fentec-project/gofe/data"
	gofe "github.com/fentec-project/gofe/internal"
	"github.com/fentec-project/gofe/sample"
	"github.com/pkg/errors"
)

// ringLWEParams represents parameters for the ring LWE scheme.
type ringLWEParams struct {
	l int // Length of data vectors for inner product

	// Main security parameters of the scheme
	n int

	// Settings for discrete gaussian sampler
	sigma *big.Float // standard deviation

	bound *big.Int // upper bound for coordinates of input vectors

	p *big.Int // modulus for the resulting inner product
	q *big.Int // modulus for ciphertext and keys

	// a is a vector with n coordinates.
	// It represents a random polynomial for the scheme.
	a data.Vector
}

// RingLWE represents a scheme instantiated from the LWE problem,
// that is much more efficient than the LWE scheme. It operates in the
// ring of polynomials R = Z[x]/((x^n)+1).
type RingLWE struct {
	params  *ringLWEParams
	sampler *sample.NormalCumulative
}

// NewRingLWE configures a new instance of the scheme.
// It accepts the length of input vectors l, the main security parameter
// n, upper bound for coordinates of input vectors x and y, modulus for the
// inner product p, modulus for ciphertext and keys q, and parameters
// for the sampler: standard deviation sigma, precision eps and a limit
// k for the sampling interval.
//
// Note that the security parameter n must be a power of 2.
// In addition, modulus p must be strictly smaller than l*bound??. If
// any of these conditions is violated, or if public parameters
// for the scheme cannot be generated for some other reason,
// an error is returned.
func NewRingLWE(l, n int, bound, p, q *big.Int, sigma *big.Float) (*RingLWE, error) {
	// Ensure that p >= 2 * l * B?? holds
	bSquared := new(big.Int).Mul(bound, bound)
	lTimesBsquared := new(big.Int).Mul(big.NewInt(int64(l)), bSquared)
	twolTimesBsquared := new(big.Int).Mul(big.NewInt(2), lTimesBsquared)
	if p.Cmp(twolTimesBsquared) < 0 {
		return nil, fmt.Errorf("precondition violated: p >= 2*l*b?? doesn't hold")
	}
	if !isPowOf2(n) {
		return nil, fmt.Errorf("security parameter n is not a power of 2")
	}

	randVec, err := data.NewRandomVector(n, sample.NewUniform(q))
	if err != nil {
		return nil, errors.Wrap(err, "cannot generate random polynomial")
	}

	return &RingLWE{
		params: &ringLWEParams{
			l:     l,
			n:     n,
			bound: bound,
			p:     p,
			q:     q,
			sigma: sigma,
			a:     randVec,
		},
		sampler: sample.NewNormalCumulative(sigma, uint(n), true),
	}, nil
}

// Calculates the center function t(x) = floor(x*q/p) % q for a matrix X.
func (s *RingLWE) center(X data.Matrix) data.Matrix {
	return X.Apply(func(x *big.Int) *big.Int {
		t := new(big.Int)
		t.Mul(x, s.params.q)
		t.Div(t, s.params.p)
		t.Mod(t, s.params.q)

		return t
	})
}

// GenerateSecretKey generates a secret key for the scheme.
// The key is a matrix of l*n small elements sampled from
// Discrete Gaussian distribution.
//
// In case secret key could not be generated, it returns an error.
func (s *RingLWE) GenerateSecretKey() (data.Matrix, error) {
	return data.NewRandomMatrix(s.params.l, s.params.n, s.sampler)
}

// GeneratePublicKey accepts a master secret key SK and generates a
// corresponding master public key.
// Public key is a matrix of l*n elements.
// In case of a malformed secret key the function returns an error.
func (s *RingLWE) GeneratePublicKey(SK data.Matrix) (data.Matrix, error) {
	if !SK.CheckDims(s.params.l, s.params.n) {
		return nil, gofe.MalformedPubKey
	}
	// Generate noise matrix
	// Elements are sampled from the same distribution as the secret key S.
	E, err := data.NewRandomMatrix(s.params.l, s.params.n, s.sampler)
	if err != nil {
		return nil, errors.Wrap(err, "public key generation failed")
	}

	// Calculate public key PK row by row as PKi = (a * SKi + Ei) % q.
	// Multiplication and addition are in the ring of polynomials
	PK := make(data.Matrix, s.params.l)
	for i := 0; i < PK.Rows(); i++ {
		pkI, _ := SK[i].MulAsPolyInRing(s.params.a)
		pkI = pkI.Add(E[i])
		PK[i] = pkI
	}
	PK = PK.Mod(s.params.q)

	return PK, nil
}

// DeriveKey accepts input vector y and master secret key SK, and derives a
// functional encryption key.
// In case of malformed secret key or input vector that violates the
// configured bound, it returns an error.
func (s *RingLWE) DeriveKey(y data.Vector, SK data.Matrix) (data.Vector, error) {
	if err := y.CheckBound(s.params.bound); err != nil {
		return nil, err
	}
	if !SK.CheckDims(s.params.l, s.params.n) {
		return nil, gofe.MalformedSecKey
	}
	// Secret key is a linear combination of input vector y and master secret keys.
	SKTrans := SK.Transpose()
	skY, err := SKTrans.MulVec(y)
	if err != nil {
		return nil, gofe.MalformedInput
	}
	skY = skY.Mod(s.params.q)

	return skY, nil
}

// Encrypt encrypts matrix X using public key PK.
// It returns the resulting ciphertext matrix. In case of malformed
// public key or input matrix that violates the configured bound,
// it returns an error.
//
//The resulting ciphertext has dimensions (l + 1) * n.
func (s *RingLWE) Encrypt(X data.Matrix, PK data.Matrix) (data.Matrix, error) {
	if err := X.CheckBound(s.params.bound); err != nil {
		return nil, err
	}

	if !PK.CheckDims(s.params.l, s.params.n) {
		return nil, gofe.MalformedPubKey
	}
	if !X.CheckDims(s.params.l, s.params.n) {
		return nil, gofe.MalformedInput
	}

	// Create a small random vector r
	r, err := data.NewRandomVector(s.params.n, s.sampler)
	if err != nil {
		return nil, errors.Wrap(err, "error in encrypt")
	}
	// Create noise matrix E to secure the encryption
	E, err := data.NewRandomMatrix(s.params.l, s.params.n, s.sampler)
	if err != nil {
		return nil, errors.Wrap(err, "error in encrypt")
	}
	// Calculate cipher CT row by row as CTi = (PKi * r + Ei) % q.
	// Multiplication and addition are in the ring of polynomials.
	CT0 := make(data.Matrix, s.params.l)
	for i := 0; i < CT0.Rows(); i++ {
		CT0i, _ := PK[i].MulAsPolyInRing(r)
		CT0i = CT0i.Add(E[i])
		CT0[i] = CT0i
	}
	CT0 = CT0.Mod(s.params.q)

	// Include the message X in the encryption
	T := s.center(X)
	CT0, _ = CT0.Add(T)
	CT0 = CT0.Mod(s.params.q)

	// Construct the last row of the cipher
	ct1, _ := s.params.a.MulAsPolyInRing(r)
	e, err := data.NewRandomVector(s.params.n, s.sampler)
	if err != nil {
		return nil, errors.Wrap(err, "error in encrypt")
	}
	ct1 = ct1.Add(e)
	ct1 = ct1.Mod(s.params.q)

	return append(CT0, ct1), nil
}

// Decrypt accepts an encrypted matrix CT, secret key skY, and plaintext
// vector y, and returns a vector of inner products of X's rows and y.
// If decryption failed (for instance with input data that violates the
// configured bound or malformed ciphertext or keys), error is returned.
func (s *RingLWE) Decrypt(CT data.Matrix, skY, y data.Vector) (data.Vector, error) {
	if err := y.CheckBound(s.params.bound); err != nil {
		return nil, err
	}
	if len(skY) != s.params.n {
		return nil, gofe.MalformedDecKey
	}
	if len(y) != s.params.l {
		return nil, gofe.MalformedInput
	}

	if !CT.CheckDims(s.params.l+1, s.params.n) {
		return nil, gofe.MalformedCipher
	}
	CT0 := CT[:s.params.l] // First l rows of cipher
	ct1 := CT[s.params.l]  // Last row of cipher

	CT0Trans := CT0.Transpose()
	CT0TransMulY, _ := CT0Trans.MulVec(y)
	CT0TransMulY = CT0TransMulY.Mod(s.params.q)

	ct1MulSkY, _ := ct1.MulAsPolyInRing(skY)
	ct1MulSkY = ct1MulSkY.Apply(func(x *big.Int) *big.Int {
		return new(big.Int).Neg(x)
	})

	d := CT0TransMulY.Add(ct1MulSkY)
	d = d.Mod(s.params.q)
	halfQ := new(big.Int).Div(s.params.q, big.NewInt(2))

	d = d.Apply(func(x *big.Int) *big.Int {
		if x.Cmp(halfQ) == 1 {
			x.Sub(x, s.params.q)
		}
		x.Mul(x, s.params.p)
		x.Add(x, halfQ)
		x.Div(x, s.params.q)

		return x
	})

	return d, nil
}

func isPowOf2(x int) bool {
	return x&(x-1) == 0
}
