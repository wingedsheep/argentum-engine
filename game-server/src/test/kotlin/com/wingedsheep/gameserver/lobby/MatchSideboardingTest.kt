package com.wingedsheep.gameserver.lobby

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gameserver.persistence.restoreTournamentLobby
import com.wingedsheep.gameserver.persistence.toPersistent
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.sdk.core.GameRules
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Sideboarding between the games of a best-of-three (CR 100.4): in Limited the whole unplayed pool
 * is the sideboard (CR 100.4b) and basic lands are free; a premade deck may only trade with the
 * sideboard it registered.
 */
class MatchSideboardingTest : FunSpec({
    val host = EntityId("host")

    fun bear(n: Int) = CardDefinition.creature(
        name = "Bear $n",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAR),
        power = 2,
        toughness = 2,
    )

    val registered = (1..23).associate { "Bear $it" to 1 } + ("Forest" to 17)

    fun sealedLobby(): TournamentLobby {
        val lobby = TournamentLobby(
            setCodes = listOf("TST"),
            setNames = listOf("Test"),
            boosterGenerator = BoosterGenerator(emptyMap()),
            format = TournamentFormat.SEALED,
        )
        lobby.addPlayer(PlayerIdentity(playerId = host, playerName = "Host"))
        lobby.players[host] = lobby.players.getValue(host).copy(cardPool = (1..30).map(::bear))
        lobby.restoreFromPersistence(LobbyState.DECK_BUILDING, hostPlayerId = host)
        lobby.submitDeck(host, registered)
        return lobby
    }

    test("a Limited deck may swap in any card from the unplayed pool") {
        val lobby = sealedLobby()
        val sideboarded = registered - "Bear 1" + ("Bear 30" to 1)

        lobby.matchDeckError(host, sideboarded).shouldBeNull()
        lobby.sideboardFor(host, sideboarded)["Bear 1"] shouldBe 1
        lobby.sideboardFor(host, sideboarded)["Bear 30"] shouldBe null
    }

    test("basic lands are free, so a land count can change without a sideboard card") {
        sealedLobby().matchDeckError(host, registered + ("Forest" to 18) + ("Island" to 2)).shouldBeNull()
    }

    test("a card from outside the pool is refused") {
        sealedLobby().matchDeckError(host, registered + ("Bear 99" to 1))!! shouldContain "not in pool"
    }

    test("the deck still needs forty cards") {
        sealedLobby().matchDeckError(host, registered - "Bear 1" - "Bear 2")!! shouldContain "at least 40"
    }

    test("an unchanged deck keeps the registered sideboard") {
        val lobby = sealedLobby()
        lobby.sideboardFor(host, registered) shouldBe lobby.getSubmittedSideboard(host)
    }

    test("Commander and Jumpstart decks stay as registered") {
        val lobby = sealedLobby()
        lobby.rules = GameRules.COMMANDER
        lobby.allowsSideboarding shouldBe false
        lobby.matchDeckError(host, registered)!! shouldContain "can't be changed"
    }

    test("a premade deck trades only with its registered sideboard") {
        val lobby = TournamentLobby(
            setCodes = listOf("TST"),
            setNames = listOf("Test"),
            boosterGenerator = BoosterGenerator(emptyMap()),
            format = TournamentFormat.PREMADE_DECKS,
        )
        lobby.addPlayer(PlayerIdentity(playerId = host, playerName = "Host"))
        val deck = (1..10).associate { "Bear $it" to 4 } + ("Forest" to 20)
        lobby.submitDeck(host, deck, sideboard = mapOf("Bear 11" to 3))

        val swapped = deck + ("Bear 1" to 2) + ("Bear 11" to 2)
        lobby.matchDeckError(host, swapped).shouldBeNull()
        lobby.sideboardFor(host, swapped) shouldBe mapOf("Bear 1" to 2, "Bear 11" to 1)

        lobby.matchDeckError(host, deck + ("Bear 11" to 4))!! shouldContain "Only 3 copies of Bear 11"
        lobby.matchDeckError(host, deck + ("Bear 12" to 1))!! shouldContain "isn't in your deck or sideboard"
    }

    test("best-of survives persistence") {
        val lobby = sealedLobby()
        lobby.bestOf = 3
        val (restored, _) = restoreTournamentLobby(lobby.toPersistent(), CardRegistry(), BoosterGenerator(emptyMap()))
        restored.bestOf shouldBe 3
    }
})
