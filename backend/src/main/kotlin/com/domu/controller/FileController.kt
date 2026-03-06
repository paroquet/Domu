package com.domu.controller

import com.domu.config.AppProperties
import com.domu.dto.FileUploadResponse
import com.domu.service.FileService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/files")
class FileController(
    private val fileService: FileService,
    private val appProperties: AppProperties
) {

    @PostMapping("/upload")
    fun uploadFile(@RequestParam("file") file: MultipartFile): ResponseEntity<FileUploadResponse> {
        val response = fileService.upload(file, appProperties.baseUrl)
        return ResponseEntity.ok(response)
    }
}
