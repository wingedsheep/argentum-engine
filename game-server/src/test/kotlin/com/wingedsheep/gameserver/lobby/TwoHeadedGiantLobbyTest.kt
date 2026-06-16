package com.wingedsheep.gameserver.lobby

import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Two-Headed Giant — Phase 6 lobby plumbing. A [QuickGameLobby] flagged [QuickGameLobby.twoHeadedGiant]
 * is a four-seat, two-team staging area; a normal lobby stays two-seat. This pins the seat-count and
 * team-partition math the handler relies on to thread `GameSession.teams` and the 2HG format.
 */
class TwoHeadedGiantLobbyTest : FunSpec({

    fun lobby(twoHeadedGiant: Boolean) =
        QuickGameLobby(vsAi = false, setCode = null, twoHeadedGiant = twoHeadedGiant)

    fun seat(n: Int) = QuickGameLobbyPlayer(EntityId.of("p$n"), "Player$n", ready = true)

    test("a 2HG lobby seats four players in two teams of two") {
        val l = lobby(twoHeadedGiant = true)
        l.maxPlayers shouldBe 4
        l.teamAssignment() shouldBe listOf(listOf(0, 1), listOf(2, 3))
        l.teamIndexOf(0) shouldBe 0
        l.teamIndexOf(1) shouldBe 0
        l.teamIndexOf(2) shouldBe 1
        l.teamIndexOf(3) shouldBe 1
    }

    test("a 2HG lobby is full and ready only with all four seats filled and ready") {
        val l = lobby(twoHeadedGiant = true)
        l.players += seat(1)
        l.players += seat(2)
        l.isFull shouldBe false
        l.allReady() shouldBe false

        l.players += seat(3)
        l.players += seat(4)
        l.isFull shouldBe true
        l.allReady() shouldBe true

        // One seat not ready blocks the start.
        l.players[3] = l.players[3].copy(ready = false)
        l.allReady() shouldBe false
    }

    test("a normal lobby is unchanged: two seats, no teams") {
        val l = lobby(twoHeadedGiant = false)
        l.maxPlayers shouldBe 2
        l.teamAssignment() shouldBe null
        l.teamIndexOf(0) shouldBe null

        l.players += seat(1)
        l.isFull shouldBe false
        l.players += seat(2)
        l.isFull shouldBe true
        l.allReady() shouldBe true
    }
})
