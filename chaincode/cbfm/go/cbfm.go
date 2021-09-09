package main

import (
	"fmt"
	"strconv"

	"bytes"
	"strings"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"github.com/chaincode/cbfm/go/fentec-project/bn256"
	"github.com/chaincode/cbfm/go/fentec-project/gofe/abe"
	"log"
	"math"
	"math/big"
	"time"
	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)

//KEY = MASTERKEY
type MasterKey struct {
	PkG2  []byte `json:"pkG2"`
	PkGT  []byte `json:"pkGT"`
	SkInt []byte `json:"skInt"`
	SkG1  []byte `json:"skG1"`
}

//KEY = USER + Msp + Username
type User struct {
	Msp	     string
	Username     string
	UserId	     string
	AuditPk      string
	Tsus         int
	Tfail        int
	Reputation   float64
	Attribute    string
	Period       string
	AttributeKey AttributesTable
	SigPk	     string
}

type AttributesTable struct {
	Attributes []int
	Period     []int
	K0         []byte
	K          []byte
	KPrime     []byte
	AttribToI  []byte
}

//KEY = CLOUD +　Msp + Name
type Cloud struct {
	Msp		string
    Name		string
	CloudId		string
    IP		string
    Port		string
	Pk		string
}

//KEY = PEER + Id
type Peer struct{
	Id 			string
	Type        string
	Msp         string
	Name        string
}

//KEY = DATA + Channel + UserId + Name
type Data struct {
	Channel           string
	Tx                string
	OwnerMsp		  string
	Owner             string
	OwnerId		  	  string
	Name              string
	CloudMsp		  string	
	Cloud             string
	URL               string
	UserHash          string
	UserSig			  string
	CloudHash	  	  string
	CloudSig	  	  string
	UploadTime        string
	UpdateTime	      string
	CipherKey         CipherKeyTable
	Policy            string
	Tag               string
	N                 int
	LatestAuditTime   string
	LatestAuditResult bool
	Introduction      string
}

type CipherKeyTable struct {
	Ct0     []byte
	Ct      []byte
	CtPrime []byte
	Msp     []byte
}

//KEY = OPERATION + ChannelID + UserId + Name + Timestamp
type Operation struct {	
	Datakey 	string
	OperatorMsp string
    Operator	string
	Operation	string
    Tx			string
    Timestamp	string
}

//KEY = SHARE + ChannelID + UserId + Name + Timestamp
type Share struct {
	Datakey 	string
	OwnerMsp    string
	DataOwner	string
	UserMsp     string
	DataUser	string
	TkPairing 	[]byte
    Timestamp 	string
	Tx 			string
}

//KEY = AUDIT + ChannelID + UserId + Name + Timestamp
type Audit struct {
	Datakey 	string
	OwnerMsp    string
	Owner	string
	AuditorMsp     string
	Auditor	string
	CloudMsp string
	Cloud string
	C string
	Timestamp string
	Challenge string
	Proof string
	Result string
	TxChallenge string
	TxVerify string
}

var fame = abe.NewFAME()
var debug = true
var sep = []byte("  ")

type SimpleChaincode struct {
}

//Init
func (t *SimpleChaincode) Init(stub shim.ChaincodeStubInterface) pb.Response {

	if mkBytes, err := stub.GetState("MASTERKEY"); err == nil && len(mkBytes) != 0 {
		return shim.Success([]byte("DO NOT ReInitialize: MasterKey already exist !"))
	}

	pubKey, secKey, err := fame.GenerateMasterKeys()
	if err != nil {
		fmt.Println("Error generate Master keys!", err)
		return shim.Error(err.Error())
	}

	if debug {
		fmt.Printf("pubKey:  %+v\n ", pubKey)
		fmt.Printf("secKey:  %+v\n ", secKey)
	}

	pkG2Asbyte := make([][]byte, 256/8*3)
	pkGTAsbyte := make([][]byte, 256/8*3)
	for i := 0; i < 2; i++ {
		pkG2Asbyte[i] = pubKey.PartG2[i].Marshal()
		pkGTAsbyte[i] = pubKey.PartGT[i].Marshal()
	}


	skIntAsBytes, _ := json.Marshal(secKey.PartInt)
	skG1Asbyte := make([][]byte, 256/8*2) //(x,y)
	for i := 0; i < 3; i++ {
		skG1Asbyte[i] = secKey.PartG1[i].Marshal()
	}

	mk := &MasterKey{
		PkG2:  bytes.Join(pkG2Asbyte, sep),
		PkGT:  bytes.Join(pkGTAsbyte, sep),
		SkInt: skIntAsBytes,
		SkG1:  bytes.Join(skG1Asbyte, sep),
	}

	mkBytes, err := json.Marshal(mk)
	if err != nil {
		return shim.Error(fmt.Sprintf("marshal masterKey error %s", err))
	}

	if err := stub.PutState("MASTERKEY", mkBytes); err != nil {
		return shim.Error(fmt.Sprintf("put masterKey error %s", err))
	}

	return shim.Success(mkBytes)
}

//Invoke
func (t *SimpleChaincode) Invoke(stub shim.ChaincodeStubInterface) pb.Response {
	//fmt.Println("ex02 Invoke")
	function, args := stub.GetFunctionAndParameters()
	if function == "queryMasterKey" {
		return t.queryMasterKey(stub, args)
	} else if function == "userRegister" {
		return t.userRegister(stub, args)
	} else if function == "userLogin" {
		return t.userLogin(stub, args)
	} else if function == "queryUser" {
		return t.queryUser(stub, args)
	} else if function == "queryPeer" {
		return t.queryPeer(stub, args)
	} else if function == "queryAllUser" {
		return t.queryAllUser(stub, args)
	} else if function == "cloudRegister" {
		return t.cloudRegister(stub, args)
	} else if function == "cloudLogin" {
		return t.cloudLogin(stub, args)
	} else if function == "queryCloud" {
		return t.queryCloud(stub, args)
	} else if function == "queryAllCloud" {
		return t.queryAllCloud(stub, args)
	} else if function == "cloudUpdate" {
		return t.cloudUpdate(stub, args)
	} else if function == "uploadMetadata" {
		return t.uploadMetadata(stub,args)
	} else if function == "updateCloudSignature" {
		return t.updateCloudSignature(stub, args)
	} else if function == "queryData" {
		return t.queryData(stub, args)
	} else if function == "queryAllData" {
		return t.queryAllData(stub, args)
	} else if function == "queryAllOperation" {
		return t.queryAllOperation(stub, args)
	} else if function == "querySbData" {
		return t.querySbData(stub, args)
	} else if function == "queryDataHistory" {
		return t.queryDataHistory(stub,args)
	} else if function == "PreDecryption"{
		return t.PreDecryption(stub,args)
	} else if function == "queryShare" {
		return t.queryShare(stub, args)
	} else if function == "queryAllShare" {
		return t.queryAllShare(stub, args)
	} else if function == "queryDataShare" {
		return t.queryDataShare(stub, args)
	} else if function == "queryDataOperation"{
		return t.queryDataOperation(stub, args)
	} else if function == "queryOperation"{
		return t.queryOperation(stub, args)
	} else if function == "updateAuditResult"{
		return t.updateAuditResult(stub,args)
	} else if function == "updateData"{
		return t.updateData(stub,args)
	} else if function == "auditData" {
		return t.auditData(stub, args)
	} else if function == "queryDataLatestAudit" {
		return t.queryDataLatestAudit(stub, args)
	} else if function == "queryDataAudit" {
		return t.queryDataAudit(stub, args)
	} else if function == "queryAllAudit" {
		return t.queryAllAudit(stub, args)
	} else if function == "queryAudit" {
		return t.queryAudit(stub, args)
	}
	return shim.Error("Invalid invoke function name. ")
}

//queryMasterKey
func (t *SimpleChaincode) queryMasterKey(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 0 {
		return shim.Error("Error! Function 'queryMasterKey' does not need args")
	}

	mkBytes, err := stub.GetState("MASTERKEY")
	if err != nil || len(mkBytes) == 0 {
		return shim.Error("MASTERKEY not found")
	}
	return shim.Success(mkBytes)
}

func calculateHashFromBytes(msg []byte)(string, error){
	//SHA256算法.
	hash := sha256.New()
	_, err := hash.Write(msg)
	result := hash.Sum(nil)
	encodeString := base64.StdEncoding.EncodeToString(result)
	return encodeString,err
}

func calculateReputation(Tsus int, Tfail int) float64 {
	floatTsus := float64(Tsus)
	floatTfail := float64(Tfail)
	Reputation := (1/math.Pi)*math.Atan(floatTsus-floatTfail) + 0.5
	return Reputation
}

