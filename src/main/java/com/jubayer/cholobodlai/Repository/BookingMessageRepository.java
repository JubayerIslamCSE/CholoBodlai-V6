package com.jubayer.cholobodlai.Repository;
import com.jubayer.cholobodlai.Model.BookingMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
public interface BookingMessageRepository extends MongoRepository<BookingMessage,String> {
    List<BookingMessage> findTop100ByTripIdOrderByCreatedAtDesc(String tripId);
}
