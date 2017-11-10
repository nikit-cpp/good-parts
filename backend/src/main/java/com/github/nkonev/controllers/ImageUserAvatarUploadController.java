package com.github.nkonev.controllers;

import com.github.nkonev.dto.UserAccountDetailsDTO;
import com.github.nkonev.exception.DataNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@RestController
public class ImageUserAvatarUploadController extends AbstractImageUploadController {

    public static final String POST_TEMPLATE = "/api/image/user/avatar";
    public static final String GET_TEMPLATE = POST_TEMPLATE + "/{id}.{ext}";

    public static final String imageType = "avatarImages";

    @PostMapping(POST_TEMPLATE)
    @PreAuthorize("isAuthenticated()")
    public ImageResponse postImage(
            @RequestPart(value = IMAGE_PART) MultipartFile imagePart,
            @NotNull @AuthenticationPrincipal UserAccountDetailsDTO userAccount
    ) throws SQLException, IOException {
        return super.postImage(
            imagePart,
            (conn, contentLength, contentType) -> {
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO images.user_avatar_image(img, content_type) VALUES (?, ?) RETURNING id")){
                    ps.setString(2, contentType);
                    ps.setBinaryStream(1, imagePart.getInputStream(), (int) contentLength);
                    try(ResultSet resp = ps.executeQuery()) {
                        if(!resp.next()) {
                            throw new RuntimeException("Expected result");
                        }
                        return resp.getObject("id", UUID.class);
                    }

                } catch (SQLException | IOException e) {
                    throw new RuntimeException(e);
                }
            },
            (uuid) -> {
                String relativeUrl = UriComponentsBuilder.fromUriString(GET_TEMPLATE)
                        .buildAndExpand(uuid, getExtension(imagePart.getContentType()))
                        .toUriString();
                return new ImageResponse(relativeUrl, customConfig.getBaseUrl() + relativeUrl);
            }
        );
    }

    ///////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////

    @GetMapping(GET_TEMPLATE)
    public void getImage(
            @PathVariable("id")UUID id,
            HttpServletResponse response,
            HttpServletRequest request
    ) throws SQLException, IOException {
        if(!set304(id, request, response, imageType)) {
            super.getImage(
                (Connection conn) -> {
                    try (PreparedStatement ps = conn.prepareStatement("SELECT img, length(img) as content_length, content_type, create_date_time FROM images.user_avatar_image WHERE id = ?");) {
                        ps.setObject(1, id);
                        try (ResultSet rs = ps.executeQuery();) {
                            if (rs.next()) {
                                response.setContentType(rs.getString("content_type"));
                                response.setContentLength(rs.getInt("content_length"));
                                addCacheHeaders(id, "create_date_time", rs, response, imageType);
                                try (InputStream imgStream = rs.getBinaryStream("img");) {
                                    copyStream(imgStream, response.getOutputStream());
                                } catch (SQLException | IOException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                throw new DataNotFoundException("avatar image with id '" + id + "' not found");
                            }
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            );
        }
    }

}