func generateAttribKey(SkInt []byte, SkG1 []byte, AttributeString string, PeriodString string) AttributesTable {

	fmt.Println("===================GenerateAttribKeys========================\n")
	//args[0]:'UserNo.1',args[1]:'[0, 2,  5]',args[2]:'[30,60,100,120]'
	//gamma := []int{0, 2, 3, 5}
	/*
			aBytes, err := stub.GetState("object_a")
			var fame *abe.FAME
			err = json.Unmarshal(aBytes, &fame)
			fmt.Println("fame==>")
		 	fmt.Printf("%+v\n",fame)
	*/
	//returns the marshaled serialized identity of the Client
	//serializedIDByte,_ := stub.GetCreator()
	//serializedIDStr := string(serializedIDByte)

	skIntAsBytes := SkInt
	skG1Byte := SkG1

	skG1Asbyte := bytes.Split(skG1Byte, sep)
	// byte to sk
	var skInt [4]*big.Int
	json.Unmarshal(skIntAsBytes, &skInt)
	var skG1 [3]*bn256.G1
	for i := 0; i < 3; i++ {
		skG1[i], _ = new(bn256.G1).Unmarshal(skG1Asbyte[i])
	}
	secKey := &abe.FAMESecKey{PartInt: skInt, PartG1: skG1}
	fmt.Printf("secKey ==> %+v\n", secKey)
	//------------------------------------------------------------------

	// string to []int
	var gamma []int
	json.Unmarshal([]byte(AttributeString), &gamma)
	fmt.Printf("Attributes ==> %#v\n", gamma)

	var period []int
	json.Unmarshal([]byte(PeriodString), &period)
	fmt.Printf("Period ==> %#vs\n", period)

	keys, err := fame.GenerateAttribKeys(gamma, secKey)
	//fmt.Printf("%+v\n",keys)

	if err != nil {
		fmt.Println("GenerateAttribKeys Error! ", err)
		return AttributesTable{}
	}
	//------------------------------------------------------------------
	//k0 to byte
	k0Asbyte := make([][]byte, 256/8*3)
	for i := 0; i < 3; i++ {
		k0Asbyte[i] = keys.K0[i].Marshal()
	}
	//stub.PutState("k0", bytes.Join(k0Asbyte,sep))

	//K to byte
	kAsByte := make([][]byte, len(gamma)*3)
	n := 0
	for i := 0; i < len(gamma); i++ {
		for l := 0; l < 3; l++ {
			//fmt.Printf("%+v\n", cipher.Ct[i][l])
			kAsByte[n] = keys.K[i][l].Marshal()
			n++
		}
	}
	//stub.PutState("k", bytes.Join(kAsByte,sep))
	//kPrime to byte
	kPrimeAsbyte := make([][]byte, 256/8*3)
	for i := 0; i < 3; i++ {
		kPrimeAsbyte[i] = keys.KPrime[i].Marshal()
	}
	//stub.PutState("kPrime", bytes.Join(kPrimeAsbyte,sep))
	//AttribToI to byte
	AToIAsbyte, _ := json.Marshal(keys.AttribToI)
	//stub.PutState("attribToI", AToIAsbyte)

	var attributesTable = AttributesTable{}
	attributesTable.Attributes = gamma
	attributesTable.Period = period
	attributesTable.K0 = bytes.Join(k0Asbyte, sep)
	attributesTable.K = bytes.Join(kAsByte, sep)
	attributesTable.KPrime = bytes.Join(kPrimeAsbyte, sep)
	attributesTable.AttribToI = AToIAsbyte
	//fmt.Println("attributesTable=>")
	//fmt.Printf("%+v\n", attributesTable)

	//attributesTableAsBytes,_ :=json.Marshal(attributesTable)

	//stub.PutState(args[0],attributesTableAsBytes)

	return attributesTable

}

//userRegister
func (t *SimpleChaincode) userRegister(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 6 {
		return shim.Error("Error! function 'userRegister' not enough args. Need 6 args. ")
	}

	mkBytes, err := stub.GetState("MASTERKEY")
	if err != nil || len(mkBytes) == 0 {
		return shim.Error("masterKey not found")
	}

	masterkey := new(MasterKey)
	if err := json.Unmarshal(mkBytes, masterkey); err != nil {
		return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
	}

	fmt.Printf("Masterkey ==> %#v\n", masterkey)

	//mktype := masterkey.Type
	//mkpkG2 := masterkey.PkG2
	//mkpkGT := masterkey.PkGT
	mkskInt := masterkey.SkInt
	mkskG1 := masterkey.SkG1

	userMspId := args[0]
	userUsername := args[1]
	userAuditPk := args[2]
	userTsus := 0
	userTfail := 0
	userReputation := calculateReputation(userTsus, userTfail)
	userAttribute := args[3]
	userPeriod := args[4]
	userAttributeKey := generateAttribKey(mkskInt, mkskG1, userAttribute, userPeriod)

	/*	
	if len(args[5]) < 8 {
		return shim.Error("Error! Password need 8 characters at least. ")
	}

	userPassword := calculateHash(args[5])
	if len(userPassword) == 0 {
		return shim.Error("Error! Calculate hash error. ")
	}*/

	userSigPk := args[5]

	userKey, err := stub.CreateCompositeKey("USER", []string{
		userMspId,
		userUsername,
	})

	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	if userBytes, err := stub.GetState(userKey); err == nil && len(userBytes) != 0 {
		return shim.Error("User Register Error : user already exist !")
	}

	creatorBytes, err := stub.GetCreator()
	if err != nil {
		return shim.Error(err.Error())
	}
	userId, err := calculateHashFromBytes(creatorBytes)
	if err != nil {
		return shim.Error(err.Error())
	}

	newUser := &User{
		Msp:		  userMspId,
		Username:     userUsername,
		UserId:	      userId,
		AuditPk:      userAuditPk,
		Tsus:         userTsus,
		Tfail:        userTfail,
		Reputation:   userReputation,
		Attribute:    userAttribute,
		Period:       userPeriod,
		AttributeKey: userAttributeKey,
		SigPk:	      userSigPk,
	}

	newUserBytes, err := json.Marshal(newUser)

	if err != nil {
		return shim.Error(fmt.Sprintf("newUser marshal user error %s", err))
	}

	if err := stub.PutState(userKey, newUserBytes); err != nil {
		return shim.Error(fmt.Sprintf("put NewUser error %s", err))
	}

	peerKey, err := stub.CreateCompositeKey("PEER", []string{
		userId,
	})

	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	if peerBytes, err := stub.GetState(peerKey); err == nil && len(peerBytes) != 0 {
		return shim.Error("Peer Register Error : peer already exist !")
	}

	newPeer := &Peer{
		Id:		userId,
		Type:	"USER",
		Msp:	userMspId,
		Name:	userUsername,
	}

	newPeerBytes, err := json.Marshal(newPeer)

	if err != nil {
		return shim.Error(fmt.Sprintf("newPeer marshal user error %s", err))
	}

	if err := stub.PutState(peerKey, newPeerBytes); err != nil {
		return shim.Error(fmt.Sprintf("put newPeer error %s", err))
	}

	return shim.Success(newUserBytes)
}

//userLogin
func (t *SimpleChaincode) userLogin(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 2 {
		return shim.Error("Error! function 'userLogin' not enough args. Need 2 args. ")
	}
	
	userMspId := args[0]
	username := args[1]

	userKey, err := stub.CreateCompositeKey("USER", []string{
		userMspId,
		username,
	})
	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	userBytes, err := stub.GetState(userKey)
	if err != nil || len(userBytes) == 0 {
		return shim.Error("Error! User not found. ")
	}

	creatorBytes, err := stub.GetCreator()
	if err != nil {
		return shim.Error(err.Error())
	}
	userId, err := calculateHashFromBytes(creatorBytes)
	if err != nil {
		return shim.Error(err.Error())
	}

	theUser := User{}

	if err := json.Unmarshal(userBytes, &theUser); err != nil {
		return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
	}

	if theUser.UserId != userId {
		return shim.Error("Error! Wrong certificate. ")
	}

	return shim.Success(userBytes)
}

//queryUser
func (t *SimpleChaincode) queryUser(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 2 {
		return shim.Error("Error! function 'queryUser' not enough args. Need 2 args. ")
	}
	userMspId := args[0]
	username := args[1]

	userKey, err := stub.CreateCompositeKey("USER", []string{
		userMspId,
		username,
	})
	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	userBytes, err := stub.GetState(userKey)
	if err != nil || len(userBytes) == 0 {
		return shim.Error("Error! User not found. ")
	}

	return shim.Success(userBytes)
}


//queryPeer
func (t *SimpleChaincode) queryPeer(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 1 {
		return shim.Error("Error! function 'queryPeer' not enough args. Need 1 args. ")
	}
	id := args[0]

	peerKey, err := stub.CreateCompositeKey("PEER", []string{
		id,
	})
	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	peerBytes, err := stub.GetState(peerKey)
	if err != nil || len(peerBytes) == 0 {
		return shim.Error("Error! Peer not found. ")
	}

	return shim.Success(peerBytes)
}

