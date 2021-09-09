// Copyright 2012 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Package bn256 implements a particular bilinear group.
//
// Bilinear groups are the basis of many of the new cryptographic protocols
// that have been proposed over the past decade. They consist of a triplet of
// groups (G₁, G₂ and GT) such that there exists a function e(g₁ˣ,g₂ʸ)=gTˣʸ
// (where gₓ is a generator of the respective group). That function is called
// a pairing function.
//
// This package specifically implements the Optimal Ate pairing over a 256-bit
// Barreto-Naehrig curve as described in
// http://cryptojedi.org/papers/dclxvi-20100714.pdf. Its output is compatible
// with the implementation described in that paper.
//
// (This package previously claimed to operate at a 128-bit security level.
// However, recent improvements in attacks mean that is no longer true. See
// https://moderncrypto.org/mail-archive/curves/2016/000740.html.)
package bn256

import (
	"crypto/rand"
	"crypto/sha256"
	"fmt"
	"io"
	"math/big"
)

// BUG(agl): this implementation is not constant time.
// TODO(agl): keep GF(p²) elements in Mongomery form.

// G1 is an abstract cyclic group. The zero value is suitable for use as the
// output of an operation, but cannot be used as an input.
type G1 struct {
	p *curvePoint
}

// RandomG1 returns x and g₁ˣ where x is a random, non-zero number read from r.
func RandomG1(r io.Reader) (*big.Int, *G1, error) {
	var k *big.Int
	var err error

	for {
		k, err = rand.Int(r, Order)
		if err != nil {
			return nil, nil, err
		}
		if k.Sign() > 0 {
			break
		}
	}

	return k, new(G1).ScalarBaseMult(k), nil
}

// HashG1 hashes string m to an element in group G1.
func HashG1(m string) (*G1, error) {
	h := sha256.Sum256([]byte(m))
	hashNum := new(big.Int).SetBytes(h[:])
	x := new(big.Int).Mod(hashNum, p)
	for {
		//let's check if there exists a point (x, y) for some y on EC -
		// that means x^3 + 3 needs to be a quadratic residue
		x2 := new(big.Int).Mul(x, x)
		x2.Mod(x2, p)
		x3 := new(big.Int).Mul(x2, x)
		x3.Mod(x3, p)
		rhs := new(big.Int).Add(x3, curveB)
		rhs.Mod(rhs, p)
		y := new(big.Int).ModSqrt(rhs, p)
		if y != nil { // alternatively, if y is not needed, big.Jacobi(rhs, p) can be used to check if rhs is quadratic residue
			// BN curve has cofactor 1 (all points of the curve form a group where we are operating),
			// so x (now that we know rhs is QR) is an x-coordinate of some point in a cyclic group
			point := &curvePoint{
				x: x,
				y: y,
				z: new(big.Int).SetInt64(1),
				t: new(big.Int).SetInt64(1),
			}
			return &G1{point}, nil
		}
		x.Add(x, big.NewInt(1))
	}
}

func (e *G1) String() string {
	return "bn256.G1" + e.p.String()
	//return e.p.String()
}

// ScalarBaseMult sets e to g*k where g is the generator of the group and
// then returns e.
func (e *G1) ScalarBaseMult(k *big.Int) *G1 {
	if e.p == nil {
		e.p = newCurvePoint(nil)
	}
	e.p.Mul(curveGen, k, new(bnPool))
	return e
}

// ScalarMult sets e to a*k and then returns e.
func (e *G1) ScalarMult(a *G1, k *big.Int) *G1 {
	if e.p == nil {
		e.p = newCurvePoint(nil)
	}
	e.p.Mul(a.p, k, new(bnPool))
	return e
}

// Add sets e to a+b and then returns e.
// BUG(agl): this function is not complete: a==b fails.
func (e *G1) Add(a, b *G1) *G1 {
	if e.p == nil {
		e.p = newCurvePoint(nil)
	}
	e.p.Add(a.p, b.p, new(bnPool))
	return e
}

