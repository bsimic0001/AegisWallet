package com.aegiswallet.utils;

/**
 * {@hide}
 */
public class EncodeException extends Exception {
    public EncodeException() {
        super();
    }

    public EncodeException(String s) {
        super(s);
    }

    public EncodeException(char c) {
        super("Unencodable char: --------> '" + c + "'");
    }
}