//queryAllUser
func (t *SimpleChaincode) queryAllUser(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 0 {
		return shim.Error("Error! function 'queryAllUser' does not need any arg. ")
	}

	keys := make([]string, 0)

	result, err := stub.GetStateByPartialCompositeKey("USER", keys)

	if err != nil {
		return shim.Error(fmt.Sprintf("queryAllUser error: %s", err))
	}
	defer result.Close()

	users := make([]*User, 0)

	for result.HasNext() {
		userVal, err := result.Next()
		if err != nil {
			return shim.Error(fmt.Sprintf("query error: %s", err))
		}

		theUser := new(User)

		if err := json.Unmarshal(userVal.GetValue(), theUser); err != nil {
			return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
		}

		users = append(users, theUser)
	}

	usersBytes, err := json.Marshal(users)
	if err != nil {
		return shim.Error(fmt.Sprintf("marshal error: %s", err))
	}

	return shim.Success(usersBytes)
}

//cloudRegister
func (t *SimpleChaincode) cloudRegister(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	
	if len(args) != 5 {
		return shim.Error("Error! function 'cloudRegister' not enough args. Need 5 args. ")
	}

	cloudMsp := args[0]
	cloudName:= args[1]
   	cloudIP:= args[2]
    cloudPort:= args[3]
	cloudPk:= args[4]

	cloudKey, err := stub.CreateCompositeKey("CLOUD", []string{
		cloudMsp,
		cloudName,
	})

	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	if cloudBytes, err := stub.GetState(cloudKey); err == nil && len(cloudBytes) != 0 {
		return shim.Error("Cloud Register Error : cloud already exist !")
	}

	creatorBytes, err := stub.GetCreator()
	if err != nil {
		return shim.Error(err.Error())
	}
	cloudId, err := calculateHashFromBytes(creatorBytes)
	if err != nil {
		return shim.Error(err.Error())
	}

	newCloud := &Cloud{
		Msp:     cloudMsp,
		Name:     cloudName,
		CloudId:	cloudId,
		IP:         cloudIP,
		Port:        cloudPort,
		Pk:	      cloudPk,
	}

	newCloudBytes, err := json.Marshal(newCloud)

	if err != nil {
		return shim.Error(fmt.Sprintf("newUser marshal cloud error %s", err))
	}

	if err := stub.PutState(cloudKey, newCloudBytes); err != nil {
		return shim.Error(fmt.Sprintf("put New Cloud error %s", err))
	}

	peerKey, err := stub.CreateCompositeKey("PEER", []string{
		cloudId,
	})

	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	if peerBytes, err := stub.GetState(peerKey); err == nil && len(peerBytes) != 0 {
		return shim.Error("Peer Register Error : peer already exist !")
	}

	newPeer := &Peer{
		Id:		cloudId,
		Type:	"CLOUD",
		Msp:	cloudMsp,
		Name:	cloudName,
	}

	newPeerBytes, err := json.Marshal(newPeer)

	if err != nil {
		return shim.Error(fmt.Sprintf("newPeer marshal user error %s", err))
	}

	if err := stub.PutState(peerKey, newPeerBytes); err != nil {
		return shim.Error(fmt.Sprintf("put newPeer error %s", err))
	}

	return shim.Success(newCloudBytes)
	
}

//cloudLogin
func (t *SimpleChaincode) cloudLogin(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 2 {
		return shim.Error("Error! function 'cloudLogin' not enough args. Need 2 args. ")
	}

	msp := args[0]	
	cloud := args[1]

	cloudKey, err := stub.CreateCompositeKey("CLOUD", []string{
		msp,
		cloud,
	})
	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	cloudBytes, err := stub.GetState(cloudKey)
	if err != nil || len(cloudBytes) == 0 {
		return shim.Error("Error! Cloud not found. ")
	}

	creatorBytes, err := stub.GetCreator()
	if err != nil {
		return shim.Error(err.Error())
	}
	cloudId, err := calculateHashFromBytes(creatorBytes)
	if err != nil {
		return shim.Error(err.Error())
	}


	theCloud := Cloud{}

	if err := json.Unmarshal(cloudBytes, &theCloud); err != nil {
		return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
	}

	if theCloud.CloudId != cloudId {
		return shim.Error("Error! Wrong certificate. ")
	}

	return shim.Success(cloudBytes)
}

//queryCloud
func (t *SimpleChaincode) queryCloud(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 2 {
		return shim.Error("Error! function 'queryCloud' not enough args. Need 2 args. ")
	}

	msp := args[0]	
	cloud := args[1]

	cloudKey, err := stub.CreateCompositeKey("CLOUD", []string{
		msp,		
		cloud,
	})
	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	cloudBytes, err := stub.GetState(cloudKey)
	if err != nil || len(cloudBytes) == 0 {
		return shim.Error("Error! Cloud not found. ")
	}

	return shim.Success(cloudBytes)
}

//queryAllCloud
func (t *SimpleChaincode) queryAllCloud(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 0 {
		return shim.Error("Error! function 'queryAllCloud' does not need any arg. ")
	}

	keys := make([]string, 0)

	result, err := stub.GetStateByPartialCompositeKey("CLOUD", keys)

	if err != nil {
		return shim.Error(fmt.Sprintf("query CSP error: %s", err))
	}
	defer result.Close()

	clouds := make([]*Cloud, 0)

	for result.HasNext() {
		cloudVal, err := result.Next()
		if err != nil {
			return shim.Error(fmt.Sprintf("query error: %s", err))
		}

		theCloud := new(Cloud)

		if err := json.Unmarshal(cloudVal.GetValue(), theCloud); err != nil {
			return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
		}

		clouds = append(clouds, theCloud)
	}

	cloudsBytes, err := json.Marshal(clouds)
	if err != nil {
		return shim.Error(fmt.Sprintf("marshal error: %s", err))
	}

	return shim.Success(cloudsBytes)
}

//cloudUpdate
func (t *SimpleChaincode) cloudUpdate(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 4 {
		return shim.Error("Error! function 'cloudUpdate' not enough args. Need 4 args. ")
	}

	msp := args[0]
	cloud := args[1]
	ip := args[2]
	port := args[3]

	cloudKey, err := stub.CreateCompositeKey("CLOUD", []string{
		msp,
		cloud,
	})
	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	cloudBytes, err := stub.GetState(cloudKey)
	if err != nil || len(cloudBytes) == 0 {
		return shim.Error("Error! Cloud not found. ")
	}

	creatorBytes, err := stub.GetCreator()
	if err != nil {
		return shim.Error(err.Error())
	}
	cloudId, err := calculateHashFromBytes(creatorBytes)
	if err != nil {
		return shim.Error(err.Error())
	}


	theCloud := Cloud{}

	if err := json.Unmarshal(cloudBytes, &theCloud); err != nil {
		return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
	}

	if theCloud.CloudId != cloudId {
		return shim.Error("Error! Wrong certificate. ")
	}

	newCloud := &Cloud{
		Msp:        theCloud.Msp,
		Name:       theCloud.Name,
		CloudId:    theCloud.CloudId,
		IP:         ip,
		Port:       port,
		Pk:	    theCloud.Pk,
	}

	newCloudBytes, err := json.Marshal(newCloud)
	if err != nil {
		return shim.Error(fmt.Sprintf("newCloud marshal error %s", err))
	}

	if err := stub.PutState(cloudKey, newCloudBytes); err != nil {
		return shim.Error(fmt.Sprintf("update cloud error %s", err))
	}	

	return shim.Success(newCloudBytes)
}