// Neg sets e to -a and then returns e.
func (e *G1) Neg(a *G1) *G1 {
	if e.p == nil {
		e.p = newCurvePoint(nil)
	}
	e.p.Negative(a.p)
	return e
}

// Set sets e to a and then returns e.
func (e *G1) Set(a *G1) *G1 {
	if e.p == nil {
		e.p = &curvePoint{}
	}
	e.p.Set(a.p)
	return e
}

// Marshal converts n to a byte slice.
func (e *G1) Marshal() []byte {
	// Each value is a 256-bit number.
	const numBytes = 256 / 8

	if e.p.IsInfinity() {
		return make([]byte, numBytes*2)
	}

	e.p.MakeAffine(nil)

	xBytes := new(big.Int).Mod(e.p.x, p).Bytes()
	yBytes := new(big.Int).Mod(e.p.y, p).Bytes()

	ret := make([]byte, numBytes*2)
	copy(ret[1*numBytes-len(xBytes):], xBytes)
	copy(ret[2*numBytes-len(yBytes):], yBytes)

	return ret
}

// Unmarshal sets e to the result of converting the output of Marshal back into
// a group element and then returns e.
func (e *G1) Unmarshal(m []byte) (*G1, bool) {
	// Each value is a 256-bit number.
	const numBytes = 256 / 8

	if len(m) != 2*numBytes {
		return nil, false
	}

	if e.p == nil {
		e.p = newCurvePoint(nil)
	}

	e.p.x.SetBytes(m[0*numBytes : 1*numBytes])
	e.p.y.SetBytes(m[1*numBytes : 2*numBytes])

	if e.p.x.Sign() == 0 && e.p.y.Sign() == 0 {
		// This is the point at infinity.
		e.p.y.SetInt64(1)
		e.p.z.SetInt64(0)
		e.p.t.SetInt64(0)
	} else {
		e.p.z.SetInt64(1)
		e.p.t.SetInt64(1)

		if !e.p.IsOnCurve() {
			return nil, false
		}
	}

	return e, true
}

// G2 is an abstract cyclic group. The zero value is suitable for use as the
// output of an operation, but cannot be used as an input.
type G2 struct {
	p *twistPoint
}

// RandomG2 returns x and g₂ˣ where x is a random, non-zero number read from r.
func RandomG2(r io.Reader) (*big.Int, *G2, error) {
	var k *big.Int
	var err error

	for {
		k, err = rand.Int(r, Order)
		if err != nil {
			return nil, nil, err
		}
		if k.Sign() > 0 {
			break
		}
	}

	return k, new(G2).ScalarBaseMult(k), nil
}

