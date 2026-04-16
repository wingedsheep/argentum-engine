package com.wingedsheep.gym.server.controller

import com.wingedsheep.gym.contract.SchemaHash
import com.wingedsheep.gym.server.dto.HealthResponse
import com.wingedsheep.gym.server.dto.SchemaHashResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Meta endpoints for Python clients:
 *  - `GET /schema-hash` — compared by clients at startup to fail fast on
 *    contract drift (server regenerated types but client didn't pull).
 *  - `GET /health` — lightweight liveness probe for container orchestration.
 */
@RestController
@Tag(name = "Meta", description = "Schema version + liveness — hit these at client startup.")
class MetaController {

    @Operation(
        summary = "Observation schema version",
        description = "Python clients should compare this to their generated version at startup and abort on mismatch."
    )
    @GetMapping("/schema-hash")
    fun schemaHash(): SchemaHashResponse = SchemaHashResponse(SchemaHash.CURRENT)

    @Operation(summary = "Liveness probe")
    @GetMapping("/health")
    fun health(): HealthResponse = HealthResponse()
}
