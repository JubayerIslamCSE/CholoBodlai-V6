package com.jubayer.cholobodlai.Repository;

import com.jubayer.cholobodlai.Model.Emums.Role;
import com.jubayer.cholobodlai.Model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByPhone(String phone);
    List<User> findByRole(Role role);
    List<User> findByEmailIgnoreCase(String email);
}
