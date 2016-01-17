package io.freeword.util;

public class PassphraseUtil {

	public static final int MINIMUM_PASSPHRASE_LENGTH = 8;

	public static final boolean validatePassphrase(char[] passphrase) {

        if (passphrase.length < PassphraseUtil.MINIMUM_PASSPHRASE_LENGTH) {
            return false;
        }

        return true;
    }
}