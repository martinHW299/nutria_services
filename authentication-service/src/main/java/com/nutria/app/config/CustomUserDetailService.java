package com.nutria.app.config;

import com.nutria.app.model.UserCredential;
import com.nutria.app.repository.UserCredentialRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

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



//    @Autowired
//    private UserCredentialRepository userCredentialRepository;
//
//    @Override
//    @Transactional
//    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//        UserCredential user = userCredentialRepository.findActiveUserByEmail(email)
//                .orElseThrow(() -> new UsernameNotFoundException(
//                        "User not found with email: " + email
//                ));
//
//        return new CustomUserDetails(user);
//    }
}