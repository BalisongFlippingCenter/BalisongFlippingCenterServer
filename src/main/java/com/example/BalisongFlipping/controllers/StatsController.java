package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.SiteStatsDto;
import com.example.BalisongFlipping.repositories.AccountRepository;
import com.example.BalisongFlipping.repositories.CollectionKnifeRepository;
import com.example.BalisongFlipping.repositories.PostsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/stats")
@RestController
public class StatsController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CollectionKnifeRepository collectionKnifeRepository;

    @Autowired
    private PostsRepository postsRepository;

    @GetMapping
    public ResponseEntity<?> getSiteStats() {
        return new ResponseEntity<>(new SiteStatsDto(
                accountRepository.count(),
                collectionKnifeRepository.count(),
                postsRepository.count()
        ), HttpStatus.OK);
    }
}
