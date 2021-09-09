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

package simple_test

import (
	"math/big"
	"testing"

	"github.com/fentec-project/gofe/data"
	"github.com/fentec-project/gofe/innerprod/simple"
	"github.com/fentec-project/gofe/sample"
	"github.com/stretchr/testify/assert"
)

func TestSimple_DDH(t *testing.T) {
	l := 3
	bound := new(big.Int).Exp(big.NewInt(2), big.NewInt(20), big.NewInt(0))
	sampler := sample.NewUniformRange(new(big.Int).Neg(bound), bound)
	modulusLength := 128

	simpleDDH, err := simple.NewDDH(l, modulusLength, bound)

	if err != nil {
		t.Fatalf("Error during simple inner product creation: %v", err)
	}

	masterSecKey, masterPubKey, err := simpleDDH.GenerateMasterKeys()

	if err != nil {
		t.Fatalf("Error during master key generation: %v", err)
	}

	y, err := data.NewRandomVector(l, sampler)

	if err != nil {
		t.Fatalf("Error during random generation: %v", err)
	}

	funcKey, err := simpleDDH.DeriveKey(masterSecKey, y)

	if err != nil {
		t.Fatalf("Error during key derivation: %v", err)
	}

	x, err := data.NewRandomVector(l, sampler)

	if err != nil {
		t.Fatalf("Error during random generation: %v", err)
	}

	// simulate the instantiation of encryptor (which should be given masterPubKey)
	encryptor := simple.NewDDHFromParams(simpleDDH.Params)
	xyCheck, err := x.Dot(y)

	if err != nil {
		t.Fatalf("Error during inner product calculation")
	}
	ciphertext, err := encryptor.Encrypt(x, masterPubKey)

	if err != nil {
		t.Fatalf("Error during encryption: %v", err)
	}

	decryptor := simple.NewDDHFromParams(simpleDDH.Params)
	xy, err := decryptor.Decrypt(ciphertext, funcKey, y)

	if err != nil {
		t.Fatalf("Error during decryption: %v", err)
	}

	assert.Equal(t, xy, xyCheck, "Original and decrypted values should match")
}
