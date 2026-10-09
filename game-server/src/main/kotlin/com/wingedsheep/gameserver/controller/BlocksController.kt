package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.auth.AuthSupport
import com.wingedsheep.gameserver.social.BlockService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * The signed-in account's blocked players, for the friends panel's "Blocked" list. Blocking itself
 * happens from the result screen over the socket ([com.wingedsheep.gameserver.social.PostGameService]),
 * where the server already knows who the opponent was — so no endpoint takes an arbitrary account id
 * to block. Only mounted when accounts are enabled.
 */
@RestController
@RequestMapping("/api/blocks")
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class BlocksController(
    private val blocks: BlockService,
    private val authSupport: AuthSupport,
) {
    data class BlockedDto(val accountId: String, val displayName: String, val blockedAt: String, val avatar: String?)

    @GetMapping
    fun list(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?): List<BlockedDto> {
        val userId = authSupport.requireUser(auth).userId
        return blocks.listAccountBlocks(userId).map {
            BlockedDto(it.accountId.toString(), it.displayName, it.blockedAt.toString(), it.avatar)
        }
    }

    @DeleteMapping("/{accountId}")
    fun unblock(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
        @PathVariable accountId: UUID,
    ): ResponseEntity<Any> {
        val userId = authSupport.requireUser(auth).userId
        return if (blocks.unblockAccount(userId, accountId)) ResponseEntity.ok(mapOf("status" to "ok"))
        else ResponseEntity.status(404).body(mapOf("error" to "Not blocked."))
    }
}
