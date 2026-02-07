package com.wingedsheep.gameserver.config

import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.PathResourceResolver

/**
 * SPA fallback configuration for serving the React frontend.
 *
 * In production, Spring serves static files from `classpath:/static/`.
 * For client-side routes like `/tournament/{id}`, the server needs to
 * serve `index.html` instead of returning 404. This config maps all
 * non-API, non-WebSocket paths to `index.html`.
 */
@Configuration
class SpaWebConfig : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(object : PathResourceResolver() {
                override fun getResource(resourcePath: String, location: Resource): Resource? {
                    val requested = location.createRelative(resourcePath)
                    return if (requested.exists() && requested.isReadable) {
                        requested
                    } else {
                        // For SPA routes, serve index.html
                        ClassPathResource("/static/index.html")
                    }
                }
            })
    }
}
