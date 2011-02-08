package iamrescue.util;

import org.apache.commons.lang.Validate;

public class ModifiableByte {

    byte b = 0;

    public ModifiableByte() {

    }

    public ModifiableByte(byte b) {
        this.b = b;
    }

    public byte getByte() {
        return b;
    }

    public void setBit(int i) {
        Validate.isTrue(i >= 0);
        Validate.isTrue(i <= 7);

        b = (byte) (b | (1 << i));
    }

    public boolean isSet(int i) {
        Validate.isTrue(i >= 0);
        Validate.isTrue(i <= 7);

        return (b & (1 << i)) != 0 ? true : false;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(b + " ");

        for (int i = 0; i < 7; i++) {
            if (isSet(i)) {
                s.append(" 1");
            }
            else {
                s.append(" 0");
            }
        }

        return s.toString();
    }
}
