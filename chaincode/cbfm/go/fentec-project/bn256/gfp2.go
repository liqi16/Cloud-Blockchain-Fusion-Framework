// Copyright 2012 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package bn256

// For details of the algorithms used, see "Multiplication and Squaring on
// Pairing-Friendly Fields, Devegili et al.
// http://eprint.iacr.org/2006/471.pdf.

import (
	"fmt"
	"math/big"
)

// gfP2 implements a field of size p² as a quadratic extension of the base
// field where i²=-1.
type gfP2 struct {
	x, y *big.Int // value is xi+y.
}

func newGFp2(pool *bnPool) *gfP2 {
	return &gfP2{pool.Get(), pool.Get()}
}

func (e *gfP2) String() string {
	x := new(big.Int).Mod(e.x, p)
	y := new(big.Int).Mod(e.y, p)
	return "(" + x.String() + "," + y.String() + ")"
}

func (e *gfP2) Put(pool *bnPool) {
	pool.Put(e.x)
	pool.Put(e.y)
}

func (e *gfP2) Set(a *gfP2) *gfP2 {
	e.x.Set(a.x)
	e.y.Set(a.y)
	return e
}

func (e *gfP2) SetZero() *gfP2 {
	e.x.SetInt64(0)
	e.y.SetInt64(0)
	return e
}

func (e *gfP2) SetOne() *gfP2 {
	e.x.SetInt64(0)
	e.y.SetInt64(1)
	return e
}

func (e *gfP2) Minimal() {
	if e.x.Sign() < 0 || e.x.Cmp(p) >= 0 {
		e.x.Mod(e.x, p)
	}
	if e.y.Sign() < 0 || e.y.Cmp(p) >= 0 {
		e.y.Mod(e.y, p)
	}
}

func (e *gfP2) IsZero() bool {
	return e.x.Sign() == 0 && e.y.Sign() == 0
}

func (e *gfP2) IsOne() bool {
	if e.x.Sign() != 0 {
		return false
	}
	words := e.y.Bits()
	return len(words) == 1 && words[0] == 1
}

func (e *gfP2) Conjugate(a *gfP2) *gfP2 {
	e.y.Set(a.y)
	e.x.Neg(a.x)
	return e
}

func (e *gfP2) Negative(a *gfP2) *gfP2 {
	e.x.Neg(a.x)
	e.y.Neg(a.y)
	return e
}

// Frobenius computes (x*i+y)^p = x^p * i^p + y^p = y - x*i (simply conjugation)
func (e *gfP2) Frobenius(a *gfP2, pool *bnPool) *gfP2 {
	e.Conjugate(a)

	return e
}

func (e *gfP2) Add(a, b *gfP2) *gfP2 {
	e.x.Add(a.x, b.x)
	e.y.Add(a.y, b.y)
	return e
}

func (e *gfP2) Sub(a, b *gfP2) *gfP2 {
	e.x.Sub(a.x, b.x)
	e.y.Sub(a.y, b.y)
	return e
}

func (e *gfP2) Double(a *gfP2) *gfP2 {
	e.x.Lsh(a.x, 1)
	e.y.Lsh(a.y, 1)
	return e
}

func (c *gfP2) Exp(a *gfP2, power *big.Int, pool *bnPool) *gfP2 {
	sum := newGFp2(pool)
	sum.SetOne()
	t := newGFp2(pool)

	for i := power.BitLen() - 1; i >= 0; i-- {
		t.Square(sum, pool)
		if power.Bit(i) != 0 {
			sum.Mul(t, a, pool)
		} else {
			sum.Set(t)
		}
	}

	c.Set(sum)

	sum.Put(pool)
	t.Put(pool)

	return c
}

// See "Multiplication and Squaring in Pairing-Friendly Fields",
// http://eprint.iacr.org/2006/471.pdf
func (e *gfP2) Mul(a, b *gfP2, pool *bnPool) *gfP2 {
	tx := pool.Get().Mul(a.x, b.y)
	t := pool.Get().Mul(b.x, a.y)
	tx.Add(tx, t)
	tx.Mod(tx, p)

	ty := pool.Get().Mul(a.y, b.y)
	t.Mul(a.x, b.x)
	ty.Sub(ty, t)
	e.y.Mod(ty, p)
	e.x.Set(tx)

	pool.Put(tx)
	pool.Put(ty)
	pool.Put(t)

	return e
}

func (e *gfP2) MulScalar(a *gfP2, b *big.Int) *gfP2 {
	e.x.Mul(a.x, b)
	e.y.Mul(a.y, b)
	return e
}

