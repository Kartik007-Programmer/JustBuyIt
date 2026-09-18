package com.example.JustBuyIt.Services;

import com.example.JustBuyIt.Models.Users;
import com.example.JustBuyIt.Repository.UsersRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsersService {

    private final UsersRepo usersRepo;

    public UsersService(UsersRepo usersRepo) {
        this.usersRepo = usersRepo;
    }

    List<Users> getUsers() {
        return usersRepo.findAll();
    }

    Users getUser(Long id) {
        return usersRepo.findById(id).orElseThrow(() -> new RuntimeException("User Not Found!"));
    }

    public Users getUserByEmail(String name) {
        return (Users) usersRepo.findByEmail(name).orElseThrow(() -> new RuntimeException("User Not Found!"));
    }
}
