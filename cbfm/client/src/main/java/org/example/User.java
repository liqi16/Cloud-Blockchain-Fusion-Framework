package org.example;

import org.hyperledger.fabric.gateway.*;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;


public class User {

    public String username;
    public String pubKey;
    public String secKey;

    public String sigPK;
    public String sigSK;

    protected PublicParam keys;

    private static int sectors = 1;

    public User(String Username) {
        this.username = Username;
        genKey();
    }

    public User(String Username, String Pubkey){
        this.username = Username;
        this.pubKey = Pubkey;
    }

    public static String getSecKeyFile(String username){
        return "./key/"+username+"_seckey";
    }

    public User(String Username, String PubKey, String SecKey) {
        this.username = Username;
        this.pubKey = PubKey;
        this.secKey = SecKey;
    }

    private void genKey(){
        /*
        PairingFactory.getInstance().setUsePBCWhenPossible(false);
        //String parameterPath = this.getClass().getClassLoader().getResource("d_159.properties").getPath();
        String parameterPath = ClientApp.path+ "d_159.properties";
        Pairing pairing = PairingFactory.getPairing(parameterPath);

        Element g1 = pairing.getG1().newRandomElement().getImmutable();
        Element g2 = pairing.getG2().newRandomElement().getImmutable();
        Element[] ps = psGen(pairing,sectors);
        Element[] us = usGen(g1,ps);
        Element x = pairing.getZr().newRandomElement().getImmutable();
        Element v = g2.duplicate().powZn(x);


        JSONObject pubkeyObject = new JSONObject();
        pubkeyObject.put("g1", Utils.elementToBase64(g1));
        pubkeyObject.put("g2", Utils.elementToBase64(g2));
        pubkeyObject.put("v", Utils.elementToBase64(v));
        pubkeyObject.put("us", Utils.elementToBase64(us[0]));

        String pubkeyString = pubkeyObject.toJSONString();

        pubKey = pubkeyString;

        JSONObject seckeyObject = new JSONObject();
        seckeyObject.put("x", Utils.elementToBase64(x));
        seckeyObject.put("ps", Utils.elementToBase64(ps[0]));

        String seckeyString = seckeyObject.toJSONString();

        secKey = seckeyString;*/
        //PairingFactory.getInstance().setUsePBCWhenPossible(false);
        String parameterPath = ClientApp.path+ "a.properties";
        Pairing pairing = PairingFactory.getPairing(parameterPath);

        Element x;
        Element g,u,v,w;

        g = pairing.getG1().newRandomElement().getImmutable();
        x = pairing.getZr().newRandomElement().getImmutable();
        u = pairing.getG2().newRandomElement().getImmutable();
        v = g.duplicate().powZn(x).getImmutable();
        w = u.duplicate().powZn(x).getImmutable();

        JSONObject pubkeyObject = new JSONObject();
        pubkeyObject.put("g", Utils.elementToBase64(g));
        pubkeyObject.put("u", Utils.elementToBase64(u));
        pubkeyObject.put("v", Utils.elementToBase64(v));
        pubkeyObject.put("w", Utils.elementToBase64(w));

        String pubkeyString = pubkeyObject.toJSONString();

        pubKey = pubkeyString;

        JSONObject prikeyObject = new JSONObject();
        prikeyObject.put("x", Utils.elementToBase64(x));

        String seckeyString = prikeyObject.toJSONString();

        secKey = seckeyString;

        keys = new PublicParam(pairing, g, u, v, w);

        try{
            KeyPair pair = ECC.generateKeyPair();

            ECC.exportPK(pair.getPublic(),"./key/"+username+"-sigPK.dat");
            ECC.exportSK(pair.getPrivate(),"./key/"+username+"-sigSK.dat");
            sigPK = ECC.exportPKToString(pair.getPublic());
            sigSK = ECC.exportSKToString(pair.getPrivate());
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    /*public static Element[] usGen(Element g1, Element[] ps) {
        int s = ps.length;
        Element[] us = new Element[s];
        for (int i = 0; i < s; i++) {
            // ui=g1^ai
            us[i] = g1.duplicate().powZn(ps[i]);
        }

        return us;
    }*/

    /*public static Element[] psGen(Pairing pairing, int s) {
        Element[] ps = new Element[s];
        for (int i = 0; i < s; i++) {
            // a1,...as
            ps[i] = pairing.getZr().newRandomElement();
        }
        return ps;
    }*/

    public static String userRegister(Network network, String mspId, String name,String attr) throws Exception{
        Contract contract = network.getContract("cbfm-go");
        byte[] result;

        Map<String,String> register = register(name);
        String username = register.get("username");
        String pubkey = register.get("pubkey");
        String seckey = register.get("seckey");

        String sigPK = register.get("sigpk");
        String sigSK = register.get("sigsk");

        TextOutput.jsonOutput("审计公钥",pubkey);
        TextOutput.jsonOutput("审计私钥",seckey);

        TextOutput.jsonOutput("签名公钥",sigPK);
        TextOutput.jsonOutput("签名私钥",sigSK);

        //String attribute = "[0,2,5]";
        String attribute = attr;

        String period = "[30,60,100,120]";

        result = contract.submitTransaction("userRegister",mspId,username,pubkey,attribute,period,sigPK);

        String chaincodeResult = new String(result);


        String userSecKeyFileName = User.getSecKeyFile(username);
        Utils.writeFile(userSecKeyFileName,seckey);

        //System.out.println("用户信息:");
        //System.out.println(chaincodeResult);
        //System.out.println("注册成功!");
        /*
        if (chaincodeResult.contains("Successfully")){

        }*/
        return chaincodeResult;
    }

    public static Map<String, String> register(String name){
        Map<String, String> result = new HashMap<>();

        //Scanner in = new Scanner(System.in);

        //String username = in.nextLine();

        String username = name;

        User user = new User(username);

        result.put("username",user.username);
        result.put("pubkey",user.pubKey);
        result.put("seckey",user.secKey);
        result.put("sigpk",user.sigPK);
        result.put("sigsk",user.sigSK);

        return result;
    }

    public static String userLogin(Network network, String mspId, String username) throws Exception {

        Contract contract = network.getContract("cbfm-go");
        byte[] result;

        result = contract.evaluateTransaction("userLogin",mspId, username);

        String resultString = new String(result);

        //System.out.println("用户信息:");
        //System.out.println(resultString);

        return resultString;

    }

    public static String queryAllUser(Network network) throws ContractException {

        Contract contract = network.getContract("cbfm-go");
        byte[] result;
        result = contract.evaluateTransaction("queryAllUser");
        //System.out.println(new String(result));

        String allUserJsonArrayString = new String(result);


        /*String[] userColumnNames = TextOutput.userColumnNames();
        Object[][] data = TextOutput.userArrayToData(allUserJsonArrayString);
        TextOutput.tableOutput("用户信息",userColumnNames,data);*/
        TextOutput.jsonOutput("用户信息",allUserJsonArrayString);
        return allUserJsonArrayString;
    }

    public static String queryUser(Network network,String msp, String name) throws ContractException {

        Contract contract = network.getContract("cbfm-go");
        byte[] result;

        result = contract.evaluateTransaction("queryUser",msp, name);

        String resultString = new String(result);

        TextOutput.jsonOutput("用户信息",resultString);
        //System.out.println("用户信息:");
        //System.out.println(resultString);

        return resultString;
    }

    public static String queryID(Network network,String id) throws ContractException {

        Contract contract = network.getContract("cbfm-go");
        byte[] result;

        result = contract.evaluateTransaction("queryPeer",id);

        String resultString = new String(result);

        TextOutput.jsonOutput("用户信息",resultString);
        //System.out.println("用户信息:");
        //System.out.println(resultString);

        return resultString;
    }


}
