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
        // 使文件上传测试环境和先上环境都生效
        // /app/data/uploads 本身已经是绝对路径，toAbsolutePath() 对绝对路径是空操作，直接原样返回。
        // 只有相对路径（如 ./data/uploads）才会被转换成基于进程工作目录的绝对路径。
        val uploadPath = Paths.get(appProperties.uploadDir).toAbsolutePath()
        Files.createDirectories(uploadPath)

        val filePath = uploadPath.resolve(filename)
        Files.copy(file.inputStream, filePath)

        val relativePath = "/files/$filename"
        val url = "$baseUrl$relativePath"

        return FileUploadResponse(path = relativePath, url = url)
    }
}
