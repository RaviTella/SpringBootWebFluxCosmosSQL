package com.ratella.ReactiveReadingListWebApp.model;

import com.azure.cosmos.*;
import com.azure.cosmos.models.*;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import java.util.List;


@Component
public class CosmosRepository {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Value("${database.endpoint}")
    private String endpoint;
    @Value("${database.key}")
    private String key;
    @Value("${database.databaseName}")
    private String databaseName;
    @Value("${database.containerName}")
    private String containerName;
    @Value("#{'${database.locations}'.split(',')}")
    private List<String> locations;
    private CosmosAsyncClient client;
    private CosmosAsyncDatabase database;
    private CosmosAsyncContainer container;


    public Flux<Book> getReadingList(String reader) {
        FeedOptions queryOptions = new FeedOptions();
        queryOptions.setMaxItemCount(10);
        String query = "SELECT * FROM ReadingList r WHERE r.reader = " + "'" + reader + "'";
        CosmosPagedFlux<Book> pagedFluxResponse = container.queryItems(
                query, queryOptions, Book.class);
        return pagedFluxResponse
                .byPage()
                .flatMap(page -> Flux.fromIterable(page.getElements()));
    }

    public Mono<Book> createBook(Book book) {
        return container
                .createItem(book)
                .map(CosmosAsyncItemResponse::getItem);
    }

    public Mono<Book> updateBook(Book book) {
        return container
                .replaceItem(book, book.getId(), new PartitionKey(book.getReader()))
                .map(CosmosAsyncItemResponse::getItem);
    }

    public Mono<Book> findBookByID(String id, String partitionKey) {
        return container
                .readItem(id, new PartitionKey(partitionKey), Book.class)
                .map(CosmosAsyncItemResponse::getItem);
    }

    public Mono<CosmosAsyncItemResponse> deleteBookByID(String id, String partitionKey) {
        return container.deleteItem(id, new PartitionKey(partitionKey));
        // TODO: 3/20/2020  Why is map throwing nullpointer exception?
        // .map(CosmosAsyncItemResponse::getItem);//.log();
    }


    @PostConstruct
    private void cosmosSetup() {
        CosmosContainerProperties containerProperties = new CosmosContainerProperties(containerName, "/reader");
        Flux
                .from(Mono.just(getClient()))
                .flatMap(n -> n.createDatabaseIfNotExists(databaseName))
                .flatMap(databaseResponse -> databaseResponse
                        .getDatabase()
                        .createContainerIfNotExists(containerProperties, 400))
                .flatMap(containerResponse -> {
                    container = containerResponse.getContainer();
                    return Mono.empty();
                })
                .subscribeOn(Schedulers.elastic())
                .subscribe();
    }


    private CosmosAsyncClient getClient() {
        if (client == null) {
            ConnectionPolicy defaultPolicy = ConnectionPolicy.getDefaultPolicy();
            defaultPolicy.setPreferredLocations(locations);
            logger.info(endpoint);
            client = new CosmosClientBuilder()
                    .setEndpoint(endpoint)
                    .setKey(key)
                    .setConnectionPolicy(defaultPolicy)
                    .setConsistencyLevel(ConsistencyLevel.EVENTUAL)
                    .buildAsyncClient();
            return client;
        }
        return client;
    }


}
