package com.wingedsheep.gameserver.lobby

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.sdk.core.CommanderPreset
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Which Commander configuration a lobby's games actually run at (issue #1456 § "Pod-tuned config").
 *
 * [CommanderPreset] BRAWL (25 life) and COMMANDER (30 life) are pacing knobs for a 1v1 limited
 * match: 60-card decks race, so the paper 40 drags. A pod inverts that — damage arrives from three
 * opponents instead of one — and paper multiplayer Commander is 40 life for exactly that reason. So
 * the preset a game runs at is a property of the *table*, not of what the host clicked, which is
 * what [TournamentLobby.effectiveCommanderPreset] encodes and both match builders read.
 */
class CommanderPodLobbyTest : FunSpec({

    val forest = CardDefinition.basicLand("Forest", Subtype.FOREST)
    val generator = BoosterGenerator(
        mapOf(
            "TST" to BoosterGenerator.SetConfig(
                setCode = "TST",
                setName = "Test",
                cards = emptyList(),
                basicLands = listOf(forest),
            )
        )
    )

    fun lobby(gameMode: LobbyGameMode, preset: CommanderPreset): TournamentLobby =
        TournamentLobby(
            setCodes = listOf("TST"),
            setNames = listOf("Test"),
            boosterGenerator = generator,
            format = TournamentFormat.COMMANDER_DRAFT,
            gameMode = gameMode,
            commanderPreset = preset,
        )

    val hostChoices = listOf(CommanderPreset.BRAWL, CommanderPreset.COMMANDER)
    val podModes = listOf(
        LobbyGameMode.FREE_FOR_ALL,
        LobbyGameMode.TWO_HEADED_GIANT,
        LobbyGameMode.TEAM_VS_TEAM,
    )

    test("a bracket honours the host's 1v1 preset") {
        for (preset in hostChoices) {
            lobby(LobbyGameMode.TOURNAMENT, preset).effectiveCommanderPreset shouldBe preset
        }
    }

    test("every single-pod table overrides it with the 40-life pod preset") {
        for (mode in podModes) {
            for (preset in hostChoices) {
                lobby(mode, preset).effectiveCommanderPreset shouldBe CommanderPreset.POD
            }
        }
    }

    test("the pod preset is paper multiplayer Commander: 40 life, 21 commander damage") {
        val format = CommanderPreset.POD.toFormat()
        format.startingLife shouldBe 40
        format.commanderDamageThreshold shouldBe 21
        // Still a 60-card limited shape; the match builder overwrites deckSize with the lobby's
        // configured minimum anyway.
        CommanderPreset.POD.deckSize shouldBe 60
    }

    test("switching a Commander Draft lobby from bracket to pod changes the life total it will start at") {
        // The bug this closes: a Commander Draft pod inherited the host's 25-life Brawl default and
        // started a four-player game at 25.
        val l = lobby(LobbyGameMode.TOURNAMENT, CommanderPreset.BRAWL)
        l.effectiveCommanderPreset.toFormat().startingLife shouldBe 25

        l.gameMode = LobbyGameMode.FREE_FOR_ALL
        l.effectiveCommanderPreset.toFormat().startingLife shouldBe 40

        // Switching back restores the host's choice — the pod override is derived, never stored.
        l.gameMode = LobbyGameMode.TOURNAMENT
        l.commanderPreset shouldBe CommanderPreset.BRAWL
        l.effectiveCommanderPreset shouldBe CommanderPreset.BRAWL
    }

    test("Commander rules are per-player at every table size — Format.Commander has no seat count") {
        // Guards the premise the pod work rests on: nothing in the engine's Commander configuration
        // is 1v1-shaped, so the same instance runs a 1v1 match and a six-player Free-for-All.
        val format = Format.Commander()
        format.startingLife shouldBe 40
        format.usesCommanders shouldBe true
        format.sharesTeamLife shouldBe false
        format.playersWinLoseAsTeam shouldBe false
    }
})
