package ru.yojo.codegen.web.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;
import reactor.core.publisher.Mono;
import ru.yojo.codegen.context.ProcessContext;
import ru.yojo.codegen.domain.lombok.Accessors;
import ru.yojo.codegen.domain.lombok.LombokProperties;
import ru.yojo.codegen.domain.message.Message;
import ru.yojo.codegen.domain.schema.Schema;
import ru.yojo.codegen.mapper.MessageMapper;
import ru.yojo.codegen.mapper.SchemaMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static ru.yojo.codegen.util.MapperUtil.castObjectToMap;

@RestController
@Component
public class YamlController {

    public YamlController(SchemaMapper schemaMapper, MessageMapper messageMapper) {
        this.schemaMapper = schemaMapper;
        this.messageMapper = messageMapper;
    }

    private final SchemaMapper schemaMapper;
    private final MessageMapper messageMapper;

    @PostMapping("/download")
    public Mono<ResponseEntity<byte[]>> downloadZip(@RequestBody Map<String, String> request) {
        return Mono.fromCallable(() -> {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
                ProcessContext processContext = new ProcessContext(new Yaml().load(request.get("yaml")));
                processContext.setPackageLocation("ru.yojo.codegen");
                processContext.setOutputDirectory("generated");
                processContext.setOutputDirectoryName("generated");
                processContext.setLombokProperties(new LombokProperties(true, true, new Accessors(false, false, false)));
                processContext.setSpringBootVersion("3");
                processContext.preparePackages();

                Map<String, Object> messagesMap =
                        castObjectToMap(
                                castObjectToMap(
                                        castObjectToMap(processContext.getContent().get("components"))).get("messages"));

                Map<String, Object> schemasMap =
                        castObjectToMap(
                                castObjectToMap(
                                        castObjectToMap(processContext.getContent().get("components"))).get("schemas"));

                processContext.setMessagesMap(messagesMap);
                processContext.setSchemasMap(schemasMap);

                List<Message> messages = messageMapper.mapMessagesToObjects(processContext);
                List<Schema> schemas = schemaMapper.mapSchemasToObjects(processContext);

                messages.forEach(element -> {
                    ZipEntry zipEntry = new ZipEntry("ru/yojo/codegen/generated/messages/" + (element).getMessageName() + ".java");
                    try {
                        zipOutputStream.putNextEntry(zipEntry);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        zipOutputStream.write((element).toWrite().getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        zipOutputStream.closeEntry();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                schemas.forEach(element -> {
                    ZipEntry zipEntry = new ZipEntry("ru/yojo/codegen/generated/schemas/" + (element).getSchemaName() + ".java");
                    try {
                        zipOutputStream.putNextEntry(zipEntry);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        zipOutputStream.write((element).toWrite().getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        zipOutputStream.closeEntry();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                try {
                    zipOutputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            byte[] zipBytes = byteArrayOutputStream.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "classes.zip");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(zipBytes);
        });
    }
}