// HashG2 hashes string m to an element in group G2. It uses:
// Fuentes-Castaneda, Laura, Edward Knapp, and Francisco Rodríguez-Henríquez. "Faster hashing to G_2."
// International Workshop on Selected Areas in Cryptography. Springer, Berlin, Heidelberg, 2011.
func HashG2(m string) (*G2, error) {
	h := sha256.Sum256([]byte(m))
	hashNum := new(big.Int).SetBytes(h[:])
	v := new(big.Int).Mod(hashNum, p)

	pool := new(bnPool)
	// gfp2 is (x1, y1) where x1*i + y1
	x := newGFp2(pool)
	xxx := newGFp2(pool)
	for {
		// let's try to construct a point in F(p^2) as 1 + v*i
		x.y = big.NewInt(1)
		x.x = v

		// now we need to check if a is x-coordinate of some point
		// on curve (if there exists b such that b^2 = a^3 + 3)
		xxx.Square(x, pool)
		xxx.Mul(xxx, x, pool)

		rhs := newGFp2(pool)
		rhs.Add(xxx, twistB)

		y, err := newGFp2(pool).Sqrt(rhs, pool)

		if err == nil { // there is a square root for rhs
			point := &twistPoint{
				x,
				y,
				&gfP2{
					bigFromBase10("0"),
					bigFromBase10("1"),
				},
				&gfP2{
					bigFromBase10("0"),
					bigFromBase10("1"),
				},
			}

			// xQ + frob(3*xQ) + frob(frob(xQ)) + frob(frob(frob(Q)))
			// xQ:
			xpoint := newTwistPoint(pool).Mul(point, u, pool)

			dblxpoint := newTwistPoint(pool)
			dblxpoint.Double(xpoint, pool)

			trplxpoint := newTwistPoint(pool)
			trplxpoint.Add(xpoint, dblxpoint, pool)
			trplxpoint.MakeAffine(pool)

			// Frobenius(3*xQ)
			t1, err := newTwistPoint(pool).Frobenius(trplxpoint, pool)
			if err != nil {
				return nil, err
			}

			// Frobenius(Frobenius((xQ))
			xpoint.MakeAffine(pool)
			t2, err := newTwistPoint(pool).Frobenius(xpoint, pool)
			if err != nil {
				return nil, err
			}
			t2, err = t2.Frobenius(t2, pool)
			if err != nil {
				return nil, err
			}

			// Frobenius(Frobenius(Frobenius(Q)))
			t3, err := newTwistPoint(pool).Frobenius(point, pool)
			if err != nil {
				return nil, err
			}
			t3, err = t3.Frobenius(t3, pool)
			if err != nil {
				return nil, err
			}
			t3, err = t3.Frobenius(t3, pool)
			if err != nil {
				return nil, err
			}

			f := newTwistPoint(pool)
			f.Add(xpoint, t1, pool)
			f.Add(f, t2, pool)
			f.Add(f, t3, pool)

			return &G2{f}, nil
		}
		v.Add(v, big.NewInt(1))
	}
}

func (e *G2) String() string {
	return "bn256.G2" + e.p.String()
	//return e.p.String()
}

// ScalarBaseMult sets e to g*k where g is the generator of the group and
// then returns out.
func (e *G2) ScalarBaseMult(k *big.Int) *G2 {
	if e.p == nil {
		e.p = newTwistPoint(nil)
	}
	e.p.Mul(twistGen, k, new(bnPool))
	return e
}

// ScalarMult sets e to a*k and then returns e.
func (e *G2) ScalarMult(a *G2, k *big.Int) *G2 {
	if e.p == nil {
		e.p = newTwistPoint(nil)
	}
	e.p.Mul(a.p, k, new(bnPool))
	return e
}

// Add sets e to a+b and then returns e.
// BUG(agl): this function is not complete: a==b fails.
func (e *G2) Add(a, b *G2) *G2 {
	if e.p == nil {
		e.p = newTwistPoint(nil)
	}
	e.p.Add(a.p, b.p, new(bnPool))
	return e
}

// Set sets e to a and then returns e.
func (e *G2) Set(a *G2) *G2 {
	if e.p == nil {
		e.p = &twistPoint{}
	}
	e.p.Set(a.p)
	return e
}

// Neg sets e to -a and then returns e.
func (e *G2) Neg(a *G2) *G2 {
	if e.p == nil {
		e.p = &twistPoint{}
	}
	e.p.Neg(a.p)
	return e
}

// Marshal converts n into a byte slice.
func (n *G2) Marshal() []byte {
	// Each value is a 256-bit number.
	const numBytes = 256 / 8

	if n.p.IsInfinity() {
		return make([]byte, numBytes*6)
	}
	
	//n.p.MakeAffine(nil)  // G2解析z为（0，1）,注释后正确解析

	xxBytes := new(big.Int).Mod(n.p.x.x, p).Bytes()
	xyBytes := new(big.Int).Mod(n.p.x.y, p).Bytes()
	yxBytes := new(big.Int).Mod(n.p.y.x, p).Bytes()
	yyBytes := new(big.Int).Mod(n.p.y.y, p).Bytes()
	zxBytes := new(big.Int).Mod(n.p.z.x, p).Bytes()
	zyBytes := new(big.Int).Mod(n.p.z.y, p).Bytes()

	ret := make([]byte, numBytes*6)
	copy(ret[1*numBytes-len(xxBytes):], xxBytes)
	copy(ret[2*numBytes-len(xyBytes):], xyBytes)
	copy(ret[3*numBytes-len(yxBytes):], yxBytes)
	copy(ret[4*numBytes-len(yyBytes):], yyBytes)
	copy(ret[5*numBytes-len(zxBytes):], zxBytes)
	copy(ret[6*numBytes-len(zyBytes):], zyBytes)

	return ret
}

