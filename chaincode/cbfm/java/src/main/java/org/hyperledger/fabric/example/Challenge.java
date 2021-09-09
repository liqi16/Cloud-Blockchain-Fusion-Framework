package org.hyperledger.fabric.example;

import it.unisa.dia.gas.jpbc.Element;

public class Challenge implements Comparable<Challenge>{

    public int num;
    public Element random;

    public Challenge(int num,Element random){
        this.num=num;
        this.random=random;
    }

    @Override
    public int compareTo(Challenge o) {
        // TODO Auto-generated method stub
        return this.num - o.num;
    }

}

