package ru.yojo.codegen.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yojo.codegen.mapper.Helper;
import ru.yojo.codegen.mapper.MessageMapper;
import ru.yojo.codegen.mapper.SchemaMapper;

@Configuration
public class BeanInitializer {

    @Bean
    public Helper helper() {
        return new Helper();
    }

    @Bean
    public SchemaMapper schemaMapper() {
        return new SchemaMapper(helper());
    }

    @Bean
    public MessageMapper messageMapper() {
        return new MessageMapper(helper(), schemaMapper());
    }
}
