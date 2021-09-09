package org.example;

import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallet.Identity;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.security.CryptoSuiteFactory;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Properties;
import java.util.Set;

public class RegisterUser {

    static {
        System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "true");
    }

    public static Identity adminLogin() throws Exception{
        // Create a CA client for interacting with the CA.
        Properties props = new Properties();
        props.put("pemFile",
                "../../first-network/crypto-config/peerOrganizations/org1.example.com/ca/ca.org1.example.com-cert.pem");
        props.put("allowAllHostNames", "true");
        HFCAClient caClient = HFCAClient.createNewInstance("https://localhost:7054", props);
        CryptoSuite cryptoSuite = CryptoSuiteFactory.getDefault().getCryptoSuite();
        caClient.setCryptoSuite(cryptoSuite);

        // Create a wallet for managing identities
        Wallet wallet = Wallet.createFileSystemWallet(Paths.get("wallet"));

        boolean userExists = wallet.exists("admin");
        if (!userExists) {
            throw new Exception("\"admin\" needs to be enrolled and added to the wallet first");
        }

        Identity adminIdentity = wallet.get("admin");

        return adminIdentity;
    }

    public static void registerUser(Identity adminIdentity, String username) throws Exception {
        // Create a CA client for interacting with the CA.

        Properties props = new Properties();
        props.put("pemFile",
                "../../first-network/crypto-config/peerOrganizations/org1.example.com/ca/ca.org1.example.com-cert.pem");
        props.put("allowAllHostNames", "true");
        HFCAClient caClient = HFCAClient.createNewInstance("https://localhost:7054", props);
        CryptoSuite cryptoSuite = CryptoSuiteFactory.getDefault().getCryptoSuite();
        caClient.setCryptoSuite(cryptoSuite);

        // Create a wallet for managing identities
        Wallet wallet = Wallet.createFileSystemWallet(Paths.get("wallet"));

        // Check to see if we've already enrolled the user.
        boolean userExists = wallet.exists(username);
        if (userExists) {
            throw new Exception("An identity for the user \""+username+"\" already exists in the wallet");
        }

        User admin = new User() {

            @Override
            public String getName() {
                return "admin";
            }

            @Override
            public Set<String> getRoles() {
                return null;
            }

            @Override
            public String getAccount() {
                return null;
            }

            @Override
            public String getAffiliation() {
                return "org1.department1";
            }

            @Override
            public Enrollment getEnrollment() {
                return new Enrollment() {

                    @Override
                    public PrivateKey getKey() {
                        return adminIdentity.getPrivateKey();
                    }

                    @Override
                    public String getCert() {
                        return adminIdentity.getCertificate();
                    }
                };
            }

            @Override
            public String getMspId() {
                return adminIdentity.getMspId();
            }

        };

        // Register the user, enroll the user, and import the new identity into the wallet.
        RegistrationRequest registrationRequest = new RegistrationRequest(username);
        registrationRequest.setAffiliation(admin.getAffiliation());
        registrationRequest.setEnrollmentID(username);
        String enrollmentSecret = caClient.register(registrationRequest, admin);
        Enrollment enrollment = caClient.enroll(username, enrollmentSecret);
        Identity user = Identity.createIdentity(admin.getMspId(), enrollment.getCert(), enrollment.getKey());
        wallet.put(username, user);
        System.out.println("Successfully enrolled user \""+username+"\" and imported it into the wallet");
    }

    public static void login(String caPort) throws Exception {
        Path walletPath = Paths.get("wallet");
        Wallet wallet = Wallet.createFileSystemWallet(walletPath);

        // load a CCP
        //Path networkConfigPath = Paths.get("..", "connection-org1.yaml");
        Path networkConfigPath = Paths.get("..", "..","first-network","connection-org1.yaml");
        System.out.println(networkConfigPath);

        Gateway.Builder builder = Gateway.createBuilder();
        builder.identity(wallet, "admin").networkConfig(networkConfigPath).discovery(true);

        // create a gateway connection
        Gateway gateway = builder.connect();
        Network network = gateway.getNetwork("mychannel");

        Main.adminIdentity = RegisterUser.adminLogin();

        Server caServer = new Server(network,Integer.valueOf(caPort));
        caServer.load();
    }

}
