package com.spring.aws.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.spring.aws.repo.entity.CustomerImage;

/**
 * 
 * @author linhpham
 *
 */
@Service
public class DefaultS3FileService implements S3FileService {
	
	@Autowired
	private AmazonS3Client s3Client;
	
	@Value("${aws.s3.bucket-name}")
	private String S3_BUCKET_NAME;
	
	/**
	 * Save image to S3 and return CustomerImage containing key and public URL
	 * 
	 * @param multipartFile
	 * @return
	 * @throws IOException
	 */
	public CustomerImage saveFileToS3(MultipartFile multipartFile) throws Exception {

		File fileToUpload = convertFromMultiPart(multipartFile);
		String key = Instant.now().getEpochSecond() + "_" + fileToUpload.getName();

		/* save file */
		s3Client.putObject(new PutObjectRequest(S3_BUCKET_NAME, key, fileToUpload));

		/* get signed URL (valid for one year) */
		GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(S3_BUCKET_NAME, key);
		generatePresignedUrlRequest.setMethod(HttpMethod.GET);
		generatePresignedUrlRequest.setExpiration(DateTime.now().plusDays(7).toDate());

		URL signedUrl = s3Client.generatePresignedUrl(generatePresignedUrlRequest); 

		return new CustomerImage(key, signedUrl.toString());
	}

	/**
	 * Delete image from S3 using specified key
	 * 
	 * @param customerImage
	 */
	@Override
	public void deleteImageFromS3(String imageKey) {
		s3Client.deleteObject(new DeleteObjectRequest(S3_BUCKET_NAME, imageKey));	
	}

	/**
	 * Convert MultiPartFile to ordinary File
	 * 
	 * @param multipartFile
	 * @return
	 * @throws IOException
	 */
	private File convertFromMultiPart(MultipartFile multipartFile) throws IOException {

		File file = new File(multipartFile.getOriginalFilename());
		file.createNewFile(); 
		FileOutputStream fos = new FileOutputStream(file); 
		fos.write(multipartFile.getBytes());
		fos.close(); 

		return file;
	}

}
