package com.wingedsheep.gymserver.controller

import com.wingedsheep.engine.gym.contract.SchemaHash
import com.wingedsheep.gymserver.dto.HealthResponse
import com.wingedsheep.gymserver.dto.SchemaHashResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Meta endpoints for Python clients:
 *  - `GET /schema-hash` — compared by clients at startup to fail fast on
 *    contract drift (server regenerated types but client didn't pull).
 *  - `GET /health` — lightweight liveness probe for container orchestration.
 */
@RestController
class MetaController {

    @GetMapping("/schema-hash")
    fun schemaHash(): SchemaHashResponse = SchemaHashResponse(SchemaHash.CURRENT)

    @GetMapping("/health")
    fun health(): HealthResponse = HealthResponse()
}
