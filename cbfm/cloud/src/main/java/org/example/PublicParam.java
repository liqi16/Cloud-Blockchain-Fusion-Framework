package org.example;

import com.alibaba.fastjson.JSONObject;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

public class PublicParam {
	private Pairing pairing;
	private Element g;
	private Element u;
	private Element v;
	private Element w;
	
	PublicParam(String jsonStringPub) {
		PairingFactory.getInstance().setUsePBCWhenPossible(false);
		pairing = PairingFactory.getPairing("a.properties");

		JSONObject pubkeyObject = JSONObject.parseObject(jsonStringPub);
		g = pairing.getG2().newElementFromBytes(Utils.base64StringToElementBytes(pubkeyObject.getString("g"))).getImmutable();
		u = pairing.getG1().newElementFromBytes(Utils.base64StringToElementBytes(pubkeyObject.getString("u"))).getImmutable();
		v = pairing.getG2().newElementFromBytes(Utils.base64StringToElementBytes(pubkeyObject.getString("v"))).getImmutable();
		w = pairing.getG1().newElementFromBytes(Utils.base64StringToElementBytes(pubkeyObject.getString("w"))).getImmutable();
	}
	PublicParam(Pairing p_, Element g_, Element u_, Element v_, Element w_) {
		pairing = p_;
		g = g_.getImmutable();
		u = u_.getImmutable();
		v = v_.getImmutable();
		w = w_.getImmutable();
	}

	public Element getG() {
		return g;
	}

	public Element getU() {
		return u;
	}

	public Element getV() {
		return v;
	}

	public Element getW() {
		return w;
	}

	public Pairing getPairing() {
		return pairing;
	}
}
