package banking;

import org.sqlite.SQLiteDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    /*
    Global constants
     */
    private final static String CREATE_ACCOUNT = "1";
    private final static String LOG_INTO_ACCOUNT = "2";
    private final static String CHECK_BALANCE = "1";
    private final static String ADD_INCOME = "2";
    private final static String DO_TRANSFER = "3";
    private final static String CLOSE_ACCOUNT = "4";
    private final static String LOG_OUT = "5";
    private final static String EXIT = "0";

    public static void main(String[] args) {

        String dataBaseName = args[1]; // The second argument (args[1]) passed is the database file name.

        String url = "jdbc:sqlite:.\\" + dataBaseName; // Stores the path to the database file.

        /*
        Creates an SQLite data source and sets its location.
         */
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        /*
        Creates a table of card accounts if one doesn't already exist.
         */
        createTable(dataSource);

        /*
        Creates an Array List that stores existing accounts in the database as well as any new cards created during
        a session.
         */
        ArrayList<Account> sessionAccounts = new ArrayList<>();

        /*
        Populates sessionAccounts with Accounts store in the database.
         */
        getExistingAccounts(sessionAccounts, dataSource);

        //printExistingRecords(sessionAccounts); // used for testing


        boolean continueMainMenu = true; // Controls the outer menu loop that displays the main menu.

        /*
        While continueMainMenu is true, keeps displaying main menu.
         */
        do {
            String mainMenuChoice = getMainMenuChoice(); // Stores the main menu choice received from user.

            switch (mainMenuChoice) {
                case CREATE_ACCOUNT:
                    sessionAccounts.add(createAccount()); // Creates a new account and adds it to the account Array.
                    break;
                case LOG_INTO_ACCOUNT:
                    Account userAccount = loginToAccount(sessionAccounts); // Stores the logged-into account.

                    /*
                    If the login was successful and returned an account, display the account menu.
                     */
                    if (userAccount != null) {
                        System.out.println("You have successfully logged in!\n");
                        boolean loggedIn = true;

                        while (loggedIn) {
                            String acctMenuChoice =  getAccountMenuChoice();

                            switch (acctMenuChoice) {
                                case CHECK_BALANCE:
                                    System.out.println("\nBalance: " + userAccount.getBalance());
                                    System.out.println();
                                    break;
                                case ADD_INCOME:
                                    System.out.println("Enter income:");
                                    double incomeAmount = getAmount(); // Gets the amount to add from the user.
                                    addIncome(userAccount, incomeAmount);// Adds the income to the account.
                                    System.out.println("Income was added!");
                                    break;
                                case DO_TRANSFER:
                                    System.out.println("Transfer");

                                    /*
                                    Calls validDateCard() to get the card number and ensure it passes Luhn algorithm.
                                     */
                                    //todo: should do if statement here, if invalid, break
                                    String cardNumber = validateCard();

                                    if (cardNumber != null) {
                                        /*
                                        Gets the account to send money to.
                                        */
                                        Account receivingAccount = getDestinationAccount(cardNumber, sessionAccounts);

                                        /*
                                        If an account to send money to was successfully retrieved, transfers the money.
                                        Otherwise display error message.
                                        */
                                        if (receivingAccount != null) {
                                            // todo: may need to put try statement here as we can't use "break" under the sout "not enough money"

                                            transferFunds(userAccount, receivingAccount); // todo: need to test this @test
                                        } else {
                                            System.out.println("Such a card does not exist");
                                        }
                                    }
                                    break;
                                case CLOSE_ACCOUNT:
                                    closeAccount(userAccount, sessionAccounts, dataSource);
                                    loggedIn = false;
                                    break;
                                case LOG_OUT:
                                    System.out.println("\nYou have successfully logged out!\n");
                                    loggedIn = false;
                                    break;
                                case EXIT:
                                    System.out.println("\nBye!\n");
                                    loggedIn = false;
                                    continueMainMenu = false;
                                    break;
                            }
                        }
                    }
                    break; // If no account was logged into, returns to the main menu.
                case EXIT:
                    System.out.println("\nBye!\n");
                    continueMainMenu = false; // Ends the program.
                    break;
            }
        } while (continueMainMenu);

        saveChanges(sessionAccounts, dataSource); // Updates database to reflect new Accounts and changes to old accounts.

        //printExistingRecords(sessionAccounts); // only used for testing
    }

    /**
     * Given an array of Accounts and a connection to a database, saves/updates Accounts in database.
     * @param accounts array of Accounts to check for changes to save.
     * @param data the connection to the database.
     */
    private static void saveChanges(ArrayList<Account> accounts, SQLiteDataSource data) {
        for (Account acc : accounts) {
            if (acc.isInDatabase() && acc.isUnsaved()) {
                updateDatabase(acc, data);
                acc.setUnsaved(false); // Flag the account as having changes saved.
            } else if (!acc.isInDatabase()) {
                addToDatabase(acc, data);
            }
        }
    }

    /**
     * Given an Account and a connection to a database, adds the Account to the database.
     * @param account the Account to add to the database.
     * @param data the connection to the database.
     */
    private static void addToDatabase(Account account, SQLiteDataSource data) {
        try (Connection con = data.getConnection()) {
            try (Statement statement = con.createStatement()) {
                statement.executeUpdate("INSERT INTO card (number, pin, balance) VALUES " +
                        "('" + account.getNumber() + "', '" + account.getPin() + "', " + account.getBalance() + ")");
            } catch (SQLException e) {
                e.printStackTrace();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Transfers funds from a source Account to a target Account.
     * @param sourceAccount the Account to subtract funds from.
     * @param targetAccount the Account to add funds to.
     */
    private static void transferFunds(Account sourceAccount, Account targetAccount) {
        System.out.println("Enter how much you want to transfer");
        double transferAmount = getAmount();

        if (sourceAccount == targetAccount) {
            System.out.println("You can't transfer money to the same account!");
        } else {
            /*
            If the source Account has enough money, do transfer.
             */
            if ((sourceAccount.getBalance() - transferAmount) > 0) {
                subtractIncome(sourceAccount, transferAmount);
                addIncome(targetAccount, transferAmount);
                System.out.println("Success!");
            } else {
                System.out.println("Not enough money!");
            }
        }
    }

    /**
     * Creates a table of card accounts if one does not already exist.
     * @param data the connection to the database where the table is created.
     */
    private static void createTable(SQLiteDataSource data) {
        try (Connection con = data.getConnection()) {
            // Statement creation
            try (Statement statement = con.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS card(" +
                        "id INTEGER PRIMARY KEY," +
                        "number TEXT NOT NULL," +
                        "pin TEXT," +
                        "balance INTEGER DEFAULT 0)");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes an account from the database and removes it from the list of accounts.
     * @param account the account to be closed.
     * @param accounts the list of accounts the account will be removed from.
     * @param data the connection to the database.
     */
    private static void closeAccount(Account account, ArrayList<Account> accounts, SQLiteDataSource data) {
        boolean accountRemoved = accounts.removeIf(acc -> account.getNumber().equals(acc.getNumber()));

        if (account.isInDatabase()) {
            try (Connection con = data.getConnection()) {
                try (Statement statement = con.createStatement()) {
                    statement.executeUpdate("DELETE FROM card WHERE number = " + account.getNumber() + ";");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (accountRemoved) {
            System.out.println("The account has been closed.");

        } else {
            System.out.println("Unable to close the account.");
        }
    }


    /**
     * Searches for and retrieves an account in a given list of accounts by credit card number.
     * @param cardNumber the credit card number to search for.
     * @param accounts the list of accounts to search.
     * @return the matching account if found, null if not found.
     */
    private static Account getDestinationAccount(String cardNumber, ArrayList<Account> accounts) {
        Account foundAccount = null;

        for (Account acc : accounts) {
            if (acc.getNumber().equals(cardNumber)) {
                foundAccount = acc;
                break;
            }
        }
        return foundAccount;
    }

    private static String validateCard() {
        Scanner input = new Scanner(System.in);

        System.out.println("Enter card number:");
        String cardNum = input.next();

        char[] separateString = cardNum.toCharArray(); // separate into array of chars

        /*
        Stores the entered check sum so it can be compared to the correct check sum.
         */
        String checkToVerify = String.valueOf(separateString[separateString.length - 1]); // Strips the last char.

        int[] cardAsIntArray = new int[separateString.length - 1]; // Makes empty int array to hold converted chars from above.

        for (int i = 0; i < separateString.length - 1; i++) {
            cardAsIntArray[i] = Integer.parseInt(String.valueOf(separateString[i]));
        }

        // create a temporary Account
        Account tempAcc = new Account(); // make new account (tempAcc)

        // store temp account's .getCheckSum()
        String correctCheckSum = tempAcc.getCheckSum(cardAsIntArray);
        String finalCardNumber = null;

        if (correctCheckSum.equals(checkToVerify)) {
            finalCardNumber = cardNum;
        } else {
            System.out.println("This is an invalid card number. Please try again.");
        }

        return finalCardNumber;
    }

    /*
    Only used for testing
     */
    /*
     private static void printExistingRecords(ArrayList<Account> existing) {
        for (Account acc : existing) {
            System.out.println( acc.toString());
        }
    }
     */

    /**
     * Adds a given amount to the given Account's balance.
     * @param acc the Account to add funds to.
     * @param amount the amount to add to the Account.
     */
    private static void addIncome(Account acc, double amount) {
        // connect to database
        double newBalance = acc.getBalance() + amount;
        // update the ArrayList record
        if (acc.isInDatabase()) {
            acc.setUnsaved(true); // Flag the account as having unsaved changes.
        }
        acc.setBalance(newBalance);
    }

    /**
     * Subtracts the given amount from an Account's balance.
     * @param acc the account to subtract money from
     * @param amount the amount to subtract from the Account's balance.
     */
    private static void subtractIncome(Account acc, double amount) {
        // connect to database
        double newBalance = acc.getBalance() - amount;

        if (acc.isInDatabase()) {
            acc.setUnsaved(true); // Flag the account as having unsaved changes.
        }

        // update the ArrayList record
        acc.setBalance(newBalance);
    }

    /**
     * Gets an amount from the user.
     * @return the amount entered by the user.
     */
    private static double getAmount() {
        Scanner input = new Scanner(System.in);
        double amount = 0;
        try {
            amount = input.nextDouble();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return amount;
    }

    /**
     * Retrieves card Accounts stored in the given database and adds them to the given ArrayList.
     * @param arrayList the ArrayList where existing records are added.
     * @param data the connection to the database to retrieve records from.
     */
    private static void getExistingAccounts(ArrayList<Account> arrayList, SQLiteDataSource data) {
        try (Connection con = data.getConnection()) {
            try (Statement statement = con.createStatement()) {
                try (ResultSet tableRecords = statement.executeQuery("SELECT * FROM " + "card")) {
                    while (tableRecords.next()) {
                        int id = tableRecords.getInt("id");
                        String number = tableRecords.getString("number");
                        String pin = tableRecords.getString("pin");
                        int balance = tableRecords.getInt("balance");

                        Account tempAcc = new Account(number, pin, balance); // make new account (tempAcc)
                        tempAcc.setInDatabase(true);    // Sets the Account's isInDatabase parameter to TRUE
                        arrayList.add(tempAcc); // add it to the sessions sessionAccounts.add(tempAcc)
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates database with changes to a particular Account.
     * @param account the Account to update.
     * @param data the connection to the database.
     */
    private static void updateDatabase(Account account, SQLiteDataSource data) {
        try (Connection con = data.getConnection()) {
            try (Statement statement = con.createStatement()) {
                statement.executeUpdate("UPDATE " + "card " +
                        "SET balance = " + account.getBalance() + " " +
                        "WHERE number = " + account.getNumber() + ";");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays a menu for operations to perform on an Account.
     * @return the user's menu choice for operation on an account.
     */
    public static String getAccountMenuChoice() {
        Scanner input = new Scanner(System.in);

        System.out.println();
        System.out.println("1. Balance");
        System.out.println("2. Add income");
        System.out.println("3. Do transfer");
        System.out.println("4. Close account");
        System.out.println("5. Log out");
        System.out.println("0. Exit");

        return input.next();
    }

    /**
     * Allows a user to log in to an account if they provide the correct card and PIN number.
     * @param arr the ArrayList of card Accounts to search.
     * @return the matching account or null if no match found.
     */
    public static Account loginToAccount(ArrayList<Account> arr) {
        Scanner input = new Scanner(System.in);

        System.out.println("Enter your card number:");
        String cardNum = input.next();
        System.out.println("Enter your PIN:");
        String pinNum  = input.next();
        Account matchingAccount = null;

        /*
         Checks if the given card and PIN match up to an Account in the list of accounts.
         */
        for (Account account : arr) {
            if (account.getNumber().equals(cardNum) && account.getPin().equals(pinNum)) {
                matchingAccount = account;
            }
        }

        if (matchingAccount == null) {
            System.out.println("Wrong card number or PIN!");
        }
        return matchingAccount;
    }

    /**
     * Creates a new instance of an Account.
     * @return the new Account.
     */
    public static Account createAccount() {
        // this function will create a new anonymous Account and add it to the list of account for this session
        Account tempAcc = new Account();
        tempAcc.setInDatabase(false); // Flag the new account as not having a place in database yet.
        System.out.println("Your card has been created");
        System.out.println("Your card number:");
        System.out.println(tempAcc.getNumber());
        System.out.println("Your card PIN:");
        System.out.println(tempAcc.getPin());
        return tempAcc;
    }

    /**
     * Displays the main menu and stores the user's menu choice.
     * @return the user's menu selection.
     */
    public static String getMainMenuChoice() {
        Scanner input = new Scanner(System.in);
        String answer;
        do {
            System.out.println("1. Create an account");
            System.out.println("2. Log into account");
            System.out.println("0. Exit");
            answer = input.next();
        } while (!answer.equals(CREATE_ACCOUNT) && !answer.equals(LOG_INTO_ACCOUNT) && !answer.equals(EXIT));

        return answer;
    }
}
