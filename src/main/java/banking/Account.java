package banking;

import java.util.Random;

public class Account {
    private String cardNumber;
    private String pin;
    private double balance = 0;

    // create constants to set minimum and maximum PIN number
    private static final String IIN = "400000";
    private static final int ACCOUNT_NUM_LENGTH = 9;
    // constant for luhn algorithm max number
    private static final int LUHN_MAX = 9;
    private static final int CHECK_SUM_UPPER_BOUND = 10;

    public Account() {
        this.cardNumber = createCardNumber();
        this.pin = createPin();
    }

    public Account(String cardNumber, String pin, double balance) {
        this.cardNumber = cardNumber;
        this.pin = pin;
        this.balance = balance;
    }


    private String createPin() {
        Random randomNum = new Random();
        String first = String.valueOf((randomNum.nextInt(10)));
        String second = String.valueOf((randomNum.nextInt(10)));
        String third = String.valueOf((randomNum.nextInt(10)));
        String fourth = String.valueOf((randomNum.nextInt(10)));

        return first + second + third + fourth;
    }

    private String createCardNumber() {
        String accountIdentifier = getAccountIdentifier();
        // need to pass IIN + accountIdendifier to method that generates the checksum
        String cardPrefix = IIN + accountIdentifier; // go ahead and concat these together for convenience
        String checkSum = getCheckSum(luhnAlgorithm(convertStringToArray(cardPrefix)));

        return cardPrefix + checkSum;
    }

    private int luhnAlgorithm(int[] prefixArray) {
        int[] updatedArray = new int[prefixArray.length];
        // for every number in prefix array add the number to a new array;
        // if the index + 1 is odd, double it before adding to the new array. ALSO
        // if the final number you're adding to the new array is > 9, subtract 9 before
        // adding
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
        return sumArray(updatedArray);
    }

    private int sumArray(int[] arr) {
        int sum = 0;
        for (int j : arr) {
            sum += j;
        }
        return sum;
    }

    private int[] convertStringToArray(String prefix) {
        char[] separateString = prefix.toCharArray();

        int[] prefixArray = new int[separateString.length];

        for (int i = 0; i < separateString.length; i++) {
            prefixArray[i] = Integer.parseInt(String.valueOf(separateString[i]));
        }

        return prefixArray;
    }

    private String getCheckSum(int luhnSum) {
        int checkSum = 0;

        for (int i = 0; i < CHECK_SUM_UPPER_BOUND; i++) {
            if ((luhnSum + i) % 10 == 0) {
                checkSum =  i;
            }
        }

        return String.valueOf(checkSum);
    }

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

    public String getCardNumber() {
        return cardNumber;
    }

    public double getBalance() {
        return balance;
    }

    public String getPin() {
        return pin;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "Account{" +
                "cardNumber='" + cardNumber + '\'' +
                ", pin='" + pin + '\'' +
                ", balance=" + balance +
                '}';
    }
}
