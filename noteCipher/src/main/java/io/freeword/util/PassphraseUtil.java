package io.freeword.util;

public class PassphraseUtil {

	public static final int MINIMUM_PASSPHRASE_LENGTH = 8;

	public static boolean validatePassphrase(char[] passphrase) {
        return passphrase.length >= MINIMUM_PASSPHRASE_LENGTH;
    }
}
