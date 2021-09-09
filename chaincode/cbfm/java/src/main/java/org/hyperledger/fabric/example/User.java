package org.hyperledger.fabric.example;

import java.util.Objects;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

@DataType()
public final class User {

    @Property()
    public final String username;

    @Property()
    public final String pubkey;

    public String getUsername() {
        return username;
    }

    public String getPubkey() {
        return pubkey;
    }

    public String getStubKey(){
        return "user_"+username;
    }

    public static String getStubKey(String username){
        return "user_"+username;
    }

    public User(@JsonProperty("username") final String u, @JsonProperty("pubkey") final String k) {
        this.username = u;
        this.pubkey = k;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        User other = (User) obj;

        return Objects.deepEquals(new String[] {getUsername(), getPubkey()},
                new String[] {other.getUsername(), other.getPubkey()});
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUsername(), getPubkey());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [username=" + username + ", pubkey="
                + pubkey + "]";
    }
}

