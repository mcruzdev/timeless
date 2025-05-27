package dev.matheuscruz.infra.persistence.converters;

import dev.matheuscruz.infra.security.AESAdapter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.AttributeConverter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

@ApplicationScoped
public class SecretAttributeConverter implements AttributeConverter<String, String> {

    final AESAdapter secretAdapter;

    public SecretAttributeConverter(AESAdapter aesAdapter) {
        this.secretAdapter = aesAdapter;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        try {
            if (attribute == null) {
                return null;
            }
            return this.secretAdapter.encrypt(attribute);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException
                | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null) {
                return null;
            }

            return this.secretAdapter.decrypt(dbData);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException
                | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }
}