//uploadMetadata
func (t *SimpleChaincode) uploadMetadata(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 11 {
		return shim.Error("Error! function 'uploadMetadata' not enough args. Need 11 args. ")
	}

	channel := stub.GetChannelID()

	tx := stub.GetTxID()
	name := args[0]
	cloudMsp := args[1]
	cloud := args[2]
	url := args[3]
	userHash := args[4]
	userSig := args[5]
	
	//uploadTime := time.Now().Format("2006-01-02 15:04:05")
	ts , err := stub.GetTxTimestamp()
	if err != nil {
		return shim.Error(err.Error())
	}
	tsUnix := time.Unix(ts.Seconds,0)
	uploadTime := tsUnix.Format("2006-01-02 15:04:05")
	

	updateTime := uploadTime
	cipherKey := []byte(args[6])
	policy := args[7]
	tag := args[8]
	n, err := strconv.Atoi(args[9])
	if err != nil {
		return shim.Error(fmt.Sprintf("Args[9] is not a 'int' : %s", err))
	}
	latestAuditTime := ""
	latestAuditResult := false
	introduction := args[10]

	creatorBytes, err := stub.GetCreator()
	if err != nil {
		return shim.Error(err.Error())
	}
	userId, err := calculateHashFromBytes(creatorBytes)
	if err != nil {
		return shim.Error(err.Error())
	}

	peerKey, err := stub.CreateCompositeKey("PEER", []string{
		userId,
	})
	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	peerBytes, err := stub.GetState(peerKey)
	if err != nil || len(peerBytes) == 0 {
		return shim.Error("Error! Peer not found. ")
	}

	thePeer := Peer{}

	if err := json.Unmarshal(peerBytes, &thePeer); err != nil {
		return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
	}

	dataKey, err := stub.CreateCompositeKey("DATA", []string{
		channel,
		userId,
		name,
	})
	if err != nil {
		return shim.Error(fmt.Sprintf("create data key error: %s", err))
	}
	if dataBytes, err := stub.GetState(dataKey); err == nil && len(dataBytes) != 0 {
		return shim.Error("Upload Metadata Error : data name already exist !")
	}
	
	var theCipherKeyTable CipherKeyTable

	//theCipherKeyTable := new(AttributesTable)
	if err := json.Unmarshal(cipherKey, &theCipherKeyTable); err != nil {
		return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
	}

	theCipherKey := CipherKeyTable{
		Ct0:     theCipherKeyTable.Ct0,
		Ct:      theCipherKeyTable.Ct,
		CtPrime: theCipherKeyTable.CtPrime,
		Msp:     theCipherKeyTable.Msp,
	}

	newData := &Data{
		Channel: channel,
		Tx: tx,
		OwnerMsp:thePeer.Msp,
		OwnerId:userId,
		Owner:thePeer.Name,
		Name:name,
		CloudMsp:cloudMsp,	
		Cloud:cloud,
		URL:url,
		UserHash:userHash,
		UserSig:userSig,
		CloudHash:"",
		CloudSig:"",
		UploadTime:uploadTime,
		UpdateTime:updateTime,
		CipherKey:theCipherKey,
		Policy:policy,
		Tag:tag,
		N:n,
		LatestAuditTime:latestAuditTime,
		LatestAuditResult:latestAuditResult,
		Introduction:introduction,
	}

	newDataBytes, err := json.Marshal(newData)
	if err != nil {
		return shim.Error(fmt.Sprintf("newData marshal masterKey error %s", err))
	}

	if err := stub.PutState(dataKey, newDataBytes); err != nil {
		return shim.Error(fmt.Sprintf("put NewData error %s", err))
	}

	operationKey, err := stub.CreateCompositeKey("OPERATION", []string{
		channel,
		userId,
		name,
		uploadTime,
	})
	if err != nil {
		return shim.Error(fmt.Sprintf("create operation key error: %s", err))
	}
	if operationBytes, err := stub.GetState(operationKey); err == nil && len(operationBytes) != 0 {
		return shim.Error("operation already exist !")
	}

	newOperation := &Operation{
		Datakey : dataKey,
		OperatorMsp : thePeer.Msp,
		Operator : thePeer.Name,
		Operation : "upload",
		Tx : tx,
		Timestamp : uploadTime,
	}

	newOperationBytes, err := json.Marshal(newOperation)
	if err != nil {
		return shim.Error(fmt.Sprintf("marshal operation error %s", err))
	}

	if err := stub.PutState(operationKey, newOperationBytes); err != nil {
		return shim.Error(fmt.Sprintf("put new operation error %s", err))
	}

	stub.SetEvent(cloudMsp+"_"+cloud+"_DataUploadEvent",newDataBytes)

	return shim.Success(newDataBytes)
}

//updateCloudSignature
func (t *SimpleChaincode) updateCloudSignature(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	
	if len(args) != 6 {
		return shim.Error("Error! function 'updateCloudSignature' not enough args. Need 6 args. ")
	}

	channel := args[0]
	ownerMsp := args[1]
	owner := args[2]
	name := args[3]
	cloudHash := args[4]
	cloudSig := args[5]
	tx := stub.GetTxID()
	//time_t := time.Now().Format("2006-01-02 15:04:05")
	ts , err := stub.GetTxTimestamp()
	if err != nil {
		return shim.Error(err.Error())
	}
	tsUnix := time.Unix(ts.Seconds,0)
	time_t := tsUnix.Format("2006-01-02 15:04:05")

	userKey, err := stub.CreateCompositeKey("USER", []string{
		ownerMsp,
		owner,
	})
	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	userBytes, err := stub.GetState(userKey)
	if err != nil || len(userBytes) == 0 {
		return shim.Error("Error! User not found. ")
	}

	theUser := User{}

	if err := json.Unmarshal(userBytes, &theUser); err != nil {
		return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
	}

	userId := theUser.UserId

	dataKey, err := stub.CreateCompositeKey("DATA", []string{
		channel,
		userId,
		name,
	})
	if err != nil {
		return shim.Error(fmt.Sprintf("create data key error: %s", err))
	}

	dataBytes, err := stub.GetState(dataKey)

	if err != nil || len(dataBytes) == 0 {
		return shim.Error("Error! Data not found. ")
	}

	theData := Data{}

	if err := json.Unmarshal(dataBytes, &theData); err != nil {
		return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
	}

	creatorBytes, err := stub.GetCreator()
	if err != nil {
		return shim.Error(err.Error())
	}
	cloudId, err := calculateHashFromBytes(creatorBytes)
	if err != nil {
		return shim.Error(err.Error())
	}

	cloudKey, err := stub.CreateCompositeKey("CLOUD", []string{
		theData.CloudMsp,
		theData.Cloud,
	})
	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	cloudBytes, err := stub.GetState(cloudKey)
	if err != nil || len(cloudBytes) == 0 {
		return shim.Error("Error! Cloud not found. ")
	}

	theCloud := Cloud{}

	if err := json.Unmarshal(cloudBytes, &theCloud); err != nil {
		return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
	}

	if theCloud.CloudId != cloudId {
		return shim.Error("Error! Wrong certificate. ")
	}

	if theData.CloudHash != "" || theData.CloudSig != ""  {
		return shim.Error(fmt.Sprintf("data already has cloudHash and cloudSignature error: %s", string(dataKey)))
	}

	newData := &Data{
		Channel: theData.Channel,
		Tx: tx,
		OwnerMsp:theData.OwnerMsp,
		OwnerId:theData.OwnerId,
		Owner:theData.Owner,
		Name:theData.Name,
		CloudMsp:theData.CloudMsp,	
		Cloud:theData.Cloud,
		URL:theData.URL,
		UserHash:theData.UserHash,
		UserSig:theData.UserSig,
		CloudHash:cloudHash,
		CloudSig:cloudSig,
		UploadTime:theData.UploadTime,
		UpdateTime:theData.UpdateTime,
		CipherKey:theData.CipherKey,
		Policy:theData.Policy,
		Tag:theData.Tag,
		N:theData.N,
		LatestAuditTime:theData.LatestAuditTime,
		LatestAuditResult:theData.LatestAuditResult,
		Introduction:theData.Introduction,
	}

	newDataBytes, err := json.Marshal(newData)
	if err != nil {
		return shim.Error(fmt.Sprintf("newData marshal masterKey error %s", err))
	}

	if err := stub.PutState(dataKey, newDataBytes); err != nil {
		return shim.Error(fmt.Sprintf("put NewData error %s", err))
	}

	operationKey, err := stub.CreateCompositeKey("OPERATION", []string{
		channel,
		userId,
		name,
		time_t,
	})
	if err != nil {
		return shim.Error(fmt.Sprintf("create operation key error: %s", err))
	}
	if operationBytes, err := stub.GetState(operationKey); err == nil && len(operationBytes) != 0 {
		return shim.Error("operation already exist !")
	}

	newOperation := &Operation{
		Datakey : dataKey,
		OperatorMsp : theData.CloudMsp,
		Operator : theData.Cloud,
		Operation : "update",
		Tx : tx,
		Timestamp : time_t,
	}

	newOperationBytes, err := json.Marshal(newOperation)
	if err != nil {
		return shim.Error(fmt.Sprintf("marshal operation error %s", err))
	}

	if err := stub.PutState(operationKey, newOperationBytes); err != nil {
		return shim.Error(fmt.Sprintf("put new operation error %s", err))
	}

	return shim.Success(newDataBytes)
}

//queryData
func (t *SimpleChaincode) queryData(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 3 {
		return shim.Error("Error! function 'queryData' not enough args. Need 3 args. ")
	}

	channel := args[0]
	userId := args[1]
	name := args[2]

	dataKey, err := stub.CreateCompositeKey("DATA", []string{
		channel,
		userId,
		name,
	})

	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	dataBytes, err := stub.GetState(dataKey)

	if err != nil || len(dataBytes) == 0 {
		return shim.Error("Error! Data not found. ")
	}

	return shim.Success(dataBytes)
}

//queryAllData
func (t *SimpleChaincode) queryAllData(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 0 {
		return shim.Error("Error! function 'queryAllData' does not need any arg. ")
	}

	keys := make([]string, 0)

	result, err := stub.GetStateByPartialCompositeKey("DATA", keys)

	if err != nil {
		return shim.Error(fmt.Sprintf("queryAllData error: %s", err))
	}
	defer result.Close()

	datas := make([]*Data, 0)

	for result.HasNext() {
		dataVal, err := result.Next()
		if err != nil {
			return shim.Error(fmt.Sprintf("query error: %s", err))
		}

		theData := new(Data)

		if err := json.Unmarshal(dataVal.GetValue(), theData); err != nil {
			return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
		}

		datas = append(datas, theData)
	}

	datasBytes, err := json.Marshal(datas)
	if err != nil {
		return shim.Error(fmt.Sprintf("marshal error: %s", err))
	}

	return shim.Success(datasBytes)
}


