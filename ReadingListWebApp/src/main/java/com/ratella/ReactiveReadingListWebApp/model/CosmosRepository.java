package com.ratella.ReactiveReadingListWebApp.model;

import com.azure.cosmos.*;
import com.azure.cosmos.models.*;
import com.azure.cosmos.util.CosmosPagedFlux;
import com.sun.xml.internal.bind.v2.TODO;
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
        CosmosQueryRequestOptions queryOptions = new CosmosQueryRequestOptions();
        queryOptions.setMaxBufferedItemCount(10);
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
                .map(cosmosItemResponse -> {
                    return cosmosItemResponse.getItem();
                });

    }

    public Mono<Book> updateBook(Book book) {
        return container
                .replaceItem(book, book.getId(), new PartitionKey(book.getReader()))
                .map(CosmosItemResponse::getItem);
    }

    public Mono<Book> findBookByID(String id, String partitionKey) {
        return container
                .readItem(id, new PartitionKey(partitionKey), Book.class)
                .map(CosmosItemResponse::getItem);
    }

    public Mono<CosmosItemResponse<Object>> deleteBookByID(String id, String partitionKey) {
        return container
                .deleteItem(id, new PartitionKey(partitionKey));
        //TODO Why is getItem() returning null?
        // .map(CosmosAsyncItemResponse::getItem);

    }


    @PostConstruct
    private void cosmosSetup() {
        CosmosContainerProperties containerProperties = new CosmosContainerProperties(containerName, "/reader");
        getClient()
                .createDatabaseIfNotExists(databaseName)
                .flatMap(databaseResponse -> {
                    database = getClient().getDatabase(databaseResponse
                            .getProperties()
                            .getId());
                    return database
                            .createContainerIfNotExists(containerProperties, ThroughputProperties.createManualThroughput(400));
                })
                .flatMap(containerResponse -> {
                    container = database.getContainer(containerResponse
                            .getProperties()
                            .getId());
                    return Mono.empty();
                })
                .subscribeOn(Schedulers.elastic())
                .subscribe();
    }


    private CosmosAsyncClient getClient() {
        if (client == null) {
            logger.info(endpoint);
            client = new CosmosClientBuilder()
                    .endpoint(endpoint)
                    .key(key)
                    .preferredRegions(locations)
                    .contentResponseOnWriteEnabled(true)
                    .consistencyLevel(ConsistencyLevel.SESSION)
                    .buildAsyncClient();
            return client;
        }
        return client;

    }


}
