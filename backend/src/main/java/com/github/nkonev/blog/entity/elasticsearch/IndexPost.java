package com.github.nkonev.blog.entity.elasticsearch;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import static com.github.nkonev.blog.entity.elasticsearch.IndexPost.INDEX;

@Document(indexName = INDEX, type = IndexPost.TYPE, createIndex = false)
public class IndexPost {

    public static final String INDEX = "post";

    public static final String FIELD_ID = "id";
    public static final String FIELD_TEXT = "text";
    public static final String FIELD_TITLE = "title";
    public static final String TYPE = "_doc";

    @Id
    private Long id;

    private String title;

    private String text;

    public IndexPost() { }

    public IndexPost(Long id, String title, String text) {
        this.id = id;
        this.title = title;
        this.text = text;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}