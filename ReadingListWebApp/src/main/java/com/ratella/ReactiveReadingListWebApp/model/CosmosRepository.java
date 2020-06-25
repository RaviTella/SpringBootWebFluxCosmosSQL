package com.ratella.ReactiveReadingListWebApp.model;

import com.azure.cosmos.*;
import com.azure.cosmos.models.*;
import com.azure.cosmos.util.CosmosPagedFlux;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import java.util.List;


@Service
public class CosmosRepository {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private CosmosDB cosmosDB;

    @Autowired
   public CosmosRepository(CosmosDB cosmosDB){
        this.cosmosDB = cosmosDB;
    }

    public Flux<Book> getReadingList(String reader) {
        CosmosQueryRequestOptions queryOptions = new CosmosQueryRequestOptions();
        queryOptions.setMaxBufferedItemCount(10);
        String query = "SELECT * FROM ReadingList r WHERE r.reader = " + "'" + reader + "'";
        CosmosPagedFlux<Book> pagedFluxResponse = cosmosDB.getContainer().queryItems(
                query, queryOptions, Book.class);
        return pagedFluxResponse
                .byPage()
                .flatMap(page -> Flux.fromIterable(page.getElements()));
    }

    public Mono<Book> createBook(Book book) {
        return cosmosDB.getContainer()
                .createItem(book)
                .map(cosmosItemResponse -> {
                    return cosmosItemResponse.getItem();
                });

    }

    public Mono<Book> updateBook(Book book) {
        return cosmosDB.getContainer()
                .replaceItem(book, book.getId(), new PartitionKey(book.getReader()))
                .map(CosmosItemResponse::getItem);
    }

    public Mono<Book> findBookByID(String id, String partitionKey) {
        return cosmosDB.getContainer()
                .readItem(id, new PartitionKey(partitionKey), Book.class)
                .map(CosmosItemResponse::getItem);
    }

    public Mono<CosmosItemResponse<Object>> deleteBookByID(String id, String partitionKey) {
        return cosmosDB.getContainer()
                .deleteItem(id, new PartitionKey(partitionKey));
        //TODO Why is getItem() returning null?
        // .map(CosmosAsyncItemResponse::getItem);

    }

}
