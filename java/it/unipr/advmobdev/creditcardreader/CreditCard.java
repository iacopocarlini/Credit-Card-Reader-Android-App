package it.unipr.advmobdev.creditcardreader;

import java.util.HashMap;
import java.util.Map;

// DATA MODEL
public class CreditCard
{
    private String cardNumber, expirationMonth, expirationYear, imagePath;

    public CreditCard(String cNumber, String expMonth, String expYear, String img)
    {
        cardNumber = cNumber;
        expirationMonth = expMonth;
        expirationYear = expYear;
        imagePath = img;
    }


    public Map<String, String> getData()
    {
        Map<String, String> data = new HashMap<>();

        data.put("number", cardNumber);
        data.put("expMonth", expirationMonth);
        data.put("expYear", expirationYear);
        data.put("imgPath", imagePath);

        return data;
    }


    public String getCardNumber() {
        return cardNumber;
    }


    public String getExpirationMonth() {
        return expirationMonth;
    }


    public String getExpirationYear() {
        return expirationYear;
    }


    public String getImagePath() {
        return imagePath;
    }


    // Luhn Formula - false: not valid card number / true: number is valid
    public boolean CheckLuhn()
    {
        String processedCardNumber = cardNumber.replaceAll("[^\\d]", "");

        int sum = 0;
        boolean alternate = false;
        for (int i = processedCardNumber.length() - 1; i >= 0; i--)
        {
            int n = Integer.parseInt(processedCardNumber.substring(i, i + 1));
            if (alternate)
            {
                n *= 2;
                if (n > 9)
                {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }


    public boolean isValid()
    {
        boolean result = false;

        if (cardNumber.length() > 0
                && expirationMonth.length() > 0
                && expirationYear.length() > 0
                && imagePath.length() > 0
                && CheckLuhn())
            result = true;

        return result;
    }

}
