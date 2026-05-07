package org.example.qasystem.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.example.qasystem.domain.File;
import org.example.qasystem.service.FileService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService){
        this.fileService = fileService;
    };

    @PostMapping("/upload")
    public File uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        return fileService.storeFile(file);
    }



    @GetMapping
    public List<File> getAll() {
        return fileService.getAllFiles();
    }

    @GetMapping("/{id:\\d+}")
    public File getById(@PathVariable Long id) {
        return fileService.findFileById(id);
    }

    @DeleteMapping("/{id:\\d+}")
    public void deleteById(@PathVariable Long id) {
        fileService.deleteFile(id);
    }

    @PutMapping("/{id:\\d+}")
    public File updateById(@PathVariable Long id, @RequestBody File file) {
        return fileService.updateFile(id, file);
    }

    @GetMapping("/category")
    public List<String> getCategory() {
        return fileService.getCategory();
    }

    @GetMapping("/page")
    public Page<File> getFilesByPage(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     @RequestParam(defaultValue = "0") int type,
                                     @RequestParam(required = false) String value) {
        return fileService.searchFilesByPage(type, value, page, size);
    }

    @GetMapping("/findone")
    public List<TypeOption> findOne() {
        // 返回所有 type 选项，用于填充第一个下拉框
        List<TypeOption> typeOptions = new ArrayList<>();
        typeOptions.add(new TypeOption(0, "无条件查询"));
        typeOptions.add(new TypeOption(1, "按ID查询"));
        typeOptions.add(new TypeOption(2, "按分类查询"));
        typeOptions.add(new TypeOption(3, "按标题查询"));
        return typeOptions;
    }

    @GetMapping("/findtwo")
    public Object findTwo(@RequestParam int type) {
        return switch (type) {
            case 1 -> fileService.getFileIds().stream()
                    .map(fileId -> new IdOption(fileId, String.valueOf(fileId)))
                    .collect(java.util.stream.Collectors.toList());
            case 2 -> fileService.getCategory().stream()
                    .map(category -> new CategoryOption(category, category))
                    .collect(java.util.stream.Collectors.toList());
            case 3 -> fileService.getTitles().stream()
                    .map(title -> new TitleOption(title, title))
                    .collect(java.util.stream.Collectors.toList());
            case 0 -> fileService.getAllFiles();
            default -> throw new IllegalArgumentException("Invalid type. Must be 0, 1, 2, or 3");
        };
    }

    // 辅助类，用于返回下拉框选项
    public static class TypeOption {
        private int value;
        private String label;

        public TypeOption(int value, String label) {
            this.value = value;
            this.label = label;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    public static class IdOption {
        private Long value;
        private String label;

        public IdOption(Long value, String label) {
            this.value = value;
            this.label = label;
        }

        public Long getValue() {
            return value;
        }

        public void setValue(Long value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    public static class CategoryOption {
        private String value;
        private String label;

        public CategoryOption(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    public static class TitleOption {
        private String value;
        private String label;

        public TitleOption(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

}