// Unmarshal sets e to the result of converting the output of Marshal back into
// a group element and then returns e.
func (e *G2) Unmarshal(m []byte) (*G2, bool) {
	// Each value is a 256-bit number.
	const numBytes = 256 / 8

	if len(m) != 6*numBytes {
		return nil, false
	}

	if e.p == nil {
		e.p = newTwistPoint(nil)
	}
	e.p.x.x.SetBytes(m[0*numBytes : 1*numBytes])
	e.p.x.y.SetBytes(m[1*numBytes : 2*numBytes])
	e.p.y.x.SetBytes(m[2*numBytes : 3*numBytes])
	e.p.y.y.SetBytes(m[3*numBytes : 4*numBytes])
	e.p.z.x.SetBytes(m[4*numBytes : 5*numBytes])
	e.p.z.y.SetBytes(m[5*numBytes : 6*numBytes])
	//fmt.Printf("\nzyBytes==> %+v\n",e.p)

/* G2解析z为（0，1）,注释后正确解析
	if e.p.x.x.Sign() == 0 &&
		e.p.x.y.Sign() == 0 &&
		e.p.y.x.Sign() == 0 &&
		e.p.y.y.Sign() == 0 {
		// This is the point at infinity.
		e.p.y.SetOne()
		e.p.z.SetZero()
		e.p.t.SetZero()
	} else {
		e.p.z.SetOne()
		e.p.t.SetOne()

		if !e.p.IsOnCurve() {
			return nil, false
		}
	}
*/
	return e, true
}

// GT is an abstract cyclic group. The zero value is suitable for use as the
// output of an operation, but cannot be used as an input.
type GT struct {
	p *gfP12
}

func (g *GT) String() string {
	return "bn256.GT" + g.p.String()
	//return  g.p.String()
}

// RandomGT returns x and gˣ where x is a random, non-zero number read from r.
func RandomGT(r io.Reader) (*big.Int, *GT, error) {
	var k *big.Int
	var err error

	for {
		k, err = rand.Int(r, Order)
		if err != nil {
			return nil, nil, err
		}
		if k.Sign() > 0 {
			break
		}
	}

	return k, new(GT).ScalarBaseMult(k), nil
}

func (e *GT) SetZero() *GT {
	e.p.SetZero()
	return e
}

func (e *GT) SetOne() *GT {
	e.p.SetOne()
	return e
}

// GetGTOne returns *GT set to 1.
func GetGTOne() *GT {
	g := newGFp12(new(bnPool))
	g.SetOne()
	return &GT{g}
}

// returns number in p-representation: a_11*p^11 + ... + a_1*p^1 + a_0 where 0 <= a_i < p
func intToPRepr(n *big.Int) []*big.Int {
	nn := new(big.Int).Set(n)
	pToI := big.NewInt(1)
	mod := new(big.Int).Set(p)
	a := make([]*big.Int, 12)
	for i := 0; i < 12; i++ {
		ai := new(big.Int).Mod(nn, mod)
		nn.Sub(nn, ai)
		ai.Div(ai, pToI)
		a[i] = ai
		if nn.Cmp(big.NewInt(0)) == 0 {
			for {
				i++
				if i == 12 {
					return a
				}
				a[i] = big.NewInt(0)
			}
		}
		pToI.Mul(pToI, p)
		mod.Mul(mod, p)
	}

	return a
}

