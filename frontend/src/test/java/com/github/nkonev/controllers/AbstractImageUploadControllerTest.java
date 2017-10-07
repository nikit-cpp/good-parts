package com.github.nkonev.controllers;

import com.github.nkonev.AbstractUtTestRunner;
import com.github.nkonev.TestConstants;
import com.google.common.net.HttpHeaders;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class AbstractImageUploadControllerTest extends AbstractUtTestRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractImageUploadControllerTest.class);

    protected abstract String postTemplate();

    protected String postImage(String putUrlTemplate, MockMultipartFile mpf) throws Exception {
        MvcResult mvcResult = mockMvc.perform(
                MockMvcRequestBuilders.fileUpload(putUrlTemplate)
                        .file(mpf).with(csrf())
        )
                .andExpect(status().isOk())
                .andReturn()
                ;
        AbstractImageUploadController.ImageResponse imageResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), AbstractImageUploadController.ImageResponse.class);
        String urlResponse = imageResponse.getUrl();

        LOGGER.info("responsed image url: {}", urlResponse);

        MvcResult result = mockMvc.perform(
                MockMvcRequestBuilders.get(urlResponse)
        )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mpf.getContentType()))
                .andReturn()
                ;
        byte[] content = result.getResponse().getContentAsByteArray();

        Assert.assertArrayEquals(mpf.getBytes(), content);

        return urlResponse;
    }

    @WithUserDetails(TestConstants.USER_NIKITA)
    @Test
    public void postImage() throws Exception {

        byte[] img0 = {(byte)0xFF, (byte)0x01, (byte)0x1A};
        MockMultipartFile mf0 = new MockMultipartFile(ImagePostTitleUploadController.IMAGE_PART, "lol-content.png", "image/png", img0);
        String url0 = postImage(postTemplate(), mf0);

        byte[] img1 = {(byte)0xAA, (byte)0xBB, (byte)0xCC, (byte)0xDD, (byte)0xCC};
        MockMultipartFile mf1 = new MockMultipartFile(ImagePostTitleUploadController.IMAGE_PART, "lol-content.png", "image/png", img1);
        String url1 = postImage(postTemplate(), mf1);

        Assert.assertNotEquals(url0, url1);
    }


    @Test
    public void putImageUnauthorized() throws Exception {
        byte[] img0 = {(byte)0xFF, (byte)0x01, (byte)0x1A};
        MockMultipartFile mf0 = new MockMultipartFile(ImagePostTitleUploadController.IMAGE_PART, "lol-content.png", "image/png", img0);
        MvcResult mvcResult = mockMvc.perform(
                MockMvcRequestBuilders.fileUpload(postTemplate())
                        .file(mf0).with(csrf())
        )
                .andExpect(status().isUnauthorized())
                .andReturn()
                ;
    }

}