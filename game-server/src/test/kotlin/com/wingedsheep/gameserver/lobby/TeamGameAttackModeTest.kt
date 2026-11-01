package com.wingedsheep.gameserver.lobby

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.sdk.core.AttackMode
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Team games seat teammates next to each other, so attack left/right would point some players at
 * their own teammate and leave them nothing to attack (issue #4153). They always attack any opponent.
 */
class TeamGameAttackModeTest : FunSpec({

    val forest = CardDefinition.basicLand("Forest", Subtype.FOREST)
    val generator = BoosterGenerator(
        mapOf("TST" to BoosterGenerator.SetConfig("TST", "Test", emptyList(), listOf(forest)))
    )

    fun lobby(gameMode: LobbyGameMode) = TournamentLobby(
        setCodes = listOf("TST"),
        setNames = listOf("Test"),
        boosterGenerator = generator,
        format = TournamentFormat.PREMADE_DECKS,
        gameMode = gameMode,
    )

    test("a team game attacks any opponent, even when attack left was picked") {
        for (mode in listOf(LobbyGameMode.TWO_HEADED_GIANT, LobbyGameMode.TEAM_VS_TEAM)) {
            val lobby = lobby(mode)
            lobby.attackMode shouldBe AttackMode.MULTIPLE
            lobby.attackMode = AttackMode.LEFT
            lobby.attackMode shouldBe AttackMode.MULTIPLE
        }
    }

    test("a Free-for-All pod still defaults to attack left and keeps the host's pick") {
        val lobby = lobby(LobbyGameMode.FREE_FOR_ALL)
        lobby.attackMode shouldBe AttackMode.LEFT
        lobby.attackMode = AttackMode.RIGHT
        lobby.attackMode shouldBe AttackMode.RIGHT
    }
})
