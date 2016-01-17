package io.darknote.core;

public class NConstants {

	public static final String SHARED_PREFS_NOTELINES = "use_lines_in_notes";

	public static final int MAX_STREAM_SIZE = 1000000;
	public static final int MIN_PASS_LENGTH = 8;
	 
	/**
	 * Checks if the password is valid based on its length
     *
	 * @param pass
     *
	 * @return True if the password is a valid one, false otherwise
	 */
	public static final boolean validatePassword(char[] pass) {
        if (pass.length < NConstants.MIN_PASS_LENGTH) {
            return false;
        }
        return true;
    }
}