package main

import (
	"fmt"
	"os"
	"io/ioutil"
	"bytes"
	"github.com/fentec-project/gofe/abe"
	"github.com/fentec-project/bn256"
	"encoding/json"
)

var sep = []byte("  ")
var fame = abe.NewFAME()
var debug = true

type masterKey struct {
	Type string `json:"type"`
	PkG2 []byte `json:"pkG2"`
	PkGT []byte `json:"pkGT"`
	SkInt []byte `json:"skInt"`
	SkG1 []byte `json:"skG1"`
}

type CipherKeyTable struct{
	Ct0 []byte
	Ct []byte
	CtPrime []byte
	Msp []byte
}

type AttributesTable struct{
	Attributes []int
	Period []int
	K0 []byte
	K []byte
	KPrime []byte
	AttribToI []byte
}

type MidResult struct{
	TkPairing  []byte
}

type User struct {
	Type string
	Username string
	Pk string
	Tsus int
	Tfail int
	Reputation float64
	Attribute string
	Period string
	AttributeKey AttributesTable
}

type Data struct{
	Type string
	Owner string
	File string
	URL string
	UploadTime string
	CipherKey CipherKeyTable
	Policy string
	Tag string
	N int
	LatestAuditTime string
	LatestAuditResult bool
}

func readFileToBytes(filename string) []byte {
	if contents, err := ioutil.ReadFile(filename); err == nil {
		return contents
	} else {
		fmt.Println(err)
		return []byte("")
	}
}

func writeStringToFile(filename string, text string) bool {
	data :=  []byte(text)
	if ioutil.WriteFile(filename,data,0644) == nil {
		return true
	}else{
		return false
	}
}

func Decryption(mk *masterKey) string {

	//fmt.Println("===================Decryption========================\n")

	//uid_dataHash :=  args[0]+args[1] //arg[0]: userId, arg[1]:dataHash

	path := "/home/simba/gopath/src/github.com/hyperledger/fabric/scripts/fabric-samples/cbfm/client/go/"

	midResultAsBytes := readFileToBytes(path + "midResult")

	midResult := MidResult{}

	if err := json.Unmarshal(midResultAsBytes, &midResult); err != nil {
		return ""
	}

	//json.Unmarshal(midResultAsBytes,&midResult)

	tkPairingByte := midResult.TkPairing
	tkPairingAsbyte := bytes.Split(tkPairingByte, sep)
	var tk [3]*bn256.GT

	for i:=0; i<3; i++ {
		tk[i], _ = new(bn256.GT).Unmarshal(tkPairingAsbyte[i])
	}
	token := &abe.FAMEMidResult{TkPairing:tk }

	dataBytes := readFileToBytes(path + "data")
	theData := Data{}
	if err := json.Unmarshal(dataBytes, &theData); err != nil {
		return ""
	}
	//json.Unmarshal(userBytes,&theUser)


	///////////////////////////////////////////////////////////////////////////////////
	//cipherKeyTableAsBytes := []byte(cipherKeyString)

	cipherKeyTable := theData.CipherKey
	//json.Unmarshal(cipherKeyTableAsBytes,&cipherKeyTable)

	//fmt.Println("==>cipherKeyTable")
	//fmt.Printf("%+v\n",cipherKeyTable)

	ct0Byte := cipherKeyTable.Ct0
	ctByte := cipherKeyTable.Ct
	ctPrimeAsBytes := cipherKeyTable.CtPrime
	mspAsBytes := cipherKeyTable.Msp
	ct0Asbyte := bytes.Split(ct0Byte, sep)
	ctAsbyte := bytes.Split(ctByte, sep)
	//byte to ct0
	var ct0 [3]*bn256.G2
	for i:=0; i<3; i++ {
		ct0[i], _ = new(bn256.G2).Unmarshal(ct0Asbyte[i])
	}
	//byte to msp
	var m *abe.MSP
	json.Unmarshal(mspAsBytes, &m)
	//byte to ct
	ct := make([][3]*bn256.G1, len(m.Mat))
	for i:= 0; i< len(m.Mat)*3; i++{
		ct[i/3][i%3], _ = new(bn256.G1).Unmarshal(ctAsbyte[i])
	}
	//byte to ctPrime
	var ctPrime *bn256.GT
	ctPrime, _ = new(bn256.GT).Unmarshal(ctPrimeAsBytes)

	cipher := &abe.FAMECipher{Ct0: ct0, Ct: ct, CtPrime: ctPrime, Msp: m}

	msgCheck, _:= fame.Decrypt(cipher, token)
	//msg := "Attack at dawn!"

    return string([]byte(msgCheck))
}

