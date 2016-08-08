package io.gomint.proxy;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.StreamBlockCipher;
import org.bouncycastle.crypto.modes.CFBBlockCipher;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import java.security.Key;

/**
 * @author geNAZt
 * @version 1.0
 */
public class CryptoContext {

    private final byte[] key;
    private final IvParameterSpec iv;
    private final CFBBlockCipher cipher;

    public CryptoContext( byte[] key, IvParameterSpec iv, CFBBlockCipher cipher ) {
        this.key = key;
        this.iv = iv;
        this.cipher = cipher;
    }

    public byte[] getKey() {
        return key;
    }

    public IvParameterSpec getIv() {
        return iv;
    }

    public CFBBlockCipher getCipher() {
        return cipher;
    }

}
