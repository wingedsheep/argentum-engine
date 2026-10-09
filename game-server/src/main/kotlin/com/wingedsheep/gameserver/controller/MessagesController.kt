package com.wingedsheep.gameserver.controller

import com.fasterxml.jackson.annotation.JsonProperty
import com.wingedsheep.gameserver.auth.AuthSupport
import com.wingedsheep.gameserver.messages.DirectMessageService
import com.wingedsheep.gameserver.messages.DirectMessageService.SendResult
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

/**
 * Direct messages for the signed-in account (see [DirectMessageService] for the rules). A
 * conversation is addressed by the other person's account id; the caller is always the Bearer
 * token's account. Only mounted when accounts are enabled.
 */
@RestController
@RequestMapping("/api/messages")
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class MessagesController(
    private val messages: DirectMessageService,
    private val authSupport: AuthSupport,
) {
    data class ParticipantDto(val accountId: String, val displayName: String, val online: Boolean, val avatar: String?)
    data class MessageDto(val id: String, val senderId: String, val body: String, val createdAt: String)
    data class ThreadDto(
        val other: ParticipantDto,
        val state: String,
        // Pinned: Jackson would otherwise strip the `is` prefix and send `friend`.
        @get:JsonProperty("isFriend") val isFriend: Boolean,
        val lastMessage: MessageDto,
        val unread: Int,
    )
    data class ConversationDto(
        val other: ParticipantDto,
        val state: String,
        @get:JsonProperty("isFriend") val isFriend: Boolean,
        val messages: List<MessageDto>,
        val hasMore: Boolean,
        val requestMessagesLeft: Int?,
        val maxLength: Int,
    )
    data class SendBody(val body: String)

    @GetMapping
    fun threads(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?): List<ThreadDto> {
        val me = authSupport.requireUser(auth).userId
        return messages.listThreads(me).map {
            ThreadDto(it.other.toDto(), it.state.name, it.isFriend, it.lastMessage.toDto(), it.unread)
        }
    }

    @GetMapping("/{accountId}")
    fun conversation(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @PathVariable accountId: UUID,
        @RequestParam(required = false) before: String?,
    ): ResponseEntity<Any> {
        val me = authSupport.requireUser(auth).userId
        val beforeAt = before?.let { runCatching { Instant.parse(it) }.getOrNull() }
        val c = messages.conversation(me, accountId, beforeAt)
            ?: return ResponseEntity.status(404).body(error(CANNOT_MESSAGE))
        return ResponseEntity.ok(
            ConversationDto(
                other = c.other.toDto(),
                state = c.state.name,
                isFriend = c.isFriend,
                messages = c.messages.map { it.toDto() },
                hasMore = c.hasMore,
                requestMessagesLeft = c.requestMessagesLeft,
                maxLength = DirectMessageService.MAX_BODY_LENGTH,
            ),
        )
    }

    @PostMapping("/{accountId}")
    fun send(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @PathVariable accountId: UUID,
        @RequestBody body: SendBody,
    ): ResponseEntity<Any> {
        val me = authSupport.requireUser(auth).userId
        return when (val result = messages.send(me, accountId, body.body)) {
            is SendResult.Sent -> ResponseEntity.ok(result.message.toDto())
            SendResult.EmptyBody -> ResponseEntity.badRequest().body(error("Write something first."))
            SendResult.TooLong -> ResponseEntity.badRequest()
                .body(error("Messages can be at most ${DirectMessageService.MAX_BODY_LENGTH} characters."))
            SendResult.SelfMessage -> ResponseEntity.badRequest().body(error("You can't message yourself."))
            SendResult.CannotMessage -> ResponseEntity.status(404).body(error(CANNOT_MESSAGE))
            SendResult.RateLimited -> ResponseEntity.status(429)
                .body(error("You're sending messages too quickly — wait a moment."))
            SendResult.RequestLimitReached -> ResponseEntity.status(409)
                .body(error("Wait for them to accept your request before sending more."))
        }
    }

    @PostMapping("/{accountId}/accept")
    fun accept(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @PathVariable accountId: UUID,
    ): ResponseEntity<Any> {
        val me = authSupport.requireUser(auth).userId
        return if (messages.accept(me, accountId)) ResponseEntity.ok(ok())
        else ResponseEntity.status(404).body(error("No request to accept."))
    }

    @PostMapping("/{accountId}/read")
    fun read(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @PathVariable accountId: UUID,
    ): ResponseEntity<Any> {
        val me = authSupport.requireUser(auth).userId
        messages.markRead(me, accountId)
        return ResponseEntity.ok(ok())
    }

    @DeleteMapping("/{accountId}")
    fun clear(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @PathVariable accountId: UUID,
    ): ResponseEntity<Any> {
        val me = authSupport.requireUser(auth).userId
        return if (messages.clear(me, accountId)) ResponseEntity.ok(ok())
        else ResponseEntity.status(404).body(error("No conversation to delete."))
    }

    @PostMapping("/{accountId}/block")
    fun block(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @PathVariable accountId: UUID,
    ): ResponseEntity<Any> {
        val me = authSupport.requireUser(auth).userId
        return if (messages.block(me, accountId)) ResponseEntity.ok(ok())
        else ResponseEntity.status(404).body(error("No conversation with that player."))
    }

    private fun DirectMessageService.Participant.toDto() =
        ParticipantDto(accountId.toString(), displayName, online, avatar)

    private fun DirectMessageService.MessageView.toDto() =
        MessageDto(id.toString(), senderId.toString(), body, createdAt.toString())

    private fun error(message: String) = mapOf("error" to message)
    private fun ok() = mapOf("status" to "ok")

    private companion object {
        const val CANNOT_MESSAGE = "You can't message this player."
    }
}
