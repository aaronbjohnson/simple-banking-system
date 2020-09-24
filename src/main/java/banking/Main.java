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


        boolean continueMainMenu = true; // Controls the outer menu loop that displays the main menu.

        /*
        While continueMainMenu is true, keeps displaying main menu.
         */
        do {
            String mainMenuChoice = getMainMenuChoice(); // Stores the main menu choice received from user.

            switch (mainMenuChoice) {
                case CREATE_ACCOUNT:
                    sessionAccounts.add(createAccount()); // Create a new account and add it to the account Array // todo:return here
                    break;
                case LOG_INTO_ACCOUNT:
                    // call the loginToAccount method
                    Account userAccount = loginToAccount(existingAccounts,sessionAccounts);

                    if (userAccount != null) {
                        // is the user account an existing account in the database?
                        // this will be used to control whether we have to update database in addition to ArrayList(s)
                        boolean isExistingAccount = isInDatabase(userAccount, existingAccounts);
                        System.out.println("You have successfully logged in!\n");
                        boolean loggedIn = true;

                        label:
                        while (loggedIn) {
                            String acctMenuChoice =  getAccountMenuChoice();

                            switch (acctMenuChoice) {
                                case "1":
                                    System.out.println("\nBalance: " + userAccount.getBalance());
                                    System.out.println();
                                    break;
                                case "2":
                                    System.out.println("Enter income:");

                                    // get the amount to add from user
                                    double incomeAmount = getAmount();

                                    // add the income to the account
                                    addIncome(userAccount, incomeAmount, isExistingAccount, dataSource);
                                    System.out.println("Income was added!");
                                    break;
                                case "3":
                                    System.out.println("Transfer");

                                    // Call validDateCard to get card number and ensure it passes Luhn algorithm
                                    // this method should return the valid card because we actually get the card number here too
                                    String cardNumber = validateCard();

                                    Account receivingAccount = getDestinationAccount(cardNumber, existingAccounts, sessionAccounts);

                                    if (receivingAccount != null) { // if cardNumber isn't null: continue
                                        // continue with transfer...

                                        // is receivingAccount in database
                                        boolean receivingAccountInDatabase = isInDatabase(receivingAccount, existingAccounts);

                                        System.out.println("Enter how much you want to transfer");
                                        double transferAmount = getAmount();
                                        //processTransfer(userAccount, receivingAccount, transferAmount);
                                        if (userAccount == receivingAccount) {
                                            System.out.println("You can't transfer money to the same account!");
                                        } else {
                                            if ((userAccount.getBalance() - transferAmount) > 0) {
                                                subtractIncome(userAccount, transferAmount, isExistingAccount, dataSource);
                                                addIncome(receivingAccount, transferAmount, receivingAccountInDatabase, dataSource);
                                                System.out.println("Success!");
                                            } else {
                                                System.out.println("Not enough money!");
                                                break;
                                            }
                                        }
                                    } else {
                                        System.out.println("Such a card does not exist");
                                    }
                                    break;
                                case "4":
                                    closeAccount(userAccount, isExistingAccount, existingAccounts, sessionAccounts, dataSource);
                                    loggedIn = false;
                                case "5":
                                    System.out.println("\nYou have successfully logged out!\n");
                                    loggedIn = false;
                                    break;
                                case "0":
                                    System.out.println("\nBye!\n");
                                    continueMainMenu = false;
                                    break label;
                            }
                        }
                    }
                    break;
                case EXIT:
                    // end the program
                    System.out.println("\nBye!\n");
                    continueMainMenu = false;
                    break;
            }
        } while (continueMainMenu);

        // Add accounts created during a session to the database to save
        updateDatabase(sessionAccounts,url);
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

    private static void closeAccount(Account account, boolean isInDatabase, ArrayList<Account> oldAcc, ArrayList<Account> newAcc, SQLiteDataSource data) {

        if (isInDatabase) {
            try (Connection con = data.getConnection()) {
                try (Statement statement = con.createStatement()) {
                    statement.executeUpdate("DELETE FROM card WHERE number = " + account.getCardNumber() + ";");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            oldAcc.removeIf(acc -> account.getCardNumber().equals(acc.getCardNumber()));
        } else {
            newAcc.removeIf(acc -> account.getCardNumber().equals(acc.getCardNumber()));
        }
        System.out.println("The account has been closed!");
    }


    private static Account getDestinationAccount(String cardNumber, ArrayList<Account> oldAccounts, ArrayList<Account> newAccounts) {
        Account foundAccount = null;

        for (Account acc : oldAccounts) {
            if (acc.getCardNumber().equals(cardNumber)) { // todo: this might cause problems...its just a copy/paste
                foundAccount = acc;
                break;
            }
        }

        for (Account acc : newAccounts) {
            if (acc.getCardNumber().equals(cardNumber)) {
                foundAccount = acc;
                break;
            }
        }
        return foundAccount;
    }

    // todo: may need to delete this as we already check for valid cards
    private static String validateCard() {
        int luhnMax = 9;
        int checkSumUpperBound = 10;
        Scanner input = new Scanner(System.in);

        System.out.println("Enter card number:");
        String cardNum = input.next();

        // does this pass the luhn algorithm?
        char[] separateString = cardNum.toCharArray();

        int[] cardAsIntArray = new int[separateString.length];

        for (int i = 0; i < separateString.length; i++) {
            cardAsIntArray[i] = Integer.parseInt(String.valueOf(separateString[i]));
        }

        int enteredCheck = cardAsIntArray[cardAsIntArray.length - 1];

        /// start of copying old luhn alrogithm
        int[] updatedArray = new int[cardAsIntArray.length - 1];

        for (int i = 0; i < cardAsIntArray.length - 1; i++) {
            int currentStep = i + 1; // step in card number; need to reuse i + 1;
            if (currentStep % 2 == 0 && cardAsIntArray[i] > luhnMax) {
                updatedArray[i] = cardAsIntArray[i] - luhnMax;
            } else if (currentStep % 2 == 0) {
                updatedArray[i] = cardAsIntArray[i];
            } else if ((2 * cardAsIntArray[i]) > luhnMax) {
                updatedArray[i] = (2 * cardAsIntArray[i]) - 9;
            } else {
                updatedArray[i] = (2 * cardAsIntArray[i]);
            }
        }

        int sum = 0;
        for (int j : updatedArray) {
            sum += j;
        }

        int checkSum = 0;

        for (int i = 0; i < checkSumUpperBound; i++) {
            if ((sum + i) % 10 == 0) {
                checkSum =  i;
            }
        }

        String checkString = String.valueOf(checkSum);
        String enteredCheckString = String.valueOf(enteredCheck);

        String finalCardNum = null;

        if (checkString.equals(enteredCheckString)) {
            finalCardNum = cardNum;
        } else {
            System.out.println("Probably you made a mistake in the card number. Please try again!");
        }
        /// end of copying old luhn algorithm
        return finalCardNum;
    }

    // TODO: can delete this as it's only used for testing
    private static void printExistingRecords(ArrayList<Account> existing) {
        for (Account acc : existing) {
            System.out.println( acc.toString());
        }
    }

    private static void addIncome(Account acc, double amount, boolean isOldAccount, SQLiteDataSource data) {
        // connect to database
        double newBalance = acc.getBalance() + amount;

        if (isOldAccount) {
            // if the account is an existing account, need to update in database AND in ArrayList of existing accounts

            // update the database record
            try (Connection con = data.getConnection()) {
                try (Statement statement = con.createStatement()) {
                    statement.executeUpdate("UPDATE " + "card " +
                            "SET balance = " + newBalance + " " +
                            "WHERE number = " + acc.getCardNumber() + ";");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        // update the ArrayList record
        acc.setBalance(newBalance);
    }

    private static void subtractIncome(Account acc, double amount, boolean isOldAccount, SQLiteDataSource data) {
        // connect to database
        double newBalance = acc.getBalance() - amount;

        if (isOldAccount) {
            // if the account is an existing account, need to update in database AND in ArrayList of existing accounts

            // update the database record
            try (Connection con = data.getConnection()) {
                try (Statement statement = con.createStatement()) {
                    statement.executeUpdate("UPDATE " + "card " +
                            "SET balance = " + newBalance + " " +
                            "WHERE number = " + acc.getCardNumber() + ";");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        // update the ArrayList record
        acc.setBalance(newBalance);
    }

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

    private static boolean isInDatabase(Account userAccount, ArrayList<Account> oldAccounts) {
        //all I have to do here is check if the account is in the ArrayList of existing accounts because I've already
        // populated it..so might as well just look there.
        boolean isExistingAccount = false;

        for (Account acc : oldAccounts) {
            if (acc.getCardNumber().equals(userAccount.getCardNumber())) {
                isExistingAccount = true;
                break;
            }
        }
        return isExistingAccount;
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
                        tempAcc.in// Sets the Account's inDatabase parameter to TRUE
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

    private static void updateDatabase(ArrayList<Account> arr, String dbLoc) {
        // takes array of accounts and a database location

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(dbLoc);

        try (Connection con = dataSource.getConnection()) {
            // Statement creation
            // for every account in session account, add it to the database
            for (Account acc : arr) {
                try (Statement statement = con.createStatement()) {
                    statement.executeUpdate("INSERT INTO card (number, pin, balance) VALUES " +
                            "('" + acc.getCardNumber() + "', '" + acc.getPin() + "', " + acc.getBalance() + ")");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

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

    public static Account loginToAccount(ArrayList<Account> oldAccounts, ArrayList<Account> newAccounts) {
        // this function will use scanner to get user to enter card num and pin
        // then will call retrieve account if it matches

        Scanner input = new Scanner(System.in);

        System.out.println("Enter your card number:");
        String cardNum = input.next();
        System.out.println("Enter your PIN:");
        String pinNum  = input.next();

        Account matchingAccount = null;

        // if cardNum in accountList see if PIN is equal that Account's PIN number
        for (Account acc : oldAccounts) {
            if (acc.getCardNumber().equals(cardNum) && acc.getPin().equals(pinNum)) {
                matchingAccount = acc;
            }
        }

        for (Account acc : newAccounts) {
            if (acc.getCardNumber().equals(cardNum) && acc.getPin().equals(pinNum)) {
                matchingAccount = acc;
            }
        }

        if (matchingAccount == null) {
            System.out.println("Wrong card number or PIN!");
        }
        return matchingAccount;
    }


    public static Account createAccount() {
        // this function will create a new anonymous Account and add it to the list of account for this session
        Account tempAcc = new Account();
        System.out.println("Your card has been created");
        System.out.println("Your card number:");
        System.out.println(tempAcc.getCardNumber());
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
