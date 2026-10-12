
package com.jubayer.cholobodlai.Security;

import com.jubayer.cholobodlai.Repository.UserRepository;
import com.jubayer.cholobodlai.Service.EmailVerificationPolicy;

import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class MongoUserDetailsService implements UserDetailsService {

    private final UserRepository users;
    private final EmailVerificationPolicy policy;

    public MongoUserDetailsService(
            UserRepository users,
            EmailVerificationPolicy policy) {

        this.users = users;
        this.policy = policy;
    }

    @Override
    public UserDetails loadUserByUsername(String phone) {

        return users.findByPhone(phone)
                .map(user ->
                        new AppUserDetails(
                                user,
                                policy.required()
                        )
                )
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found"
                        )
                );
    }
}