package com.stream.controller;

import java.io.File;
import java.io.IOException;

import static java.lang.Math.min;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.apache.commons.lang3.StringUtils;

@RestController
public class VideoStreamController {

	private static final long CHUNK_SIZE = 1000000L;
	public static final String VideoUploadingDir = System.getProperty("user.dir") + "/Uploads/Posts/Videos";

	@GetMapping(value = "/video", produces = "application/octet-stream")
	public ResponseEntity<ResourceRegion> getVideo(@RequestHeader(value = "Range", required = false) String rangeHeader)
			throws IOException {

		if (!new File(VideoUploadingDir).exists()) {
			new File(VideoUploadingDir).mkdirs();
		}
		return getVideoRegion(rangeHeader);

	}

	public ResponseEntity<ResourceRegion> getVideoRegion(String rangeHeader) throws IOException {
		FileUrlResource videoResource = new FileUrlResource(VideoUploadingDir + "/video.mp4");
		ResourceRegion resourceRegion = getResourceRegion(videoResource, rangeHeader);

		return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
				.contentType(MediaTypeFactory.getMediaType(videoResource).orElse(MediaType.APPLICATION_OCTET_STREAM))
				.body(resourceRegion);
	}

	private ResourceRegion getResourceRegion(UrlResource video, String httpHeaders) throws IOException {
		ResourceRegion resourceRegion = null;

		long contentLength = video.contentLength();
		int fromRange = 0;
		int toRange = 0;
		if (StringUtils.isNotBlank(httpHeaders)) {
			String[] ranges = httpHeaders.substring("bytes=".length()).split("-");
			fromRange = Integer.valueOf(ranges[0]);
			if (ranges.length > 1) {
				toRange = Integer.valueOf(ranges[1]);
			} else {
				toRange = (int) (contentLength - 1);
			}
		}

		if (fromRange > 0) {
			long rangeLength = min(CHUNK_SIZE, toRange - fromRange + 1);
			resourceRegion = new ResourceRegion(video, fromRange, rangeLength);
		} else {
			long rangeLength = min(CHUNK_SIZE, contentLength);
			resourceRegion = new ResourceRegion(video, 0, rangeLength);
		}

		return resourceRegion;
	}

}
