package com.github.nkonev.blog.config;

import com.github.nkonev.blog.Constants;
import com.github.nkonev.blog.converter.PostConverter;
import com.github.nkonev.blog.entity.elasticsearch.IndexPost;
import com.github.nkonev.blog.services.PostService;
import com.github.nkonev.blog.utils.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Qualifier(ElasticsearchConfig.ELASTICSEARCH_CONFIG)
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.github.nkonev.blog.repo.elasticsearch")
@EntityScan(basePackages = "com.github.nkonev.blog.entity.elasticsearch")
public class ElasticsearchConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchConfig.class);
    public static final String ELASTICSEARCH_CONFIG = "elasticsearchConfig";

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private PostConverter postConverter;

    @Value(Constants.CUSTOM_ELASTICSEARCH_DROP_FIRST)
    private boolean dropFirst;


    @Value("${custom.elasticsearch.create-index:true}")
    private boolean createIndex;

    @Value("classpath:/config/index-post.json")
    private Resource indexSettings;

    @Value("classpath:/config/index-post-mapping.json")
    private Resource postMapping;

    @Autowired
    private PostService postService;

    @Value("${custom.elasticsearch.refresh-on-start:true}")
    private boolean refreshOnStart;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value(Constants.ELASTICSEARCH_REFRESH_ON_START_KEY_TIMEOUT)
    private int timeout;

    @Value(Constants.ELASTICSEARCH_REFRESH_ON_START_KEY_TIMEUNIT)
    private TimeUnit timeUnit;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");

    @PostConstruct
    public void pc(){

        if (dropFirst) {
            try {
                LOGGER.info("Dropping elasticsearch index");
                elasticsearchTemplate.deleteIndex(IndexPost.INDEX);
            } catch (Exception e) {
                LOGGER.error("Error during dropping elasticsearch index");
            }
        }

        if (createIndex) {
            try {
                LOGGER.info("Creating elasticsearch index");
                final String settings = ResourceUtils.stringFromResource(indexSettings);
                elasticsearchTemplate.createIndex(IndexPost.INDEX, settings);

                final String mapping = ResourceUtils.stringFromResource(postMapping);
                elasticsearchTemplate.putMapping(IndexPost.INDEX, IndexPost.TYPE, mapping);
                LOGGER.info("Successfully created elasticsearch index");
            } catch (Exception e) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.error("Error during create elasticsearch index", e);
                } else {
                    LOGGER.info("Error during create elasticsearch index: " + e.getMessage());
                }
            }
        }

        if (refreshOnStart) {
            LOGGER.info("Will try to refresh elasticsearch index");
            final String key = getKey();

            final boolean firstRun = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, LocalDateTime.now().format(formatter)));
            String dateTimeString = redisTemplate.opsForValue().get(key);
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeString, formatter);

            if (dateTime.plus(Duration.of(timeout, timeUnit.toChronoUnit())).isBefore(LocalDateTime.now()) || firstRun) {
                LOGGER.info("Condition is successful, so we'll refresh elasticsearch index");
                postService.refreshFulltextIndex(false);
                redisTemplate.opsForValue().set(key, LocalDateTime.now().format(formatter));
                if (dropFirst){
                    redisTemplate.delete(key);
                }
            } else {
                LOGGER.info("Condition isn't successful, so we won't refresh elasticsearch index");
            }
        }

    }
    private String getKey() {
        return "elasticsearch:"+ IndexPost.INDEX+":build-time";
    }

}
