package main

import (
	"encoding/base64"
	"fmt"
	"goEncrypt"
	"io/ioutil"
	"os"
	"strings"
)

func readFileToBytes(filename string) []byte {
	if contents, err := ioutil.ReadFile(filename); err == nil {
		return contents
	} else {
		fmt.Println(err)
		return []byte("")
	}
}

func encryptFile(keyString string, fileBytes []byte) string {
	key := []byte(keyString)
	plaintext := fileBytes
	cryptText, err := goEncrypt.DesCbcEncrypt(plaintext, key) //得到密文,可以自己传入初始化向量,如果不传就使用默认的初始化向量,8字节
	if err != nil {
		fmt.Println(err)
		return ""
	} else {
		result := base64.StdEncoding.EncodeToString(cryptText)
		return result
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

func decryptFile(keyString string, fileBase64Bytes []byte) string {
	key := []byte(keyString)

	base64FileString := string(fileBase64Bytes)

	base64FileString = strings.Replace(base64FileString, " ", "", -1)

	result, err := base64.StdEncoding.DecodeString(base64FileString)
	if err != nil {
		fmt.Println(err)
		return ""
	}else{
		plaintext, err := goEncrypt.DesCbcDecrypt(result, key) //解密得到密文,可以自己传入初始化向量,如果不传就使用默认的初始化向量,8字节
		if err != nil {
			fmt.Println(err)
			return ""
		}
		return string(plaintext)
	}

}

// args : 1.cmd = {enc,dec} 2. symkey 3. filename
func main() {
	
	/*
	var keyString string

	_ , err := fmt.Scanln(&keyString)
	if err !=nil{
		fmt.Println("Error! Key input error.")
		return
	}

	filename := "./test.txt"
	*/

	cmd := os.Args[1]

	keyString := os.Args[2]
	filename := os.Args[3]

	if len(keyString)!=8 {
		fmt.Println("Error! Key length is 8 characters.")
		return
	}
	
	if cmd == "enc" {

		fileContentBytes := readFileToBytes(filename)
		if len(fileContentBytes) == 0 {
			fmt.Println("Error! The file is empty.")
			return
		}

		cipherFileString := encryptFile(keyString,fileContentBytes)
		if cipherFileString == "" {
			fmt.Println("Error! Encrypt file error.")
			return
		}

		writeFileResult := writeStringToFile(filename+"_cipherText", cipherFileString)
		if !writeFileResult {
			fmt.Println("Error! Write file error.")
			return
		}

	}else if cmd == "dec" {

		cipherFileBytes := readFileToBytes(filename)
		if len(cipherFileBytes) == 0 {
			fmt.Println("Error! The file is empty.")
			return
		}

		//fmt.Println(string(cipherFileBytes))

		plaintext := decryptFile(keyString,cipherFileBytes)
		if plaintext == "" {
			fmt.Println("Error! File decrypt error.")
			return
		}

		//fmt.Println(plaintext)
		writeFileResult := writeStringToFile(filename + "_plainText",plaintext)
		if !writeFileResult {
			fmt.Println("Error! File write error!")
			return
		}
	
	}else {
		fmt.Println("Error! cmd = {enc,dec}.")
		return
	}

	return
	/*

	key := []byte("ejdhcy34")

	plaintext := []byte("a") //明文
	fmt.Println("明文为：", string(plaintext))

	// 传入明文和自己定义的密钥，密钥为8字节
	cryptText, err := goEncrypt.DesCbcEncrypt(plaintext, key) //得到密文,可以自己传入初始化向量,如果不传就使用默认的初始化向量,8字节
	if err != nil {
		fmt.Println(err)
	}
	fmt.Println("DES的CBC模式加密后的密文为:", base64.StdEncoding.EncodeToString(cryptText))

	newplaintext, err := goEncrypt.DesCbcDecrypt(cryptText, key) //解密得到密文,可以自己传入初始化向量,如果不传就使用默认的初始化向量,8字节
	if err != nil {
		fmt.Println(err)
	}

	fmt.Println("DES的CBC模式解密完：", string(newplaintext))*/
}

