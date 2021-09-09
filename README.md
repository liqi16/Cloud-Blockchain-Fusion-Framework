# Cloud-Blockchain-Fusion-Framework
Implementing the framework from our paper: "CBFF: A Cloud-Blockchain Fusion Framework Ensuring Data Accountability for Multi-cloud Environments"

In this paper, we propose a Cloud-Blockchain Fusion Framework (CBFF) to relieve the “data island” between multiple clouds. CBFF improves the security of cloud data by designing the cooperation between clouds and blockchain. It designs a unified data Naming and Addressing Mechanism to publish and locate cloud data globally in a multi-cloud environment. Also, it proposes the Operation Tracing Mechanism to achieve reliable operation logging and tracing. With CBFF, we present a prototype implementation system using Hyperledger Fabric blockchain and Alibaba Cloud Computing. Our system could provide secure data uploading, sharing, and updating among multiple clouds. 

##Requirements

  Hyperledger Fabric v1.4.4(https://github.com/hyperledger/fabric/tree/v1.4.4)
  Fabric Samples v1.4.4(https://github.com/hyperledger/fabric-samples/tree/v1.4.4)

##Install
 - Put folder “cbfm” into ./fabric-samples
 - Put folder chaincode into ./fabric-samples
 - cd ./fabric-samples/cbfm
 - Run the script startFabric.sh to start blockchain network, install smart contract, and instantiate smart contract
 - cd ./ca
 - Run the script compileCA.sh to compile CA program. Run the script runCA.sh to the CA program.
 - cd ../cloud
 - Run the script compileCloud.sh to compile Cloud program. Run the script runCloud.sh to the Cloud program.
 - cd ../client
 - Run the script compileClient.sh to compile Client program. Run the script runClient.sh to the Client program.

