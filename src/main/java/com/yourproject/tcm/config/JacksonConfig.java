package com.yourproject.tcm.config;

import com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson Configuration for Hibernate lazy loading support
 * 
 * This configuration registers the Hibernate5JakartaModule with Jackson,
 * which enables proper serialization of Hibernate lazy-loaded entities
 * and prevents LazyInitializationException when serializing to JSON.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Hibernate5JakartaModule hibernate5Module() {
        Hibernate5JakartaModule module = new Hibernate5JakartaModule();
        
        // Configure the module to not force lazy loading
        module.configure(Hibernate5JakartaModule.Feature.FORCE_LAZY_LOADING, false);
        
        // Ignore properties that cannot be loaded (lazy associations)
        module.configure(Hibernate5JakartaModule.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, true);
        
        return module;
    }
}