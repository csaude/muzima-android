package mz.org.csaude.muzimamobile.utils;

/**
 * @author Jose Julai Ritsure
 */
public class PhoneNumberUtils {
    public static boolean validatePhoneNumber(String phoneNumber) {
        if(phoneNumber.startsWith("8") && phoneNumber.length()==9) {
            return true;
        }
        return false;
    }

}
