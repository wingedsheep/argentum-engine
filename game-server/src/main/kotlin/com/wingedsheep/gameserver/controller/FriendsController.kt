package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.auth.AuthSupport
import com.wingedsheep.gameserver.friends.FriendsService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Friends and friend requests for the signed-in account. The authenticated user is always taken from
 * the Bearer token (never the request body), so you can only act on your own relationships. Adding a
 * friend takes the other person's account id — their shareable "friend code" — so no email is exposed.
 *
 * Only mounted when accounts are enabled.
 */
@RestController
@RequestMapping("/api/friends")
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class FriendsController(
    private val friends: FriendsService,
    private val authSupport: AuthSupport,
) {
    data class FriendDto(val accountId: String, val displayName: String, val online: Boolean)
    data class RequestDto(
        val requestId: String,
        val accountId: String,
        val displayName: String,
        val createdAt: String,
    )
    data class RequestsDto(val incoming: List<RequestDto>, val outgoing: List<RequestDto>)
    data class AddFriendBody(val accountId: String)
    data class VisibilityBody(val hidden: Boolean)

    @GetMapping
    fun list(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?): List<FriendDto> {
        val userId = authSupport.requireUser(auth).userId
        return friends.listFriends(userId).map { FriendDto(it.accountId.toString(), it.displayName, it.online) }
    }

    @GetMapping("/requests")
    fun requests(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?): RequestsDto {
        val userId = authSupport.requireUser(auth).userId
        val r = friends.listRequests(userId)
        return RequestsDto(r.incoming.map { it.toDto() }, r.outgoing.map { it.toDto() })
    }

    @PostMapping("/requests")
    fun sendRequest(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @RequestBody body: AddFriendBody,
    ): ResponseEntity<Any> {
        val userId = authSupport.requireUser(auth).userId
        val targetId = runCatching { UUID.fromString(body.accountId.trim()) }.getOrNull()
            ?: return ResponseEntity.badRequest().body(error("That doesn't look like a valid friend code."))
        return when (val result = friends.sendRequest(userId, targetId)) {
            is FriendsService.SendResult.Created -> ResponseEntity.ok(result.request.toDto())
            FriendsService.SendResult.SelfRequest ->
                ResponseEntity.badRequest().body(error("You can't add yourself as a friend."))
            FriendsService.SendResult.UnknownUser ->
                ResponseEntity.status(404).body(error("No account with that friend code."))
            FriendsService.SendResult.AlreadyFriends ->
                ResponseEntity.status(409).body(error("You're already friends."))
            FriendsService.SendResult.AlreadyRequested ->
                ResponseEntity.status(409).body(error("You've already sent them a request."))
            is FriendsService.SendResult.IncomingPending ->
                ResponseEntity.status(409).body(error("They've already sent you a request — accept it instead."))
        }
    }

    @PostMapping("/requests/{id}/accept")
    fun accept(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @PathVariable id: UUID,
    ): ResponseEntity<Any> {
        val userId = authSupport.requireUser(auth).userId
        return if (friends.accept(userId, id)) ResponseEntity.ok(ok())
        else ResponseEntity.status(404).body(error("Request not found."))
    }

    @DeleteMapping("/requests/{id}")
    fun removeRequest(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @PathVariable id: UUID,
    ): ResponseEntity<Any> {
        val userId = authSupport.requireUser(auth).userId
        return if (friends.removeRequest(userId, id)) ResponseEntity.ok(ok())
        else ResponseEntity.status(404).body(error("Request not found."))
    }

    @DeleteMapping("/{accountId}")
    fun unfriend(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @PathVariable accountId: UUID,
    ): ResponseEntity<Any> {
        val userId = authSupport.requireUser(auth).userId
        return if (friends.unfriend(userId, accountId)) ResponseEntity.ok(ok())
        else ResponseEntity.status(404).body(error("Not friends."))
    }

    @PutMapping("/visibility")
    fun setVisibility(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @RequestBody body: VisibilityBody,
    ): ResponseEntity<Any> {
        val userId = authSupport.requireUser(auth).userId
        friends.setHidePresence(userId, body.hidden)
        return ResponseEntity.ok(mapOf("hidden" to body.hidden))
    }

    private fun FriendsService.RequestView.toDto() =
        RequestDto(requestId.toString(), accountId.toString(), displayName, createdAt.toString())

    private fun error(message: String) = mapOf("error" to message)
    private fun ok() = mapOf("status" to "ok")
}
