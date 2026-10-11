
package com.jubayer.cholobodlai.Security;

import com.jubayer.cholobodlai.Repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class MongoUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public MongoUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String phone) {
        return users.findByPhone(phone)
                .map(AppUserDetails::new)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));
    }
}
