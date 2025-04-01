package com.nutria.app.config;

import com.nutria.app.model.UserCredential;
import com.nutria.app.repository.UserCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UserCredentialRepository userCredentialRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<UserCredential> credential = userCredentialRepository.findByEmail(email);
        return credential.map(CustomUserDetails::new).orElseThrow(()-> new UsernameNotFoundException("User not found with email" + email));
    }
}