//queryAllOperations
func (t *SimpleChaincode) queryAllOperation(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 0 {
		return shim.Error("Error! function 'queryAllOperation' does not need any arg. ")
	}

	keys := make([]string, 0)

	result, err := stub.GetStateByPartialCompositeKey("OPERATION", keys)

	if err != nil {
		return shim.Error(fmt.Sprintf("queryAllOperations error: %s", err))
	}
	defer result.Close()

	operations := make([]*Operation, 0)

	for result.HasNext() {
		operationVal, err := result.Next()
		if err != nil {
			return shim.Error(fmt.Sprintf("query error: %s", err))
		}

		theOperation := new(Operation)

		if err := json.Unmarshal(operationVal.GetValue(), theOperation); err != nil {
			return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
		}

		operations = append(operations, theOperation)
	}

	operationsBytes, err := json.Marshal(operations)
	if err != nil {
		return shim.Error(fmt.Sprintf("marshal error: %s", err))
	}

	return shim.Success(operationsBytes)
}

//querySbData
func (t *SimpleChaincode) querySbData(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 2 {
		return shim.Error("Error! function 'querySbData' not enough args. Need 2 args. ")
	}

	channelId := args[0]
	userId := args[1]

	keys := make([]string, 0)

	keys = append(keys, channelId)
	keys = append(keys, userId)

	result, err := stub.GetStateByPartialCompositeKey("DATA", keys)

	if err != nil {
		return shim.Error(fmt.Sprintf("querySbData error: %s", err))
	}
	
	defer result.Close()

	datas := make([]*Data, 0)

	for result.HasNext() {
		dataVal, err := result.Next()
		if err != nil {
			return shim.Error(fmt.Sprintf("query error: %s", err))
		}

		theData := new(Data)

		if err := json.Unmarshal(dataVal.GetValue(), theData); err != nil {
			return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
		}

		datas = append(datas, theData)
	}

	datasBytes, err := json.Marshal(datas)
	if err != nil {
		return shim.Error(fmt.Sprintf("marshal error: %s", err))
	}

	return shim.Success(datasBytes)
}

//queryDataHistory
func (t *SimpleChaincode) queryDataHistory(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 3 {
		return shim.Error("Error! function 'queryDataHistory' not enough args. Need 3 args. ")
	}

	channel := args[0]
	userId := args[1]
	name := args[2]

	dataKey, err := stub.CreateCompositeKey("DATA", []string{
		channel,
		userId,
		name,
	})
	if err != nil {
		return shim.Error(fmt.Sprintf("create data key error: %s", err))
	}

	
	result, err := stub.GetHistoryForKey(dataKey)

	if err != nil {
    		return shim.Error(fmt.Sprintf("get history for key error: %s", err))
	}
	defer result.Close()

	datas := make([]*Data, 0)

	for result.HasNext() {
		dataVal, err := result.Next()
		if err != nil {
			return shim.Error(fmt.Sprintf("query error: %s", err))
		}

		theData := new(Data)

		if err := json.Unmarshal(dataVal.GetValue(), theData); err != nil {
			return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
		}

		datas = append(datas, theData)
	}

	datasBytes, err := json.Marshal(datas)
	if err != nil {
		return shim.Error(fmt.Sprintf("marshal error: %s", err))
	}

	return shim.Success(datasBytes)
}

//preDecryption
func (t *SimpleChaincode)PreDecryption(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	fmt.Println("===================PreDecryption========================\n")

	if len(args) != 3 {
		return shim.Error("Error! function 'PreDecryption' not enough args. Need 3 args. ")
	}

	channel := args[0]
	ownerId := args[1]
	filename := args[2]
	
	newTxid := stub.GetTxID()

	dataKey, err := stub.CreateCompositeKey("DATA", []string{
		channel,
		ownerId,
		filename,
	})

	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	dataBytes, err := stub.GetState(dataKey)

	if err != nil || len(dataBytes) == 0 {
		return shim.Error("Error! Data not found. ")
	}

	theData := Data{}
	json.Unmarshal(dataBytes, &theData)
	cipherKeyTable := theData.CipherKey

	fmt.Println("==>cipherKeyTable")
	fmt.Printf("%+v\n", cipherKeyTable)
	ct0Byte := cipherKeyTable.Ct0
	ctByte := cipherKeyTable.Ct
	ctPrimeAsBytes := cipherKeyTable.CtPrime
	mspAsBytes := cipherKeyTable.Msp
	ct0Asbyte := bytes.Split(ct0Byte, sep)
	ctAsbyte := bytes.Split(ctByte, sep)
	//byte to ct0
	var ct0 [3]*bn256.G2
	for i := 0; i < 3; i++ {
		ct0[i], _ = new(bn256.G2).Unmarshal(ct0Asbyte[i])
	}
	//byte to msp
	var m *abe.MSP
	json.Unmarshal(mspAsBytes, &m)
	//byte to ct
	ct := make([][3]*bn256.G1, len(m.Mat))
	for i := 0; i < len(m.Mat)*3; i++ {
		ct[i/3][i%3], _ = new(bn256.G1).Unmarshal(ctAsbyte[i])
	}
	//byte to ctPrime
	var ctPrime *bn256.GT
	ctPrime, _ = new(bn256.GT).Unmarshal(ctPrimeAsBytes)

	cipher := &abe.FAMECipher{Ct0: ct0, Ct: ct, CtPrime: ctPrime, Msp: m}


	creatorBytes, err := stub.GetCreator()
	if err != nil {
		return shim.Error(err.Error())
	}
	userId, err := calculateHashFromBytes(creatorBytes)
	if err != nil {
		return shim.Error(err.Error())
	}

	peerKey, err := stub.CreateCompositeKey("PEER", []string{
		userId,
	})
	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	peerBytes, err := stub.GetState(peerKey)
	if err != nil || len(peerBytes) == 0 {
		return shim.Error("Error! Peer not found. ")
	}

	thePeer := Peer{}

	if err := json.Unmarshal(peerBytes, &thePeer); err != nil {
		return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
	}

	userKey, err := stub.CreateCompositeKey("USER", []string{
		thePeer.Msp,
		thePeer.Name,
	})

	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	userBytes, err := stub.GetState(userKey)

	if err != nil || len(userBytes) == 0 {
		return shim.Error("Error! User not found. ")
	}

	theUser := User{}
	json.Unmarshal(userBytes, &theUser)
	attributesTable := theUser.AttributeKey

	fmt.Println("==>attributesTable")
	fmt.Printf("%+v\n", attributesTable)
	//period := attributesTable.Period
	k0Byte := attributesTable.K0
	kByte := attributesTable.K
	kPrimeByte := attributesTable.KPrime
	attribToIAsByte := attributesTable.AttribToI

	k0Asbyte := bytes.Split(k0Byte, sep)
	kAsbyte := bytes.Split(kByte, sep)
	kPrimeAsbyte := bytes.Split(kPrimeByte, sep)
	//byte to k0
	var k0 [3]*bn256.G2
	for i := 0; i < 3; i++ {
		k0[i], _ = new(bn256.G2).Unmarshal(k0Asbyte[i])
	}
	//byte to attribToI
	var attribToI map[int]int
	json.Unmarshal(attribToIAsByte, &attribToI)
	//byte to k
	attribMap := make(map[int]bool)
	for k := range attribToI {
		attribMap[k] = true
	}
	countAttr := 0
	for i := 0; i < len(cipher.Msp.Mat); i++ {
		if attribMap[cipher.Msp.RowToAttrib[i]] {
			countAttr += 1
		}
	}
	k := make([][3]*bn256.G1, countAttr)
	for i := 0; i < countAttr*3; i++ {
		k[i/3][i%3], _ = new(bn256.G1).Unmarshal(kAsbyte[i])
	}
	//byte to kPrime
	var kPrime [3]*bn256.G1
	for i := 0; i < 3; i++ {
		kPrime[i], _ = new(bn256.G1).Unmarshal(kPrimeAsbyte[i])
	}

	keys := &abe.FAMEAttribKeys{K0: k0, K: k, KPrime: kPrime, AttribToI: attribToI}

	mkBytes, err := stub.GetState("MASTERKEY")
	if err != nil || len(mkBytes) == 0 {
		return shim.Error("masterKey not found")
	}

	masterkey := new(MasterKey)
	if err := json.Unmarshal(mkBytes, masterkey); err != nil {
		return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
	}

	mkpkG2Byte := masterkey.PkG2
	mkpkGTByte := masterkey.PkGT

	pkG2Asbyte := bytes.Split(mkpkG2Byte, sep) //split转 []byte 为[][]byte
	pkGTAsbyte := bytes.Split(mkpkGTByte, sep)

	var pkG2 [2]*bn256.G2
	var pkGT [2]*bn256.GT
	for i := 0; i < 2; i++ {
		pkG2[i], _ = new(bn256.G2).Unmarshal(pkG2Asbyte[i])
		pkGT[i], _ = new(bn256.GT).Unmarshal(pkGTAsbyte[i])
	}

	pubKey := &abe.FAMEPubKey{PartG2: pkG2, PartGT: pkGT}

	token, err := fame.PreDecrypt(cipher, keys, pubKey)

	if err != nil {
		//theReputation := theUser.Reputation
		log.Println(err)
		newTsus := theUser.Tsus
		newTfail := theUser.Tfail + 1

		newReputation := calculateReputation(newTsus, newTfail)

		newUser := &User{
			Msp:		  theUser.Msp,
			Username:     theUser.Username,
			UserId:		  theUser.UserId,
			AuditPk:      theUser.AuditPk,
			Tsus:         newTsus,
			Tfail:        newTfail,
			Reputation:   newReputation,
			Attribute:    theUser.Attribute,
			Period:       theUser.Period,
			AttributeKey: theUser.AttributeKey,
			SigPk:	      theUser.SigPk,
		}

		newUserBytes, err := json.Marshal(newUser)
		if err != nil {
			return shim.Error(fmt.Sprintf("newUser marshal masterKey error %s", err))
		}

		if err := stub.PutState(userKey, newUserBytes); err != nil {
			return shim.Error(fmt.Sprintf("put NewUser error %s", err))
		}

		return shim.Success(newUserBytes)
	}

	fmt.Println("midcipher==> ", token)
	//token as byte
	tokenAsbyte := make([][]byte, 256/8*3)
	for i := 0; i < 3; i++ {
		tokenAsbyte[i] = token.TkPairing[i].Marshal()
	}

	var shareRecord = Share{}

	shareRecord.TkPairing = bytes.Join(tokenAsbyte, sep)
	shareRecord.Datakey = dataKey
	shareRecord.OwnerMsp = theData.OwnerMsp
	shareRecord.DataOwner = theData.Owner
	shareRecord.UserMsp = thePeer.Msp
    shareRecord.DataUser = thePeer.Name
    shareRecord.Tx = newTxid
	//shareRecord.Timestamp = time.Now().Format("2006-01-02 15:04:05")
	ts , err := stub.GetTxTimestamp()
	if err != nil {
		return shim.Error(err.Error())
	}
	tsUnix := time.Unix(ts.Seconds,0)
	shareRecord.Timestamp = tsUnix.Format("2006-01-02 15:04:05")

	shareRecordAsBytes, err := json.Marshal(shareRecord)

	if err != nil {
		return shim.Error(fmt.Sprintf("Error! MidKey marshal error %s", err))
	}

	shareKey, err := stub.CreateCompositeKey("SHARE", []string{
		channel,
		ownerId,
		filename,
		shareRecord.Timestamp,
	})

	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	if err := stub.PutState(shareKey, shareRecordAsBytes); err != nil {
		return shim.Error(fmt.Sprintf("put shareRecord error %s", err))
	}

	//theReputation := theUser.Reputation
	newTsus := theUser.Tsus + 1
	newTfail := theUser.Tfail

	newReputation := calculateReputation(newTsus, newTfail)

	newUser := &User{
		Msp:		  theUser.Msp,
		Username:     theUser.Username,
		UserId:		  theUser.UserId,
		AuditPk:      theUser.AuditPk,
		Tsus:         newTsus,
		Tfail:        newTfail,
		Reputation:   newReputation,
		Attribute:    theUser.Attribute,
		Period:       theUser.Period,
		AttributeKey: theUser.AttributeKey,
		SigPk:	      theUser.SigPk,
	}


	newUserBytes, err := json.Marshal(newUser)
	if err != nil {
		return shim.Error(fmt.Sprintf("newUser marshal masterKey error %s", err))
	}

	if err := stub.PutState(userKey, newUserBytes); err != nil {
		return shim.Error(fmt.Sprintf("put NewUser error %s", err))
	}

	operationKey, err := stub.CreateCompositeKey("OPERATION", []string{
		channel,
		ownerId,
		filename,
		shareRecord.Timestamp,
	})
	if err != nil {
		return shim.Error(fmt.Sprintf("create operation key error: %s", err))
	}
	if operationBytes, err := stub.GetState(operationKey); err == nil && len(operationBytes) != 0 {
		return shim.Error("operation already exist !")
	}

	newOperation := &Operation{
		Datakey : dataKey,
		OperatorMsp : thePeer.Msp,
		Operator : thePeer.Name,
		Operation : "share",
		Tx : newTxid,
		Timestamp : shareRecord.Timestamp,
	}

	newOperationBytes, err := json.Marshal(newOperation)
	if err != nil {
		return shim.Error(fmt.Sprintf("marshal operation error %s", err))
	}

	if err := stub.PutState(operationKey, newOperationBytes); err != nil {
		return shim.Error(fmt.Sprintf("put new operation error %s", err))
	}

	return shim.Success(shareRecordAsBytes)

}

