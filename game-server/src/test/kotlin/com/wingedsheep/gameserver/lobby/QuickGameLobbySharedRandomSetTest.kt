package com.wingedsheep.gameserver.lobby

import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Random deck > Any set" seats share one set: [QuickGameLobby.pinnedRandomSetCode] is what every
 * such seat builds from when something is already pinned, and null tells the handler to roll once
 * for the whole table rather than once per seat.
 */
class QuickGameLobbySharedRandomSetTest : FunSpec({

    fun seat(n: Int, setCode: String? = null, isAi: Boolean = false) =
        QuickGameLobbyPlayer(EntityId.of("p$n"), "Player$n", isAi = isAi, deckList = emptyMap(), setCode = setCode)

    test("two humans on Any set pin nothing, so the table rolls one shared set") {
        val lobby = QuickGameLobby(vsAi = false, setCode = null)
        lobby.players += seat(1)
        lobby.players += seat(2)
        lobby.pinnedRandomSetCode() shouldBe null
    }

    test("a human on Any set follows the set their opponent pinned") {
        val lobby = QuickGameLobby(vsAi = false, setCode = null)
        lobby.players += seat(1)
        lobby.players += seat(2, setCode = "DOM")
        lobby.pinnedRandomSetCode() shouldBe "DOM"
    }

    test("the lobby's legacy set wins over a seat's pin") {
        val lobby = QuickGameLobby(vsAi = false, setCode = "M19")
        lobby.players += seat(1, setCode = "DOM")
        lobby.pinnedRandomSetCode() shouldBe "M19"
    }

    test("an AI seat's set is never what the humans follow") {
        val lobby = QuickGameLobby(vsAi = true, setCode = null)
        lobby.players += seat(1)
        lobby.players += seat(2, setCode = "DOM", isAi = true)
        lobby.pinnedRandomSetCode() shouldBe null
    }
})
