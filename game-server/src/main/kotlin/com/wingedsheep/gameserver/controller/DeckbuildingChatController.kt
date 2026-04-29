package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.deckbuilding.DeckbuildingChatService
import com.wingedsheep.gameserver.deckbuilding.DeckbuildingChatService.ChatRequest
import com.wingedsheep.gameserver.deckbuilding.DeckbuildingChatService.ChatResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/deckbuilding")
class DeckbuildingChatController(
    private val service: DeckbuildingChatService
) {

    @PostMapping("/chat")
    fun chat(@RequestBody request: ChatRequest): ResponseEntity<ChatResponse> {
        if (!service.isAvailable()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                ChatResponse(
                    assistantMessage = "Deckbuilder chat is not configured on the server.",
                    actions = emptyList(),
                    error = "llm_not_configured"
                )
            )
        }
        val response = service.respond(request)
        return ResponseEntity.ok(response)
    }
}
