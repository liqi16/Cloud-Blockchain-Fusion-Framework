package org.example;

import com.alibaba.fastjson.JSONObject;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

public class SecretParam {
    private Element x;

    SecretParam(String jsonStringSec) {
        PairingFactory.getInstance().setUsePBCWhenPossible(false);
        Pairing pairing = PairingFactory.getPairing(ClientApp.path + "a.properties");

        JSONObject prikeyObject = JSONObject.parseObject(jsonStringSec);
        x = pairing.getZr().newElementFromBytes(Utils.base64StringToElementBytes(prikeyObject.getString("x"))).getImmutable();
    }

    SecretParam(Element x_) {
        x = x_.getImmutable();
    }

    public Element getX() {
        return x;
    }
}
