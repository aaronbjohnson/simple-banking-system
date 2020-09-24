package banking;

import java.util.Random;

public class Account {
    /**
     * Constants used to generate card numbers.
     */
    private static final String IIN = "400000";
    private static final int ACCOUNT_NUM_LENGTH = 9;
    private static final int LUHN_MAX = 9;
    private static final int CHECK_SUM_UPPER_BOUND = 10;

    private final String number;
    private final String pin;
    private double balance = 0;
    private boolean isUnsaved = false;  // Used to control whether Account instance gets updated in database.
    private boolean isInDatabase = false;         // Used to control whether Account instance gets saved in database.

    public Account() {
        this.number = createCardNumber();
        this.pin = createPin();
    }

    public Account(String number, String pin, double balance) {
        this.number = number;
        this.pin = pin;
        this.balance = balance;
    }


    /**
     * Returns a random 4 digit PIN number for the account.
     * @return <code>String</code> 4 digit PIN number for account.
     */
    private String createPin() {
        Random randomNum = new Random();
        String first = String.valueOf((randomNum.nextInt(10)));
        String second = String.valueOf((randomNum.nextInt(10)));
        String third = String.valueOf((randomNum.nextInt(10)));
        String fourth = String.valueOf((randomNum.nextInt(10)));
        return first + second + third + fourth;
    }


    /**
     * Combines IIN, account identifier, and the check sum for a complete valid credit card number.
     * @return the full card number for the account.
     */
    private String createCardNumber() {
        String accountIdentifier = getAccountIdentifier();
        String cardPrefix = IIN + accountIdentifier;
        int[] convertedCardPrefix = convertStringToArray(cardPrefix); // Converts the prefix string to array of ints.
        String checkSum = getCheckSum(convertedCardPrefix);
        return cardPrefix + checkSum;
    }

    /**
     * Takes in the first 15 digits of a card number and generates a valid check sum according to the Luhn Algorithm.
     * @param prefixArray an array of integers representing the first 15 digits of a card number.
     * @return the valid check sum based on the prefixArray.
     */
    private String getCheckSum(int[] prefixArray) {
        int[] updatedArray = new int[prefixArray.length];
        /*
        For every number in prefix array add the number to a new array.
        If the index + 1 is odd, double it before adding to the new array.
        Also, if the final number you're adding to the new array is > 9, subtract 9 before adding to array.
         */
        for (int i = 0; i < prefixArray.length; i++) {
            int currentStep = i + 1; // step in card number; need to reuse i + 1;
            if (currentStep % 2 == 0 && prefixArray[i] > LUHN_MAX) {
                updatedArray[i] = prefixArray[i] - LUHN_MAX;
            } else if (currentStep % 2 == 0) {
                updatedArray[i] = prefixArray[i];
            } else if ((2 * prefixArray[i]) > LUHN_MAX) {
                updatedArray[i] = (2 * prefixArray[i]) - 9;
            } else {
                updatedArray[i] = (2 * prefixArray[i]);
            }
        }
        int sum = sumArray(updatedArray); // Gets the sum of the populated array to pass to convertSumToCheck().

        return convertSumToCheck(sum); // Gets and returns the check sum from the sum of above algorithm.
    }

    /**
     * Takes an array of ints and returns their sum.
     * @param arr the array of integers to sum.
     * @return the sum of the given array.
     */
    private int sumArray(int[] arr) {
        int sum = 0;
        for (int j : arr) {
            sum += j;
        }
        return sum;
    }

    /**
     * Takes a string representing a credit card number prefix and converts it to an array of individual integers.
     * @param prefix string representing card number prefix.
     * @return integer array of credit card number prefix.
     */
    private int[] convertStringToArray(String prefix) {
        char[] separateString = prefix.toCharArray();   // Separates each character in the string.

        int[] prefixArray = new int[separateString.length]; // Makes a new array to add converted integers to.

        for (int i = 0; i < separateString.length; i++) {
            prefixArray[i] = Integer.parseInt(String.valueOf(separateString[i]));
        }

        return prefixArray;
    }

    /**
     * Takes the sum generated within getCheckSum() and creates a check sum according to the Luhn Algorithm.
     * @param sum the value produced inside the getCheckSum() method.
     * @return the value of the check sum.
     */
    private String convertSumToCheck(int sum) {
        int checkSum = 0;

        for (int i = 0; i < CHECK_SUM_UPPER_BOUND; i++) {
            if ((sum + i) % 10 == 0) {
                checkSum =  i;
            }
        }

        return String.valueOf(checkSum);
    }

    /**
     * Generates a random account identifier for a new card (digits 7 through 15).
     * @return the randomly generated account identifier.
     */
    private String getAccountIdentifier() {
        Random randomNum = new Random();
        StringBuilder accountNumTemp = new StringBuilder();
        for (int i = 1; i <= ACCOUNT_NUM_LENGTH; i++) {
            int randomInt = randomNum.nextInt(10);
            String s = String.valueOf(randomInt);
            accountNumTemp.append(s);
        }
        return accountNumTemp.toString();
    }

    /**
     *
     * @return credit card number for account.
     */
    public String getCardNumber() {
        return number;
    }

    /**
     *
     * @return current balance for account.
     */
    public double getBalance() {
        return balance;
    }

    /**
     *
     * @return PIN number for account.
     */
    public String getPin() {
        return pin;
    }

    /**
     *
     * @param balance new balance to set for account.
     */
    public void setBalance(double balance) {
        this.balance = balance;
    }

    public boolean isUnsaved() {
        return isUnsaved;
    }

    public void setUnsaved(boolean unsaved) {
        isUnsaved = unsaved;
    }

    public boolean isInDatabase() {
        return isInDatabase;
    }

    public void setInDatabase(boolean inDatabase) {
        isInDatabase = inDatabase;
    }

    @Override
    public String toString() {
        return "Account{" +
                "cardNumber='" + number + '\'' +
                ", pin='" + pin + '\'' +
                ", balance=" + balance +
                '}';
    }
}