// converts number in p-representation into *big.Int
func pReprToInt(a []*big.Int) *big.Int {
	pToI := big.NewInt(1)
	n := big.NewInt(0)
	for i := 0; i < 12; i++ {
		t := new(big.Int).Mul(a[i], pToI)
		n.Add(n, t)
		pToI.Mul(pToI, p)
	}

	return n
}

// MapStringToGT maps a string to GT group element. Needed for example when a message to be encrypted
// needs to be mapped into GT group.
func MapStringToGT(msg string) (*GT, error) {
	m := new(big.Int)
	m.SetBytes([]byte(msg))
	bound := new(big.Int).Exp(p, big.NewInt(12), nil)
	if m.Cmp(bound) >= 0 {
		return nil, fmt.Errorf("message is bigger than modulo, use key encapsulation")
	}
	a := intToPRepr(m)
	g := newGFp12(new(bnPool))
	g.x.x.x = a[0]
	g.x.x.y = a[1]
	g.x.y.x = a[2]
	g.x.y.y = a[3]
	g.x.z.x = a[4]
	g.x.z.y = a[5]

	g.y.x.x = a[6]
	g.y.x.y = a[7]
	g.y.y.x = a[8]
	g.y.y.y = a[9]
	g.y.z.x = a[10]
	g.y.z.y = a[11]

	return &GT{g}, nil
}

// MapGTToString maps an element from GT group to a string
func MapGTToString(gt *GT) string {
	a := make([]*big.Int, 12)
	a[0] = gt.p.x.x.x
	a[1] = gt.p.x.x.y
	a[2] = gt.p.x.y.x
	a[3] = gt.p.x.y.y
	a[4] = gt.p.x.z.x
	a[5] = gt.p.x.z.y

	a[6] = gt.p.y.x.x
	a[7] = gt.p.y.x.y
	a[8] = gt.p.y.y.x
	a[9] = gt.p.y.y.y
	a[10] = gt.p.y.z.x
	a[11] = gt.p.y.z.y

	r := pReprToInt(a)
	return string(r.Bytes())
}

// ScalarBaseMult sets e to pair(g1, g2)*k where g1 is the generator of G1 and
// g2 is generator of G2.
func (e *GT) ScalarBaseMult(k *big.Int) *GT {
	gt := Pair(&G1{curveGen}, &G2{twistGen})
	g := new(GT).ScalarMult(gt, k)
	e.Set(g)
	return e
}

// ScalarMult sets e to a*k and then returns e.
func (e *GT) ScalarMult(a *GT, k *big.Int) *GT {
	if e.p == nil {
		e.p = newGFp12(nil)
	}
	e.p.Exp(a.p, k, new(bnPool))
	e.p.Minimal()
	return e
}

// Add sets e to a+b and then returns e.
func (e *GT) Add(a, b *GT) *GT {
	if e.p == nil {
		e.p = newGFp12(nil)
	}
	e.p.Mul(a.p, b.p, new(bnPool))
	e.p.Minimal()
	return e
}

// Neg sets e to -a and then returns e.
func (e *GT) Neg(a *GT) *GT {
	if e.p == nil {
		e.p = newGFp12(nil)
	}
	e.p.Invert(a.p, new(bnPool))
	return e
}

// Set sets e to a and then returns e.
func (e *GT) Set(a *GT) *GT {
	if e.p == nil {
		e.p = newGFp12(nil)
	}
	e.p.Set(a.p)
	return e
}

