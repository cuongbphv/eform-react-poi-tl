package com.ceent.eform.service;

import com.ceent.eform.dto.*;
import com.ceent.eform.dto.request.FormDataRequest;
import com.ceent.eform.dto.request.GeneratePdfRequest;
import com.ceent.eform.entity.Form;
import com.ceent.eform.entity.Template;
import com.ceent.eform.repository.FormRepository;
import com.ceent.eform.repository.TemplateRepository;
import com.deepoove.poi.XWPFTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter;
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class EFormService {

    private final TemplateRepository templateRepository;
    private final FormRepository formRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.output.dir:outputs}")
    private String outputDir;

    @Transactional
    public TemplateDto uploadTemplate(MultipartFile file, String templateName) throws Exception {
        // Tạo thư mục upload nếu chưa tồn tại
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Lưu file
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        String uniqueFilename = System.currentTimeMillis() + "_" + filename;
        Path targetPath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Phân tích biến trong template
        List<String> variables = extractVariablesFromTemplate(targetPath.toString());

        // Lưu thông tin template vào database
        Template template = new Template();
        template.setName(templateName);
        template.setFilename(filename);
        template.setFilePath(targetPath.toString());
        template.setVariables(objectMapper.writeValueAsString(variables));

        template = templateRepository.save(template);

        // Chuyển đổi sang DTO
        TemplateDto dto = new TemplateDto();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setFilename(template.getFilename());
        dto.setVariables(variables);
        dto.setCreatedAt(template.getCreatedAt());
        dto.setUpdatedAt(template.getUpdatedAt());

        return dto;
    }

    public List<TemplateDto> getAllTemplates() {
        List<Template> templates = templateRepository.findAll();
        return templates.stream().map(this::convertToDto).toList();
    }

    public TemplateDto getTemplate(Long id) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        return convertToDto(template);
    }

    @Transactional
    public FormDto saveForm(FormDataRequest request) throws Exception {
        Template template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new RuntimeException("Template not found"));

        Form form = new Form();
        form.setTemplate(template);
        form.setName(request.getName());
        form.setFormData(objectMapper.writeValueAsString(request.getData()));

        form = formRepository.save(form);

        return convertToFormDto(form);
    }

    public List<FormDto> getAllForms() {
        List<Form> forms = formRepository.findAll();
        return forms.stream().map(this::convertToFormDto).toList();
    }

    public FormDto getForm(Long id) {
        Form form = formRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Form not found"));
        return convertToFormDto(form);
    }

    public byte[] generatePdf(GeneratePdfRequest request) throws Exception {
        Form form = formRepository.findById(request.getFormId())
                .orElseThrow(() -> new RuntimeException("Form not found"));

        // Tạo thư mục output nếu chưa tồn tại
        Path outputPath = Paths.get(outputDir);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }

        // Lấy dữ liệu từ request hoặc form
        Map<String, Object> data = request.getData() != null ?
                request.getData() :
                objectMapper.readValue(form.getFormData(), new TypeReference<Map<String, Object>>() {});

        // Tạo file Word từ template
        XWPFTemplate template = XWPFTemplate.compile(form.getTemplate().getFilePath())
                .render(data);

        // Tạo file tạm thời cho Word
        String tempWordFile = outputPath.resolve("temp_" + System.currentTimeMillis() + ".docx").toString();
        FileOutputStream wordOut = new FileOutputStream(tempWordFile);
        template.write(wordOut);
        wordOut.close();
        template.close();

        // Chuyển đổi Word sang PDF
        ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
        try (FileInputStream wordIn = new FileInputStream(tempWordFile)) {
            XWPFDocument document = new XWPFDocument(wordIn);
            PdfOptions options = PdfOptions.create();
            PdfConverter.getInstance().convert(document, pdfOut, options);
            document.close();
        }

        // Xóa file tạm thời
        Files.deleteIfExists(Paths.get(tempWordFile));

        return pdfOut.toByteArray();
    }

    private List<String> extractVariablesFromTemplate(String filePath) throws Exception {
        Set<String> variables = new HashSet<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            // Tìm biến trong paragraphs
            document.getParagraphs().forEach(paragraph -> {
                String text = paragraph.getText();
                if (text != null) {
                    variables.addAll(findVariables(text));
                }
            });

            // Tìm biến trong tables
            document.getTables().forEach(table -> {
                table.getRows().forEach(row -> {
                    row.getTableCells().forEach(cell -> {
                        String text = cell.getText();
                        if (text != null) {
                            variables.addAll(findVariables(text));
                        }
                    });
                });
            });
        }

        return new ArrayList<>(variables);
    }

    private Set<String> findVariables(String text) {
        Set<String> variables = new HashSet<>();
        Pattern pattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            variables.add(matcher.group(1).trim());
        }

        return variables;
    }

    private TemplateDto convertToDto(Template template) {
        try {
            List<String> variables = objectMapper.readValue(template.getVariables(),
                    new TypeReference<List<String>>() {});

            return new TemplateDto(
                    template.getId(),
                    template.getName(),
                    template.getFilename(),
                    variables,
                    template.getCreatedAt(),
                    template.getUpdatedAt()
            );
        } catch (Exception e) {
            log.error("Error converting template to DTO", e);
            return new TemplateDto(
                    template.getId(),
                    template.getName(),
                    template.getFilename(),
                    new ArrayList<>(),
                    template.getCreatedAt(),
                    template.getUpdatedAt()
            );
        }
    }

    private FormDto convertToFormDto(Form form) {
        try {
            Map<String, Object> formData = objectMapper.readValue(form.getFormData(),
                    new TypeReference<Map<String, Object>>() {});

            return new FormDto(
                    form.getId(),
                    form.getTemplate().getId(),
                    form.getTemplate().getName(),
                    form.getName(),
                    formData,
                    form.getCreatedAt(),
                    form.getUpdatedAt()
            );
        } catch (Exception e) {
            log.error("Error converting form to DTO", e);
            return new FormDto(
                    form.getId(),
                    form.getTemplate().getId(),
                    form.getTemplate().getName(),
                    form.getName(),
                    new HashMap<>(),
                    form.getCreatedAt(),
                    form.getUpdatedAt()
            );
        }
    }
}
