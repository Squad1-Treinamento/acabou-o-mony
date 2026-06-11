package com.acabouomony.payment.domain.model;

import java.util.regex.Pattern;

/**
 * Supported card types with BIN range detection.
 * 
 * Each card type has:
 * - BIN pattern (first 6 digits)
 * - Valid card number lengths
 * - CVV length requirements
 * 
 * Spec: spec-001-core-payment-processing.md - Card Validation Rules
 */
public enum CardType {
    
    VISA("^4[0-9]{0,}$", new int[]{13, 16, 19}, 3),
    MASTERCARD("^(5[1-5]|222[1-9]|22[3-9]|2[3-6]|27[01]|2720)[0-9]{0,}$", new int[]{16}, 3),
    AMEX("^3[47][0-9]{0,}$", new int[]{15}, 4),
    DISCOVER("^(6011|65|64[4-9]|622)[0-9]{0,}$", new int[]{16}, 3),
    DINERS("^(36|38|30[0-5])[0-9]{0,}$", new int[]{14}, 3),
    JCB("^35[0-9]{0,}$", new int[]{16}, 3),
    ELO("^(4011|4312|4389|4514|4576|5041|5066|5067|6277|6362|6363|6504|6505|6516)[0-9]{0,}$", new int[]{16}, 3),
    HIPERCARD("^(606282|3841)[0-9]{0,}$", new int[]{16}, 3);
    
    private final Pattern pattern;
    private final int[] validLengths;
    private final int cvvLength;
    
    CardType(String regex, int[] validLengths, int cvvLength) {
        this.pattern = Pattern.compile(regex);
        this.validLengths = validLengths;
        this.cvvLength = cvvLength;
    }
    
    /**
     * Detects card type from card number.
     * 
     * @param cardNumber The card number (digits only)
     * @return The detected CardType, or null if not recognized
     */
    public static CardType detect(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return null;
        }
        
        // Remove any non-digit characters
        String digits = cardNumber.replaceAll("\\D", "");
        
        for (CardType type : CardType.values()) {
            if (type.pattern.matcher(digits).matches()) {
                return type;
            }
        }
        
        return null;
    }
    
    /**
     * Validates card number length for this card type.
     * 
     * @param cardNumber The card number (digits only)
     * @return true if length is valid for this card type
     */
    public boolean isValidLength(String cardNumber) {
        if (cardNumber == null) {
            return false;
        }
        
        String digits = cardNumber.replaceAll("\\D", "");
        int length = digits.length();
        
        for (int validLength : validLengths) {
            if (length == validLength) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Gets the expected CVV length for this card type.
     * 
     * @return CVV length (3 or 4 digits)
     */
    public int getCvvLength() {
        return cvvLength;
    }
    
    /**
     * Validates CVV length for this card type.
     * 
     * @param cvv The CVV code
     * @return true if CVV length matches expected length
     */
    public boolean isValidCvvLength(String cvv) {
        if (cvv == null) {
            return false;
        }
        
        String digits = cvv.replaceAll("\\D", "");
        return digits.length() == cvvLength;
    }
}