//queryShare
func (t *SimpleChaincode) queryShare(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 4 {
		return shim.Error("Error! function 'queryShare' not enough args. Need 4 args. ")
	}

	channel := args[0]
	ownerId := args[1]
	name := args[2]
	timestamp := args[3]

	shareKey, err := stub.CreateCompositeKey("SHARE", []string{
		channel,
		ownerId,
		name,
		timestamp,
	})

	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	shareBytes, err := stub.GetState(shareKey)

	if err != nil || len(shareBytes) == 0 {
		return shim.Error("Error! Data not found. ")
	}

	return shim.Success(shareBytes)
}

//queryAllShare
func (t *SimpleChaincode) queryAllShare(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 0 {
		return shim.Error("Error! function 'queryAllShare' does not need any arg. ")
	}

	keys := make([]string, 0)

	result, err := stub.GetStateByPartialCompositeKey("SHARE", keys)

	if err != nil {
		return shim.Error(fmt.Sprintf("queryAllShare error: %s", err))
	}
	defer result.Close()

	shares := make([]*Share, 0)

	for result.HasNext() {
		shareVal, err := result.Next()
		if err != nil {
			return shim.Error(fmt.Sprintf("query error: %s", err))
		}

		theShare := new(Share)

		if err := json.Unmarshal(shareVal.GetValue(), theShare); err != nil {
			return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
		}

		shares = append(shares, theShare)
	}

	sharesBytes, err := json.Marshal(shares)
	if err != nil {
		return shim.Error(fmt.Sprintf("marshal error: %s", err))
	}

	return shim.Success(sharesBytes)
}

//queryDataShare
func (t *SimpleChaincode) queryDataShare(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 3 {
		return shim.Error("Error! function 'queryDataAllShare' does not have enough args. need 3 args. ")
	}

	channel := args[0]
	ownerId := args[1]
	name := args[2]

	keys := make([]string, 0)

	keys = append(keys, channel)
	keys = append(keys, ownerId)
	keys = append(keys, name)

	result, err := stub.GetStateByPartialCompositeKey("SHARE", keys)

	if err != nil {
		return shim.Error(fmt.Sprintf("queryDataAllShare error: %s", err))
	}
	defer result.Close()

	shares := make([]*Share, 0)

	for result.HasNext() {
		shareVal, err := result.Next()
		if err != nil {
			return shim.Error(fmt.Sprintf("query error: %s", err))
		}

		theShare := new(Share)

		if err := json.Unmarshal(shareVal.GetValue(), theShare); err != nil {
			return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
		}

		shares = append(shares, theShare)
	}

	sharesBytes, err := json.Marshal(shares)
	if err != nil {
		return shim.Error(fmt.Sprintf("marshal error: %s", err))
	}

	return shim.Success(sharesBytes)
}

//queryDataOperation
func (t *SimpleChaincode) queryDataOperation(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 3 {
		return shim.Error("Error! function 'queryDataOperation' does not have enough args. need 3 args. ")
	}

	channel := args[0]
	ownerId := args[1]
	name := args[2]

	keys := make([]string, 0)

	keys = append(keys, channel)
	keys = append(keys, ownerId)
	keys = append(keys, name)

	result, err := stub.GetStateByPartialCompositeKey("OPERATION", keys)

	if err != nil {
		return shim.Error(fmt.Sprintf("queryDataOperations error: %s", err))
	}
	defer result.Close()

	operations := make([]*Operation, 0)

	for result.HasNext() {
		operationVal, err := result.Next()
		if err != nil {
			return shim.Error(fmt.Sprintf("query error: %s", err))
		}

		theOperation := new(Operation)

		if err := json.Unmarshal(operationVal.GetValue(), theOperation); err != nil {
			return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
		}

		operations = append(operations, theOperation)
	}

	operationsBytes, err := json.Marshal(operations)
	if err != nil {
		return shim.Error(fmt.Sprintf("marshal error: %s", err))
	}

	return shim.Success(operationsBytes)
}

