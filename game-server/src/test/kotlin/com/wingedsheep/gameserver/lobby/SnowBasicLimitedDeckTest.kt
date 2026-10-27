package com.wingedsheep.gameserver.lobby

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Basic snow lands are pack cards in Limited (a Kaldheim draft booster opens one): a deck may play
 * the copies its player opened, but unlike the five nonsnow basics they are not free.
 */
class SnowBasicLimitedDeckTest : FunSpec({
    val host = EntityId("host")

    fun bear(n: Int) = CardDefinition.creature(
        name = "Bear $n",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAR),
        power = 2,
        toughness = 2,
    )

    val snowForest = CardDefinition(
        name = "Snow-Covered Forest",
        manaCost = ManaCost.ZERO,
        typeLine = TypeLine.parse("Basic Snow Land — Forest"),
    )

    fun lobby(): TournamentLobby {
        val lobby = TournamentLobby(
            setCodes = listOf("TST"),
            setNames = listOf("Test"),
            boosterGenerator = BoosterGenerator(emptyMap()),
            format = TournamentFormat.SEALED,
        )
        lobby.addPlayer(PlayerIdentity(playerId = host, playerName = "Host"))
        lobby.players[host] = lobby.players.getValue(host).copy(
            cardPool = (1..23).map(::bear) + List(2) { snowForest },
        )
        lobby.restoreFromPersistence(LobbyState.DECK_BUILDING, hostPlayerId = host)
        return lobby
    }

    val spells = (1..23).associate { "Bear $it" to 1 }

    test("an opened snow basic is playable") {
        lobby().submitDeck(host, spells + ("Snow-Covered Forest" to 2) + ("Forest" to 15)) shouldBe
            TournamentLobby.DeckSubmissionResult.Success(allReady = true)
    }

    test("snow basics are bounded by the copies opened") {
        val result = lobby().submitDeck(host, spells + ("Snow-Covered Forest" to 3) + ("Forest" to 14))

        (result as TournamentLobby.DeckSubmissionResult.Error).message shouldContain
            "Not enough copies of Snow-Covered Forest in pool"
    }
})
