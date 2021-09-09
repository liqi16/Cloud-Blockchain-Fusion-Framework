echo "cd $GOPATH/src/github.com/hyperledger/fabric/scripts/fabric-samples/cbfm/client"
cd $GOPATH/src/github.com/hyperledger/fabric/scripts/fabric-samples/cbfm/client
echo "rm -rf $GOPATH/src/github.com/hyperledger/fabric/scripts/fabric-samples/cbfm/client/wallet/*"
rm -rf $GOPATH/src/github.com/hyperledger/fabric/scripts/fabric-samples/cbfm/client/wallet/*
echo "mvn clean package"
mvn clean package
echo "cp target/cbfm-client-jar-with-dependencies.jar cbfm-client.jar"
cp target/cbfm-client-jar-with-dependencies.jar cbfm-client.jar