//queryOperation
func (t *SimpleChaincode) queryOperation(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 4 {
		return shim.Error("Error! function 'queryShare' not enough args. Need 4 args. ")
	}

	channel := args[0]
	ownerId := args[1]
	name := args[2]
	timestamp := args[3]

	operationKey, err := stub.CreateCompositeKey("OPERATION", []string{
		channel,
		ownerId,
		name,
		timestamp,
	})

	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	operationBytes, err := stub.GetState(operationKey)

	if err != nil || len(operationBytes) == 0 {
		return shim.Error("Error! Data not found. ")
	}

	return shim.Success(operationBytes)
}

//auditData
func (t *SimpleChaincode) auditData(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 13 {
		return shim.Error("Error! function 'audit' not enough args. Need 13 args. ")
	}

	channel := args[0]
	ownerid := args[1]
	dataname := args[2]
	ownermsp := args[3]
	dataowner := args[4]
	auditmsp := args[5]
	auditor := args[6]
	cloudmsp := args[7]
	cloudid := args[8]
	c := args[9]
	timestamp := args[10]
	challenge := args[11]
	proof := ""
	result := ""
	txchallenge := args[12]
	txverify := ""
	
	var auditRecord = Audit{}
	real_datakey, err :=  stub.CreateCompositeKey("DATA", []string{
		channel,
		ownerid,
		dataname,
	})

	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	dataBytes, err := stub.GetState(real_datakey)

	if err != nil || len(dataBytes) == 0 {
		return shim.Error("Error! Data not found. ")
	}

	
	auditRecord.OwnerMsp = ownermsp
	auditRecord.Owner = dataowner
	auditRecord.AuditorMsp = auditmsp
	auditRecord.Auditor = auditor
	auditRecord.CloudMsp = cloudmsp
	auditRecord.Cloud = cloudid
	auditRecord.C = c
	auditRecord.Timestamp = timestamp
	auditRecord.Challenge = challenge
	auditRecord.Proof = proof
	auditRecord.Result = result
	auditRecord.TxChallenge = txchallenge
	auditRecord.TxVerify = txverify

	temp_datakey, err := stub.CreateCompositeKey("DATA", []string{
		channel,
		ownerid,
		dataname,
	})

	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	dataBytes, err = stub.GetState(temp_datakey)
	
	if err != nil || len(dataBytes) == 0 {
		return shim.Error("datakey error")
	}

	auditRecord.Datakey = temp_datakey


	auditRecordAsBytes, err := json.Marshal(auditRecord)

	if err != nil {
		return shim.Error(fmt.Sprintf("Error! audit marshal error %s", err))
	}

	//KEY = AUDIT + ChannelID + UserId + Name + Timestamp

	auditKey, err := stub.CreateCompositeKey("AUDIT", []string{
		channel,
		ownerid,
		dataname,
		auditRecord.Timestamp,
	})

	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	if tryBytes, err := stub.GetState(auditKey); err == nil && len(tryBytes) != 0 {
		return shim.Error("audit record already exist !")
	}

	if err := stub.PutState(auditKey, auditRecordAsBytes); err != nil {
		return shim.Error(fmt.Sprintf("put auditRecord error %s", err))
	}

	//generate Operation Record

	operationKey, err := stub.CreateCompositeKey("OPERATION", []string{
		channel,
		ownerid,
		dataname,
		auditRecord.Timestamp,
	})
	if err != nil {
		return shim.Error(fmt.Sprintf("create operation key error: %s", err))
	}
	if operationBytes, err := stub.GetState(operationKey); err == nil && len(operationBytes) != 0 {
		return shim.Error("operation already exist !")
	}

	newOperation := &Operation{
		Datakey : auditRecord.Datakey,
		OperatorMsp: auditRecord.AuditorMsp,
		Operator : auditRecord.Auditor,
		Operation : "audit",
		Tx : txchallenge,
		Timestamp : auditRecord.Timestamp,
	}

	newOperationBytes, err := json.Marshal(newOperation)
	if err != nil {
		return shim.Error(fmt.Sprintf("marshal operation error %s", err))
	}

	if err := stub.PutState(operationKey, newOperationBytes); err != nil {
		return shim.Error(fmt.Sprintf("put new operation error %s", err))
	}

	return shim.Success(auditRecordAsBytes)

}

//updateAuditResult
func (t *SimpleChaincode) updateAuditResult(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	
	if len(args) != 6 {
		return shim.Error("Error! function 'updateAuditResult' not enough args. Need 6 args. ")
	}

	channel := args[0]
	ownerId := args[1]
	name := args[2]
	auditTime := args[3]
	proof := args[4]
	auditResult := args[5]
	

	tx := stub.GetTxID()

	//update Audit Record
	auditKey, err := stub.CreateCompositeKey("AUDIT", []string{
		channel,
		ownerId,
		name,
		auditTime,
	})

	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	auditBytes, err := stub.GetState(auditKey)
	
	if err != nil && len(auditBytes) == 0 {
		return shim.Error("Error! Audit record does not exsit.")
	}

	theAudit := Audit{}

	if err := json.Unmarshal(auditBytes, &theAudit); err != nil {
		return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
	}

	newAudit := &Audit{
		Datakey:theAudit.Datakey,
		OwnerMsp:theAudit.OwnerMsp,
		Owner:theAudit.Owner,
		AuditorMsp:theAudit.AuditorMsp,
		Auditor:theAudit.Auditor,
		CloudMsp:theAudit.CloudMsp,
		Cloud:theAudit.Cloud,
		C:theAudit.C,
		Timestamp:theAudit.Timestamp,
		Challenge:theAudit.Challenge,
		Proof : proof,
		Result : auditResult,
		TxChallenge : theAudit.TxChallenge,
		TxVerify : tx,
	}

	newAuditBytes, err := json.Marshal(newAudit)
	if err != nil {
		return shim.Error(fmt.Sprintf("newAudit marshal masterKey error %s", err))
	}

	if err := stub.PutState(auditKey, newAuditBytes); err != nil {
		return shim.Error(fmt.Sprintf("put NewAudit error %s", err))
	}

	//update Data Record

	dataKey, err := stub.CreateCompositeKey("DATA", []string{
		channel,
		ownerId,
		name,
	})

	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	dataBytes, err := stub.GetState(dataKey)

	if err != nil || len(dataBytes) == 0 {
		return shim.Error("Error! Data not found. ")
	}

	theData := Data{}

	if err := json.Unmarshal(dataBytes, &theData); err != nil {
		return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
	}

	bool_result,err := strconv.ParseBool(auditResult)

	newData := &Data{
		Channel: theData.Channel,
		Tx: tx,
		OwnerMsp:theData.OwnerMsp,
		OwnerId:theData.OwnerId,
		Owner:theData.Owner,
		Name:theData.Name,
		CloudMsp:theData.CloudMsp,	
		Cloud:theData.Cloud,
		URL:theData.URL,
		UserHash:theData.UserHash,
		UserSig:theData.UserSig,
		CloudHash:theData.CloudHash,
		CloudSig:theData.CloudSig,
		UploadTime:theData.UploadTime,
		UpdateTime:theData.UpdateTime,
		CipherKey:theData.CipherKey,
		Policy:theData.Policy,
		Tag:theData.Tag,
		N:theData.N,
		LatestAuditTime:auditTime,
		LatestAuditResult: bool_result,
		Introduction:theData.Introduction,
	}

	newDataBytes, err := json.Marshal(newData)
	if err != nil {
		return shim.Error(fmt.Sprintf("newData marshal masterKey error %s", err))
	}

	if err := stub.PutState(dataKey, newDataBytes); err != nil {
		return shim.Error(fmt.Sprintf("put NewData error %s", err))
	}

	//generate Operation Record

	ts , err := stub.GetTxTimestamp()
	if err != nil {
		return shim.Error(err.Error())
	}
	tsUnix := time.Unix(ts.Seconds,0)
	updateTime := tsUnix.Format("2006-01-02 15:04:05")

	operationKey, err := stub.CreateCompositeKey("OPERATION", []string{
		channel,
		ownerId,
		name,
		updateTime,
	})
	if err != nil {
		return shim.Error(fmt.Sprintf("create operation key error: %s", err))
	}
	if operationBytes, err := stub.GetState(operationKey); err == nil && len(operationBytes) != 0 {
		return shim.Error("operation already exist !")
	}

	newOperation := &Operation{
		Datakey : dataKey,
		OperatorMsp: newAudit.CloudMsp,
		Operator : newAudit.Cloud,
		Operation : "update",
		Tx : tx,
		Timestamp : updateTime,
	}

	newOperationBytes, err := json.Marshal(newOperation)
	if err != nil {
		return shim.Error(fmt.Sprintf("marshal operation error %s", err))
	}

	if err := stub.PutState(operationKey, newOperationBytes); err != nil {
		return shim.Error(fmt.Sprintf("put new operation error %s", err))
	}

	return shim.Success(newAuditBytes)
}