// Marshal converts n into a byte slice.
func (n *GT) Marshal() []byte {
	n.p.Minimal()

	xxxBytes := n.p.x.x.x.Bytes()
	xxyBytes := n.p.x.x.y.Bytes()
	xyxBytes := n.p.x.y.x.Bytes()
	xyyBytes := n.p.x.y.y.Bytes()
	xzxBytes := n.p.x.z.x.Bytes()
	xzyBytes := n.p.x.z.y.Bytes()
	yxxBytes := n.p.y.x.x.Bytes()
	yxyBytes := n.p.y.x.y.Bytes()
	yyxBytes := n.p.y.y.x.Bytes()
	yyyBytes := n.p.y.y.y.Bytes()
	yzxBytes := n.p.y.z.x.Bytes()
	yzyBytes := n.p.y.z.y.Bytes()

	// Each value is a 256-bit number.
	const numBytes = 256 / 8

	ret := make([]byte, numBytes*12)
	copy(ret[1*numBytes-len(xxxBytes):], xxxBytes)
	copy(ret[2*numBytes-len(xxyBytes):], xxyBytes)
	copy(ret[3*numBytes-len(xyxBytes):], xyxBytes)
	copy(ret[4*numBytes-len(xyyBytes):], xyyBytes)
	copy(ret[5*numBytes-len(xzxBytes):], xzxBytes)
	copy(ret[6*numBytes-len(xzyBytes):], xzyBytes)
	copy(ret[7*numBytes-len(yxxBytes):], yxxBytes)
	copy(ret[8*numBytes-len(yxyBytes):], yxyBytes)
	copy(ret[9*numBytes-len(yyxBytes):], yyxBytes)
	copy(ret[10*numBytes-len(yyyBytes):], yyyBytes)
	copy(ret[11*numBytes-len(yzxBytes):], yzxBytes)
	copy(ret[12*numBytes-len(yzyBytes):], yzyBytes)

	return ret
}

// Unmarshal sets e to the result of converting the output of Marshal back into
// a group element and then returns e.
func (e *GT) Unmarshal(m []byte) (*GT, bool) {
	// Each value is a 256-bit number.
	const numBytes = 256 / 8

	if len(m) != 12*numBytes {
		return nil, false
	}

	if e.p == nil {
		e.p = newGFp12(nil)
	}

	e.p.x.x.x.SetBytes(m[0*numBytes : 1*numBytes])
	e.p.x.x.y.SetBytes(m[1*numBytes : 2*numBytes])
	e.p.x.y.x.SetBytes(m[2*numBytes : 3*numBytes])
	e.p.x.y.y.SetBytes(m[3*numBytes : 4*numBytes])
	e.p.x.z.x.SetBytes(m[4*numBytes : 5*numBytes])
	e.p.x.z.y.SetBytes(m[5*numBytes : 6*numBytes])
	e.p.y.x.x.SetBytes(m[6*numBytes : 7*numBytes])
	e.p.y.x.y.SetBytes(m[7*numBytes : 8*numBytes])
	e.p.y.y.x.SetBytes(m[8*numBytes : 9*numBytes])
	e.p.y.y.y.SetBytes(m[9*numBytes : 10*numBytes])
	e.p.y.z.x.SetBytes(m[10*numBytes : 11*numBytes])
	e.p.y.z.y.SetBytes(m[11*numBytes : 12*numBytes])

	return e, true
}

// Pair calculates an Optimal Ate pairing.
func Pair(g1 *G1, g2 *G2) *GT {
	return &GT{optimalAte(g2.p, g1.p, new(bnPool))}
}

// bnPool implements a tiny cache of *big.Int objects that's used to reduce the
// number of allocations made during processing.
type bnPool struct {
	bns   []*big.Int
	count int
}

func (pool *bnPool) Get() *big.Int {
	if pool == nil {
		return new(big.Int)
	}

	pool.count++
	l := len(pool.bns)
	if l == 0 {
		return new(big.Int)
	}

	bn := pool.bns[l-1]
	pool.bns = pool.bns[:l-1]
	return bn
}

func (pool *bnPool) Put(bn *big.Int) {
	if pool == nil {
		return
	}
	pool.bns = append(pool.bns, bn)
	pool.count--
}

func (pool *bnPool) Count() int {
	return pool.count
}