func Encryption(mk *masterKey,msg string,policy string) string {

	//fmt.Println("===================Encryption========================\n")

	pkG2Byte := mk.PkG2
	pkGTByte := mk.PkGT


	//pkG2Byte, err := APIstub.GetState("pkG2") 
	//pkGTByte, err := APIstub.GetState("pkGT") 

	pkG2Asbyte := bytes.Split(pkG2Byte, sep) //split转 []byte 为[][]byte
	pkGTAsbyte := bytes.Split(pkGTByte, sep)

	var pkG2 [2]*bn256.G2
	var pkGT [2]*bn256.GT
	for i:=0; i<2; i++ {
		pkG2[i], _ = new(bn256.G2).Unmarshal(pkG2Asbyte[i])
		pkGT[i], _ = new(bn256.GT).Unmarshal(pkGTAsbyte[i])
	}

	pubKey := &abe.FAMEPubKey{PartG2: pkG2, PartGT: pkGT}
	/*fmt.Println("pubKey==>")
	fmt.Printf("%+v\n", pubKey)*/
	//------------------------------------------------------------------
	//-*加密部分可由owner链下完成*-
	//msg := "Attack at dawn!"
	//msg := args[0]
	//fmt.Println("msg =>")
	//fmt.Println(msg)

	//policy := "((0 AND 1) OR (2 AND 3)) AND 5"	
	//policy := args[1]
	//fmt.Println("policy =>")
	//fmt.Println(policy)
	msp, err:= abe.BooleanToMSP(policy, false)

	if err != nil {
		errString := "BooleanToMSP Error!"
		fmt.Println("BooleanToMSP Error! ", err)
		return errString
	}

	cipher, _ := fame.Encrypt(msg, msp, pubKey)
	//if debug {
	//	fmt.Println("Ciphertext=>")
	// 	fmt.Printf("%+v\n", cipher)
	// }
	//------------------------------------------------------------------
	//ct0 to byte
	ct0Asbyte := make([][]byte, 256/8*3)
	for i:=0; i<3; i++ {
		ct0Asbyte[i] = cipher.Ct0[i].Marshal()
	}
	//APIstub.PutState("ct0", bytes.Join(ct0Asbyte,sep))

	//ct to byte

	ctAsByte := make([][]byte, len(msp.Mat)*3)
	n := 0
	for i := 0; i < len(msp.Mat); i++ {
		for l := 0; l<3; l++ {
			//fmt.Printf("%+v\n", cipher.Ct[i][l])
			ctAsByte[n] = cipher.Ct[i][l].Marshal()
			n++
		}
	}
	//APIstub.PutState("ct", bytes.Join(ctAsByte,sep))
	// //ctprime to byte
	ctPrimeAsbyte := cipher.CtPrime.Marshal()
	//APIstub.PutState("ctPrime", ctPrimeAsbyte)
	//MSP to byte
	mspAsBytes, _ := json.Marshal(cipher.Msp)
	//APIstub.PutState("msp", mspAsBytes)

	var cipherKeyTable = CipherKeyTable{}
	cipherKeyTable.Ct0 = bytes.Join(ct0Asbyte,sep)
	cipherKeyTable.Ct = bytes.Join(ctAsByte,sep)
	cipherKeyTable.CtPrime = ctPrimeAsbyte
	cipherKeyTable.Msp = mspAsBytes
	//fmt.Println("cipherKeyTable=>")
	//fmt.Printf("%+v\n", cipherKeyTable)
	cipherKeyTableAsBytes,_ :=json.Marshal(cipherKeyTable)
	//APIstub.PutState(args[2],cipherKeyTableAsBytes) 
	cipherKeyString := string(cipherKeyTableAsBytes)

	return cipherKeyString
}

func main() {
	//fmt.Println("命令行参数数量:",len(os.Args))

	//for k,v:= range os.Args{
	//    fmt.Printf("args[%v]=[%v]\n",k,v)
        //}

	path := "/home/simba/gopath/src/github.com/hyperledger/fabric/scripts/fabric-samples/cbfm/client/go/"

	file, err := os.Open(path+"masterKey")
	if err != nil {
	panic(err)
	}
	defer file.Close()
	content, err := ioutil.ReadAll(file)
	//fmt.Println(string(content))

	masterKeyString := string(content)
	masterKeyBytes:=[]byte(masterKeyString)

	masterkey := new(masterKey)
	if err := json.Unmarshal(masterKeyBytes, masterkey); err != nil {
		return
	}

	//mktype := masterkey.Type
	//mkpkG2 := masterkey.PkG2
	//mkpkGT := masterkey.PkGT
	//mkskInt := masterkey.SkInt
	//mkskG1 := masterkey.SkG1

	//fmt.Printf("mktype:  %+v\n " , mktype)
	//fmt.Printf("mkpkG2:  %+v\n " , mkpkG2)
	//fmt.Printf("mkpkGT:  %+v\n " , mkpkGT)
	//fmt.Printf("mkskInt:  %+v\n " , mkskInt)
	//fmt.Printf("mkskG1:  %+v\n " , mkskG1)

	if os.Args[1]=="enc" {
		symKeyString := os.Args[2]
		policyString := string(readFileToBytes(path+"policy"))
		if len(policyString) == 0 {
			fmt.Printf("Error! Policy is empty.\n")
		}else{
			cipherKeyString := Encryption(masterkey,symKeyString , policyString)
			//fmt.Println(cipherKeyString)
			writeStringToFile(path+"encryptedSymKey", cipherKeyString)
			//if result {
			//	fmt.Printf("Success!\n")
			//}else{
			//	fmt.Printf("Fail!\n")
			//}
		}

	}else if os.Args[1] == "dec"{
		msgString := Decryption(masterkey)
		if len(msgString)==0 {
			fmt.Printf("Error! Decryption Failed.")
		}else{
			//fmt.Printf(msgString)
			writeStringToFile(path+"symKey", msgString)
			//if result {
			//	fmt.Printf("\nSuccess!\n")
			//}else{
			//	fmt.Printf("\nFail!\n")
			//}
		}
	}else{
		fmt.Printf(" args error! ")
	}

}













