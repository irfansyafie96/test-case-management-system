package com.yourproject.tcm.controller;

import com.yourproject.tcm.model.Invitation;
import com.yourproject.tcm.model.dto.AcceptInviteRequest;
import com.yourproject.tcm.model.dto.InviteRequest;
import com.yourproject.tcm.service.InvitationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    @Autowired
    private InvitationService invitationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createInvitation(@Valid @RequestBody InviteRequest request) {
        try {
            Invitation invitation = invitationService.createInvitation(request.getEmail(), request.getRole());
            return ResponseEntity.ok("Invitation sent to " + invitation.getEmail());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/accept")
    public ResponseEntity<?> acceptInvitation(@Valid @RequestBody AcceptInviteRequest request) {
        try {
            invitationService.acceptInvitation(request.getToken(), request.getUsername(), request.getPassword());
            return ResponseEntity.ok("Invitation accepted! You can now login.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @GetMapping("/{token}")
    public ResponseEntity<?> getInvitation(@PathVariable String token) {
        try {
            Invitation invitation = invitationService.getInvitation(token);
            if (!invitation.isValid()) {
                return ResponseEntity.badRequest().body("Invitation invalid or expired");
            }
            // Return basic info for the frontend to show "Join Company X as QA"
            return ResponseEntity.ok(invitation);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
