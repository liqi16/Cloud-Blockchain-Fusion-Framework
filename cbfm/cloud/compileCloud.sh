echo "cd $GOPATH/src/github.com/hyperledger/fabric/scripts/fabric-samples/cbfm/cloud"
cd $GOPATH/src/github.com/hyperledger/fabric/scripts/fabric-samples/cbfm/cloud
echo "rm -rf $GOPATH/src/github.com/hyperledger/fabric/scripts/fabric-samples/cbfm/cloud/wallet/*"
rm -rf $GOPATH/src/github.com/hyperledger/fabric/scripts/fabric-samples/cbfm/cloud/wallet/*
echo "rm -rf $GOPATH/src/github.com/hyperledger/fabric/scripts/fabric-samples/cbfm/cloud/Org*MSP_*"
rm -rf $GOPATH/src/github.com/hyperledger/fabric/scripts/fabric-samples/cbfm/cloud/Org*MSP_*
echo "mvn clean package"
mvn clean package
echo "cp target/cbfm-cloud-jar-with-dependencies.jar cbfm-cloud.jar"
cp target/cbfm-cloud-jar-with-dependencies.jar cbfm-cloud.jar
#echo "java -cp target/cbfm-cloud-jar-with-dependencies.jar org.example.Main"
#java -cp target/cbfm-cloud-jar-with-dependencies.jar org.example.Main
