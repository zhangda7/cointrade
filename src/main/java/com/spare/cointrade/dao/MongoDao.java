package com.spare.cointrade.dao;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by dada on 2017/7/16.
 */
@Component
public class MongoDao {

    private static Logger logger = LoggerFactory.getLogger(MongoDao.class);

    private MongoClient mongoClient;

    private MongoDatabase mongoDatabase;

    private String uri;

    @Value("${mongo.host}")
    private String host;

    @Value("${mongo.port}")
    private int port;

    @Value("${mongo.database}")
    private String database;

    @PostConstruct
    public void init() {
        this.uri = "mongodb://" + host + ":" + port;
//        this.database = database;
        MongoClientOptions.Builder builder = new MongoClientOptions.Builder();
        builder.connectionsPerHost(20).maxWaitTime(20000);

        mongoClient = new MongoClient(new MongoClientURI(uri, builder));
        mongoDatabase = mongoClient.getDatabase(database);

    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }
}
