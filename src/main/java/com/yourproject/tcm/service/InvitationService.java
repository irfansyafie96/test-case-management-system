package com.yourproject.tcm.service;

import com.yourproject.tcm.model.Invitation;
import com.yourproject.tcm.model.Organization;
import com.yourproject.tcm.model.Role;
import com.yourproject.tcm.model.User;
import com.yourproject.tcm.repository.InvitationRepository;
import com.yourproject.tcm.repository.RoleRepository;
import com.yourproject.tcm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
public class InvitationService {

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private TcmService tcmService; // To get current user

    public Invitation createInvitation(String email, String roleName) {
        User currentUser = tcmService.getCurrentUser();
        Organization org = currentUser.getOrganization();

        if (org == null) {
            throw new RuntimeException("Current user does not belong to an organization");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("User with this email already exists");
        }

        Invitation invitation = new Invitation(email, roleName, org);
        invitation = invitationRepository.save(invitation);

        // Generate link (assuming frontend runs on port 4200)
        String link = "http://localhost:4200/join?token=" + invitation.getToken();
        
        emailService.sendInvitationEmail(email, link, org.getName());

        return invitation;
    }

    public User acceptInvitation(String token, String username, String password) {
        Invitation invitation = invitationRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Invalid invitation token"));

        if (!invitation.isValid()) {
            throw new RuntimeException("Invitation has expired or already been used");
        }

        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username is already taken");
        }

        // Create User
        User user = new User(username, invitation.getEmail(), encoder.encode(password));
        user.setOrganization(invitation.getOrganization());

        // Assign Role
        Set<Role> roles = new HashSet<>();
        Role role = roleRepository.findByName(invitation.getRole())
                .orElseThrow(() -> new RuntimeException("Role not found: " + invitation.getRole()));
        roles.add(role);
        user.setRoles(roles);

        userRepository.save(user);

        // Mark invite as accepted
        invitation.setAccepted(true);
        invitationRepository.save(invitation);

        return user;
    }
    
    public Invitation getInvitation(String token) {
        return invitationRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Invalid invitation token"));
    }
}