//updateData
func (t *SimpleChaincode) updateData(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 11 {
		return shim.Error("Error! function 'updateData' not enough args. Need 11 args. ")
	}

	channel := args[0]
	ownerId := args[1]
	name := args[2]
	userHash := args[3]
	userSig := args[4]
	
	cipherKey := []byte(args[5])

	var theCipherKeyTable CipherKeyTable

	//theCipherKeyTable := new(AttributesTable)
	if err := json.Unmarshal(cipherKey, &theCipherKeyTable); err != nil {
		return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
	}

	theCipherKey := CipherKeyTable{
		Ct0:     theCipherKeyTable.Ct0,
		Ct:      theCipherKeyTable.Ct,
		CtPrime: theCipherKeyTable.CtPrime,
		Msp:     theCipherKeyTable.Msp,
	}

	policy := args[6]
	tag := args[7]
	//updateTime := time.Now().Format("2006-01-02 15:04:05")
	ts , err := stub.GetTxTimestamp()
	if err != nil {
		return shim.Error(err.Error())
	}
	tsUnix := time.Unix(ts.Seconds,0)
	updateTime := tsUnix.Format("2006-01-02 15:04:05")
	n, err := strconv.Atoi(args[8])
	if err != nil {
		return shim.Error(fmt.Sprintf("Args[10] is not a 'int' : %s", err))
	}
	introduction := args[9]
	isNewFile := args[10]

	tx := stub.GetTxID()

	dataKey, err := stub.CreateCompositeKey("DATA", []string{
		channel,
		ownerId,
		name,
	})

	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	dataBytes, err := stub.GetState(dataKey)

	if err != nil || len(dataBytes) == 0 {
		return shim.Error("Error! Data not found. ")
	}

	theData := Data{}

	if err := json.Unmarshal(dataBytes, &theData); err != nil {
		return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
	}

	creatorBytes, err := stub.GetCreator()
	if err != nil {
		return shim.Error(err.Error())
	}
	userId, err := calculateHashFromBytes(creatorBytes)
	if err != nil {
		return shim.Error(err.Error())
	}

	if ownerId != userId {
		return shim.Error("Error! Invalid certificate. ")
	} 


	cloudHash := theData.CloudHash
	cloudSig := theData.CloudSig

	if isNewFile == "T" {
		cloudHash = ""
		cloudSig = ""
	}

	

	newData := &Data{
		Channel: theData.Channel,
		Tx: tx,
		OwnerMsp:theData.OwnerMsp,
		OwnerId:theData.OwnerId,
		Owner:theData.Owner,
		Name:theData.Name,
		CloudMsp:theData.CloudMsp,	
		Cloud:theData.Cloud,
		URL:theData.URL,
		UserHash: userHash,
		UserSig:userSig,
		CloudHash:cloudHash,
		CloudSig:cloudSig,
		UploadTime:theData.UploadTime,
		UpdateTime:updateTime,
		CipherKey:theCipherKey,
		Policy:policy,
		Tag:tag,
		N:n,
		LatestAuditTime:theData.LatestAuditTime,
		LatestAuditResult: theData.LatestAuditResult,
		Introduction:introduction,
	}

	newDataBytes, err := json.Marshal(newData)
	if err != nil {
		return shim.Error(fmt.Sprintf("newData marshal masterKey error %s", err))
	}

	if err := stub.PutState(dataKey, newDataBytes); err != nil {
		return shim.Error(fmt.Sprintf("put NewData error %s", err))
	}

	operationKey, err := stub.CreateCompositeKey("OPERATION", []string{
		channel,
		ownerId,
		name,
		updateTime,
	})
	if err != nil {
		return shim.Error(fmt.Sprintf("create operation key error: %s", err))
	}
	if operationBytes, err := stub.GetState(operationKey); err == nil && len(operationBytes) != 0 {
		return shim.Error("operation already exist !")
	}

	newOperation := &Operation{
		Datakey : dataKey,
		OperatorMsp : theData.OwnerMsp,
		Operator : theData.Owner,
		Operation : "update",
		Tx : tx,
		Timestamp : updateTime,
	}

	newOperationBytes, err := json.Marshal(newOperation)
	if err != nil {
		return shim.Error(fmt.Sprintf("marshal operation error %s", err))
	}

	if err := stub.PutState(operationKey, newOperationBytes); err != nil {
		return shim.Error(fmt.Sprintf("put new operation error %s", err))
	}

	if isNewFile == "T" {
		stub.SetEvent(theData.CloudMsp+"_"+theData.Cloud+"_DataUploadEvent",newDataBytes)
	}

	return shim.Success(newDataBytes)
}

//queryAudit
func (t *SimpleChaincode) queryAudit(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 4 {
		return shim.Error("Error! function 'queryAudit' not enough args. Need 4 args. ")
	}
	channel := args[0]
	userid := args[1]
	dataname := args[2]
	timestamp := args[3]

	auditKey, err := stub.CreateCompositeKey("AUDIT", []string{
		channel,
		userid,
		dataname,
		timestamp,
	})
	if err != nil {
		return shim.Error(fmt.Sprintf("create key error: %s", err))
	}

	auditBytes, err := stub.GetState(auditKey)
	if err != nil || len(auditBytes) == 0 {
		return shim.Error("Error! User not found. ")
	}

	return shim.Success(auditBytes)
}
//queryAllAudit
func (t *SimpleChaincode) queryAllAudit(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 0 {
		return shim.Error("Error! function 'queryAllAudit' does not need any arg. ")
	}

	keys := make([]string, 0)

	result, err := stub.GetStateByPartialCompositeKey("AUDIT", keys)

	if err != nil {
		return shim.Error(fmt.Sprintf("queryAllAudit error: %s", err))
	}
	defer result.Close()

	audits := make([]*Audit, 0)

	for result.HasNext() {
		auditVal, err := result.Next()
		if err != nil {
			return shim.Error(fmt.Sprintf("query error: %s", err))
		}

		theAudit := new(Audit)

		if err := json.Unmarshal(auditVal.GetValue(), theAudit); err != nil {
			return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
		}

		audits = append(audits, theAudit)
	}

	auditsBytes, err := json.Marshal(audits)
	if err != nil {
		return shim.Error(fmt.Sprintf("marshal error: %s", err))
	}

	return shim.Success(auditsBytes)
}
//queryDataAudit
func (t *SimpleChaincode) queryDataAudit(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 3 {
		return shim.Error("Error! function 'queryDataAudit' not enough args. Need 3 args. ")
	}

	channel := args[0]
	userid := args[1]
	dataname := args[2]


	keys := make([]string, 0)
	keys = append(keys, channel)
	keys = append(keys, userid)
	keys = append(keys, dataname)

	result, err := stub.GetStateByPartialCompositeKey("AUDIT", keys)

	if err != nil {
		return shim.Error(fmt.Sprintf("queryAllAudit error: %s", err))
	}
	defer result.Close()

	audits := make([]*Audit, 0)

	for result.HasNext() {
		auditVal, err := result.Next()
		if err != nil {
			return shim.Error(fmt.Sprintf("query error: %s", err))
		}

		theAudit := new(Audit)

		if err := json.Unmarshal(auditVal.GetValue(), theAudit); err != nil {
			return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
		}

		audits = append(audits, theAudit)
	}

	auditsBytes, err := json.Marshal(audits)
	if err != nil {
		return shim.Error(fmt.Sprintf("marshal error: %s", err))
	}

	return shim.Success(auditsBytes)
}

//queryDataLatestAudit
func (t *SimpleChaincode) queryDataLatestAudit(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 3 {
		return shim.Error("Error! function 'queryDataLatestAudit' not enough args. Need 3 args. ")
	}

	channel := args[0]
	userid := args[1]
	dataname := args[2]


	keys := make([]string, 0)
	keys = append(keys, channel)
	keys = append(keys, userid)
	keys = append(keys, dataname)

	result, err := stub.GetStateByPartialCompositeKey("AUDIT", keys)

	if err != nil {
		return shim.Error(fmt.Sprintf("queryAllAudit error: %s", err))
	}
	defer result.Close()

	temp_audit := []byte("")
	temp_time := ""

	for result.HasNext() {
		auditVal, err := result.Next()
		if err != nil {
			return shim.Error(fmt.Sprintf("query error: %s", err))
		}

		theAudit := new(Audit)

		if err := json.Unmarshal(auditVal.GetValue(), theAudit); err != nil {
			return shim.Error(fmt.Sprintf("unmarshal error: %s", err))
		}

		if strings.Compare(theAudit.Timestamp,temp_time) > 0 {
			temp_time = theAudit.Timestamp
			temp_audit = auditVal.GetValue()
		}

	}

	return shim.Success(temp_audit)
}


func main() {
	err := shim.Start(new(SimpleChaincode))
	if err != nil {
		fmt.Printf("Error starting Simple chaincode: %s", err)
	}
}
