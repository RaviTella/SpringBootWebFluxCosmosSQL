package com.ratella.ReactiveReadingListWebApp.controller;


import com.ratella.ReactiveReadingListWebApp.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.thymeleaf.spring5.context.webflux.IReactiveDataDriverContextVariable;
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Controller
public class ReadingListController {


    private CosmosRepository cosmosRepository;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private RecommendationRepository recommendationRepository;

    @Autowired
    public ReadingListController(
            CosmosRepository cosmosRepository, RecommendationRepository recommendationRepository) {
        this.cosmosRepository = cosmosRepository;
        this.recommendationRepository = recommendationRepository;
    }

    @GetMapping(value = "/readingList")
    public String readersBooks(Model model) {
        Flux<Book> readingList = cosmosRepository.getReadingList("Tella");
        model.addAttribute("books", readingList);
        model.addAttribute("recommendations", recommendationRepository.getRecommendations());
        return "readingList";
    }


    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public Mono<String> addToReadingList(Book book) {
        book.setReader("Tella");
        book.setId(UUID
                .randomUUID()
                .toString());
        logger.info("Adding a book -> " + book);
        return cosmosRepository
                .createBook(book)
                .map(responseStatusCode -> {
                    return "redirect:/readingList";
                });
    }

    @RequestMapping(value = "/delete/{id}", method = RequestMethod.GET)
    public Mono<String> deleteFromReadingList(@PathVariable String id) {
        logger.info("Deleting a book with ID -> " + id);
        return cosmosRepository
                .deleteBookByID(id, "Tella")
                .map(responseStatusCode -> {
                    return "redirect:/readingList";
                });
    }

    @RequestMapping(value = "/edit/{id}", method = RequestMethod.GET)
    public Mono<String> editReadingListView(@PathVariable String id, Model model) {
        logger.info("Finding a book with ID -> " + id);
        return cosmosRepository
                .findBookByID(id, "Tella")
                .map(book -> {
                    model.addAttribute("book", book);
                    return "editReadingList";
                });


    }

    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public Mono<String> editReadingListItem(Book updatedBook) {
        return cosmosRepository
                .findBookByID(updatedBook.getId(), "Tella")
                .map(
                        currentBook -> {
                            currentBook.setTitle(updatedBook.getTitle());
                            currentBook.setAuthor(updatedBook.getAuthor());
                            currentBook.setIsbn(updatedBook.getIsbn());
                            currentBook.setDescription(updatedBook.getDescription());
                            return currentBook;
                        })
                .flatMap(currentBook -> {
                    logger.info("Updating a book -> " + currentBook.toString());
                    return cosmosRepository.updateBook(currentBook);
                })
                .map(responseStatusCode -> {
                    return "redirect:/readingList";
                });


    }

}