// MulXi sets e=ξa where ξ=i+3 and then returns e.
func (e *gfP2) MulXi(a *gfP2, pool *bnPool) *gfP2 {
	// (xi+y)(i+3) = (3x+y)i+(3y-x)
	tx := pool.Get().Lsh(a.x, 1)
	tx.Add(tx, a.x)
	tx.Add(tx, a.y)

	ty := pool.Get().Lsh(a.y, 1)
	ty.Add(ty, a.y)
	ty.Sub(ty, a.x)

	e.x.Set(tx)
	e.y.Set(ty)

	pool.Put(tx)
	pool.Put(ty)

	return e
}

func (e *gfP2) Square(a *gfP2, pool *bnPool) *gfP2 {
	// Complex squaring algorithm:
	// (xi+b)² = (x+y)(y-x) + 2*i*x*y
	t1 := pool.Get().Sub(a.y, a.x)
	t2 := pool.Get().Add(a.x, a.y)
	ty := pool.Get().Mul(t1, t2)
	ty.Mod(ty, p)

	t1.Mul(a.x, a.y)
	t1.Lsh(t1, 1)

	e.x.Mod(t1, p)
	e.y.Set(ty)

	pool.Put(t1)
	pool.Put(t2)
	pool.Put(ty)

	return e
}

func divBy2(a *big.Int) *big.Int {
	if new(big.Int).Mod(a, big.NewInt(2)).Cmp(big.NewInt(0)) == 0 {
		return new(big.Int).Div(a, big.NewInt(2))
	}
	s := new(big.Int).Add(a, p)
	return new(big.Int).Div(s, big.NewInt(2))
}

// Sqrt returns square root of g. Let's say g = a + b*i and tSqrt = sqrt(a^2 + b^2).
// Then Sqrt(g) = sqrt((a + tSqrt)/2) + i * b * 1 / (2*sqrt((a + tSqrt)/2)).
func (e *gfP2) Sqrt(g *gfP2, pool *bnPool) (*gfP2, error) {
	yy := new(big.Int).Mul(g.y, g.y)
	yy.Mod(yy, p)
	xx := new(big.Int).Mul(g.x, g.x)
	xx.Mod(xx, p)
	t := new(big.Int).Add(xx, yy)
	t.Mod(t, p)

	tSqrt := new(big.Int).ModSqrt(t, p) // z = sqrt(g.y^2 + g.x^2)
	if tSqrt == nil {                   // g.y^2 + g.x^2 is not QR
		return nil, fmt.Errorf("could not compute square root")
	}

	z := new(big.Int).Add(tSqrt, g.y) // z = g.y + sqrt(g.y^2 + g.x^2)
	z.Mod(z, p)

	z = divBy2(z) // z = (g.y + sqrt(g.y^2 + g.x^2)) / 2

	newY := new(big.Int).ModSqrt(z, p) // real part of what we are looking for (note that here gfP2 is x*i+y)
	if newY == nil {                   // (g.y + sqrt(g.y^2 + g.x^2)) / 2 is not QR
		tSqrtMin := new(big.Int).Sub(p, tSqrt)
		z := new(big.Int).Add(tSqrtMin, g.y) // z = g.y + sqrt(g.y^2 + g.x^2)
		z.Mod(z, p)
		z = divBy2(z)                     // z = (g.y + sqrt(g.y^2 + g.x^2)) / 2
		newY = new(big.Int).ModSqrt(z, p) // real part of what we are looking for (note that here gfP2 is x*i+y)
		if newY == nil {
			return nil, fmt.Errorf("could not compute square root")
		}
	}

	newYInv := new(big.Int).ModInverse(newY, p)
	xDiv2 := divBy2(g.x)
	newX := new(big.Int).Mul(xDiv2, newYInv)
	newX.Mod(newX, p)

	e.y = newY
	e.x = newX

	return e, nil
}

func (e *gfP2) Invert(a *gfP2, pool *bnPool) *gfP2 {
	// See "Implementing cryptographic pairings", M. Scott, section 3.2.
	// ftp://136.206.11.249/pub/crypto/pairings.pdf
	t := pool.Get()
	t.Mul(a.y, a.y)
	t2 := pool.Get()
	t2.Mul(a.x, a.x)
	t.Add(t, t2)

	inv := pool.Get()
	inv.ModInverse(t, p)

	e.x.Neg(a.x)
	e.x.Mul(e.x, inv)
	e.x.Mod(e.x, p)

	e.y.Mul(a.y, inv)
	e.y.Mod(e.y, p)

	pool.Put(t)
	pool.Put(t2)
	pool.Put(inv)

	return e
}
