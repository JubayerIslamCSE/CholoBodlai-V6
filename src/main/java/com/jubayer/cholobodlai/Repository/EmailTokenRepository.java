package com.jubayer.cholobodlai.Repository;
import com.jubayer.cholobodlai.Model.EmailToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;
public interface EmailTokenRepository extends MongoRepository<EmailToken,String> {
    Optional<EmailToken> findByTokenHashAndUsedFalse(String hash);
    List<EmailToken> findByUserIdAndPurposeAndUsedFalse(String userId, EmailToken.Purpose purpose);
}
