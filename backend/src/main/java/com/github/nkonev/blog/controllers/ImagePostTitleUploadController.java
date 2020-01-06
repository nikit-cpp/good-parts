package com.github.nkonev.blog.controllers;

import com.github.nkonev.blog.dto.UserAccountDetailsDTO;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.UUID;

import static com.github.nkonev.blog.Constants.Urls.API;
import static com.github.nkonev.blog.Constants.Urls.IMAGE;

@RestController
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ImagePostTitleUploadController extends AbstractImageUploadController {

    public static final String POST_TEMPLATE = API+IMAGE+"/post/title";
    public static final String GET_TEMPLATE = POST_TEMPLATE + "/{id}.{ext}";

    public static final String imageType = "postTitleImages";

    @PostMapping(POST_TEMPLATE)
    @PreAuthorize("isAuthenticated()")
    public ImageResponse postImage(
            @RequestPart(value = IMAGE_PART) MultipartFile imagePart,
            @NotNull @AuthenticationPrincipal UserAccountDetailsDTO userAccount
    ) throws IOException {
        return insertImage(
                imagePart.getSize(),
                imagePart.getContentType(),
                imagePart.getInputStream()
        );
    }

    @Override
    public ImageResponse insertImage(
            long contentLength,
            String contentType,
            InputStream inputStream
    )  {
        try {
            return super.postImage(
                    "INSERT INTO images.post_title_image(img, content_type) VALUES (?, ?) RETURNING id",
                    GET_TEMPLATE,
                    contentLength,
                    contentType,
                    inputStream
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    ///////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////

    @GetMapping(GET_TEMPLATE)
    public void getImage(
            @PathVariable("id")UUID id,
            HttpServletResponse response,
            HttpServletRequest request
    ) {
        super.getImage(
        "SELECT img, length(img) as content_length, content_type, create_date_time FROM images.post_title_image WHERE id = ?",
                id,
                request,
                response,
                imageType,
                "post title image with id '" + id + "' not found"
        );
    }

    public int clearPostTitleImages(){
        return jdbcTemplate.update("delete from images.post_title_image where id in (" +
                "select i.id from images.post_title_image i " +
                "left join posts.post p on p.title_img like '%' || '/api/image/post/title/' || i.id || '%' " +
                "where p.id is null" +
        ");");
    }
}
 