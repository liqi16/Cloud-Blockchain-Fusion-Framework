#lsexport JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
echo "cd $GOPATH/src/github.com/hyperledger/fabric/scripts/fabric-samples/cbfm/ca"
cd $GOPATH/src/github.com/hyperledger/fabric/scripts/fabric-samples/cbfm/ca
echo "rm -rf $GOPATH/src/github.com/hyperledger/fabric/scripts/fabric-samples/cbfm/ca/wallet/*"
rm -rf $GOPATH/src/github.com/hyperledger/fabric/scripts/fabric-samples/cbfm/ca/wallet/*
echo "mvn clean package"
mvn clean package
echo "cp target/cbfm-ca-jar-with-dependencies.jar cbfm-ca.jar"
cp target/cbfm-ca-jar-with-dependencies.jar cbfm-ca.jar
#echo "java -cp target/cbfm-ca-jar-with-dependencies.jar org.example.Main"
#java -cp target/cbfm-ca-jar-with-dependencies.jar org.example.Main
