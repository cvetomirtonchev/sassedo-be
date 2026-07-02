package server.sassedo.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.sassedo.user.data.dto.ERole;
import server.sassedo.user.data.dto.Role;
import server.sassedo.user.repository.RoleRepository;

import jakarta.annotation.PostConstruct;

@Service
public class RoleService {
    
    @Autowired
    private RoleRepository roleRepository;

    @PostConstruct
    public void init() {
        long count = roleRepository.count();
        if (count == 0) {
            roleRepository.save(new Role(ERole.ROLE_USER));
            roleRepository.save(new Role(ERole.ROLE_MODERATOR));
            roleRepository.save(new Role(ERole.ROLE_ADMIN));
        }
    }
}
