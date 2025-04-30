package com.example.BalisongFlipping.implementation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.BalisongFlipping.modals.accounts.Account;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.dtos.DisplayNameChangeDto;
import com.example.BalisongFlipping.dtos.UserDto;
import com.example.BalisongFlipping.repositories.AccountRepository;
import com.example.BalisongFlipping.services.AccountService;

@Service
public class AccountServiceImplementation implements AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public String generateIdentifierCode(String displayName) {
        StringBuilder identifier = new StringBuilder();

        do {
            if (!identifier.isEmpty())
                identifier.delete(0, identifier.length());

            Random rand = new Random();
            for (int i = 0; i < 4; i++) {
                // generate random number and append to string
                int num = rand.nextInt(10);
                identifier.append(String.valueOf(num));
            }
        } while (!validateGeneratedIdentifierCode(String.valueOf(identifier), displayName));

        return identifier.toString();
    }

    /**
     * 
     * @param identifierCode
     * @param displayName
     * @return
     * 
     * Method checks for already existing identifier code with matching display name
     */
    private boolean validateGeneratedIdentifierCode(String identifierCode, String displayName) {
        // get list of accounts already created with passed display name
        List<User> foundAccounts = accountRepository.findAllByDisplayName(displayName);

        // return true for empty list with passed display name
        if (foundAccounts.isEmpty())
            return true;

        // loop through acounts to check identifier codes
        for (User account : foundAccounts) {
            if (account.getIdentifierCode().equals(identifierCode))
                return false;
        }

        // return true validation if no codes match the passed code
        return true;
    }

    /***
     *
     * @param account (Account of any sub type passed to function to be converted
     *                to the dto equivilant)
     * @return (Returns the dto of every type of account based on the role of each
     *         account
     * @throws Exception (throws excepion based on failed update)
     */
    public static UserDto convertAccountToDto(Account account) throws Exception {
        if (account == null)
            throw new Exception("Passed account is null.");

        return new UserDto(
                account.getId(),
                account.getEmail(),
                ((User) account).getDisplayName(),
                ((User) account).getIdentifierCode(),
                "USER",
                ((User) account).getCollectionId(),
                ((User) account).getBannerImg(),
                ((User) account).getProfileImg(),
                ((User) account).getFacebookLink(),
                ((User) account).getTwitterLink(),
                ((User) account).getInstagramLink(),
                ((User) account).getYoutubeLink(),
                ((User) account).getDiscordLink(),
                ((User) account).getRedditLink(),
                ((User) account).getPersonalEmailLink(),
                ((User) account).getPersonalWebsiteLink());
    }

    @Override
    public Account getAccount(String accountId) throws Exception {
        return accountRepository.findById(new ObjectId(accountId)).get();
    }

    @Override
    public Boolean checkForAccountExistance(String accountId) throws Exception {
        Optional<Account> foundUser = accountRepository.findById(new ObjectId(accountId));

        return foundUser.isPresent();
    }

    @Override
    public UserDto getSelf() throws Exception {
        // get authentication context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // get authorized account from principle
        Account currentAccount = (Account) authentication.getPrincipal();

        return convertAccountToDto(currentAccount);
    }

    @Override
    public List<Account> allUsers() {
        List<Account> accounts = new ArrayList<>();

        accounts = accountRepository.findAll();

        return accounts;
    }

    private boolean validateDisplayName(String newDisplayName) {
        // return false if empty string
        System.out.println(newDisplayName);
        if (newDisplayName.isEmpty())
            return false;

        // return false if passed string is smaller than 4 characters
        if (newDisplayName.length() < 4)
            return false;

        int numLetters = 0;
        int numNumbers = 0;

        for (int i = 0; i < newDisplayName.length(); i++) {
            // if character isn't a valid character, letter, or number, return false
            if (!Character.isDigit(newDisplayName.charAt(i)) &&
                    !Character.isLetter(newDisplayName.charAt(i)) && newDisplayName.charAt(i) != '_')
                return false;

            if (Character.isLetter(newDisplayName.charAt(i))) {
                numLetters += 1;
            } else if (Character.isDigit(newDisplayName.charAt(i))) {
                numNumbers += 1;
            }
        }

        if (numNumbers > numLetters)
            return false;

        if (numLetters == 0)
            return false;

        return true;
    }

    @Override
    public String updateFacebookLink(String newLink, String accountId) throws Exception {
        try {

            Optional<Account> foundAccount = accountRepository.findById(new ObjectId(accountId));

            if (foundAccount.isEmpty())
                throw new Exception("Account Not Found.");

            User castedAccount = (User) foundAccount.get();

            castedAccount.setFacebookLink(newLink);
            accountRepository.save(castedAccount);

            return newLink;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public String updateInstagramLink(String newLink, String accountId) throws Exception {
        try {

            Optional<Account> foundAccount = accountRepository.findById(new ObjectId(accountId));

            if (foundAccount.isEmpty())
                throw new Exception("Account Not Found.");

            User castedAccount = (User) foundAccount.get();

            castedAccount.setInstagramLink(newLink);
            accountRepository.save(castedAccount);

            return newLink;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public String updateTwitterLink(String newLink, String accountId) throws Exception {
        try {

            Optional<Account> foundAccount = accountRepository.findById(new ObjectId(accountId));

            if (foundAccount.isEmpty())
                throw new Exception("Account Not Found.");

            User castedAccount = (User) foundAccount.get();

            castedAccount.setTwitterLink(newLink);
            accountRepository.save(castedAccount);

            return newLink;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public String updateYoutubeLink(String newLink, String accountId) throws Exception {
        try {

            Optional<Account> foundAccount = accountRepository.findById(new ObjectId(accountId));

            if (foundAccount.isEmpty())
                throw new Exception("Account Not Found.");

            User castedAccount = (User) foundAccount.get();

            castedAccount.setYoutubeLink(newLink);
            accountRepository.save(castedAccount);

            return newLink;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public String updateRedditLink(String newLink, String accountId) throws Exception {
        try {

            Optional<Account> foundAccount = accountRepository.findById(new ObjectId(accountId));

            if (foundAccount.isEmpty())
                throw new Exception("Account Not Found.");

            User castedAccount = (User) foundAccount.get();

            castedAccount.setRedditLink(newLink);
            accountRepository.save(castedAccount);

            return newLink;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public String updateDiscordLink(String newLink, String accountId) throws Exception {
        try {

            Optional<Account> foundAccount = accountRepository.findById(new ObjectId(accountId));

            if (foundAccount.isEmpty())
                throw new Exception("Account Not Found.");

            User castedAccount = (User) foundAccount.get();

            castedAccount.setDiscordLink(newLink);
            accountRepository.save(castedAccount);

            return newLink;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public String updatePersonalEmailLink(String newLink, String accountId)
            throws Exception {
        try {

            Optional<Account> foundAccount = accountRepository.findById(new ObjectId(accountId));

            if (foundAccount.isEmpty())
                throw new Exception("Account Not Found.");

            User castedAccount = (User) foundAccount.get();

            castedAccount.setPersonalEmailLink(newLink);
            accountRepository.save(castedAccount);

            return newLink;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public String updatePersonalWebsiteLink(String newLink, String accountId)
            throws Exception {
        try {

            Optional<Account> foundAccount = accountRepository.findById(new ObjectId(accountId));

            if (foundAccount.isEmpty())
                throw new Exception("Account Not Found.");

            User castedAccount = (User) foundAccount.get();

            castedAccount.setPersonalWebsiteLink(newLink);
            accountRepository.save(castedAccount);

            return newLink;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public DisplayNameChangeDto changeDisplayName(String accountID, String newDisplayName) throws Exception {
        try {
            // get object
            Optional<Account> foundAccount = accountRepository.findById(new ObjectId(accountID));

            // check if it is empty
            if (foundAccount.isEmpty()) {
                throw new Exception("Couldn't find account.");
            }

            // validate new display name
            if (!validateDisplayName(newDisplayName)) {
                throw new Exception("Display name not valid.");
            }

            // set new display name in account obj
            User accountObj = ((User) foundAccount.get());
            accountObj.setDisplayName(newDisplayName);

            // set new identifier code in account obj
            String newIdentifier = generateIdentifierCode(newDisplayName);
            accountObj.setIdentifierCode(newIdentifier);

            // save account
            accountRepository.save(accountObj);

            return new DisplayNameChangeDto(
                    newDisplayName,
                    newIdentifier);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public String updateProfileImg(String accountId, MultipartFile file) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateProfileImg'");
    }

    @Override
    public String updateBannerImg(String accountId, MultipartFile file) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateBannerImg'");
    }

    // /***
    // *
    // * @param accountId (String to represend the id of a account saved in the
    // repo)
    // * @return (Returns true/false value for account existing in repo based on
    // passed id
    // */
    // public boolean checkForAccountExistance(String accountId) {
    // return !accountRepository.existsById(new ObjectId(accountId));
    // }

    // /***
    // *
    // * @param accountId (String to represend the id of an account saved in the
    // repo)
    // * @param file (Multipart file that needs to be stored
    // * @return (Returns boolean value based on successful storage of image and
    // update on account)
    // *
    // * Function takes in an image and an id to identify the account a user wannts
    // to update the profile image on,
    // * then attempts to update that profile image on the db for that user.
    // */
    // public String updateProfileImg(String accountId, MultipartFile file) {
    // Optional<Account> foundAccount = accountRepository.findById(new
    // ObjectId(accountId));

    // // if account isn't found with passed id, return false
    // if (foundAccount.isEmpty()) return null;

    // try {
    // // checks for profile img already existing for user
    // if (!((User) foundAccount.get()).getProfileImg().isEmpty()) {
    // // deletes old profile img
    // if (!javaFSService.deleteAsset(((User) foundAccount.get()).getProfileImg()))
    // {
    // return null;
    // }
    // }

    // // stores new image in java fs
    // String id = javaFSService.addAsset("Profile Img",file);

    // // updates account and saves it to repo
    // ((User) foundAccount.get()).setProfileImg(id);
    // accountRepository.save(foundAccount.get());

    // return id;
    // }
    // catch(Exception e) {
    // // catches exception during updating process
    // return null;
    // }
    // }

    // /***
    // *
    // * @param accountId (String to represent the id of an account saved in the
    // repo)
    // * @param file (Multipart file that needs to be stored
    // * @return (Returns boolean value based on successful storage of image and
    // update on account)
    // *
    // * Function takes in an image and an id to identify the account a user wannts
    // to update the babber image on,
    // * then attempts to update that banner image on the db for that user.
    // */
    // public String updateBannerImg(String accountId, MultipartFile file) throws
    // Exception {
    // Optional<Account> foundAccount = accountRepository.findById(new
    // ObjectId(accountId));

    // // if account isn't found with passed id, return false
    // if (foundAccount.isEmpty()) throw new Exception("Can't find account.");

    // try {
    // // checks for banner img already existing for user
    // if (!((User) foundAccount.get()).getBannerImg().isEmpty()) {
    // // deletes old profile img
    // if (!javaFSService.deleteAsset(((User) foundAccount.get()).getBannerImg())) {
    // throw new Exception("Conflict during update.");
    // }
    // }

    // // stores new image in java fs
    // String id = javaFSService.addAsset("Banner Img",file);

    // // updates account and saves it to repo
    // ((User) foundAccount.get()).setBannerImg(id);
    // accountRepository.save(foundAccount.get());
    // return id;
    // }
    // catch(Exception e) {
    // // catches exception during updating process
    // throw e;
    // }
    // }
}
