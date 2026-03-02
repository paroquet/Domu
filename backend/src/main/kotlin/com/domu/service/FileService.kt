package com.domu.service

import com.domu.config.AppProperties
import com.domu.dto.FileUploadResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

@Service
class FileService(private val appProperties: AppProperties) {

    fun upload(file: MultipartFile, baseUrl: String): FileUploadResponse {
        val contentType = file.contentType
        if (contentType == null || !contentType.startsWith("image/")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed")
        }

        val originalFilename = file.originalFilename ?: "upload"
        val extension = originalFilename.substringAfterLast('.', "").let {
            if (it.isNotBlank()) ".$it" else ""
        }
        val filename = "${UUID.randomUUID()}$extension"

        val uploadPath = Paths.get(appProperties.uploadDir)
        Files.createDirectories(uploadPath)

        val filePath = uploadPath.resolve(filename)
        file.transferTo(filePath.toFile())

        val relativePath = "/files/$filename"
        val url = "$baseUrl$relativePath"

        return FileUploadResponse(path = relativePath, url = url)
    }
}
