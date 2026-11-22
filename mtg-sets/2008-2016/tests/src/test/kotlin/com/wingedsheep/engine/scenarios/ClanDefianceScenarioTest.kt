package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Clan Defiance (GTC #151) — {X}{R}{G} choose one or more: X damage to a creature with flying, to a
 * creature without flying, and/or to a player or planeswalker.
 */
class ClanDefianceScenarioTest : ScenarioTestBase() {
    init {
        test("all three modes deal X damage to their targets") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Clan Defiance")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(2, "Storm Crow")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val crow = game.findPermanent("Storm Crow")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val spell = game.findCardsInHand(1, "Clan Defiance").single()

            val result = game.execute(
                CastSpell(
                    game.player1Id,
                    spell,
                    xValue = 2,
                    chosenModes = listOf(0, 1, 2),
                    modeTargetsOrdered = listOf(
                        listOf(ChosenTarget.Permanent(crow)),
                        listOf(ChosenTarget.Permanent(bears)),
                        listOf(ChosenTarget.Player(game.player2Id)),
                    ),
                )
            )
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isInGraveyard(2, "Storm Crow") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.getLifeTotal(2) shouldBe 18
        }

        test("the without-flying mode can't target a flier") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Clan Defiance")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(2, "Storm Crow")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val crow = game.findPermanent("Storm Crow")!!
            val spell = game.findCardsInHand(1, "Clan Defiance").single()

            val result = game.execute(
                CastSpell(
                    game.player1Id,
                    spell,
                    xValue = 2,
                    chosenModes = listOf(1),
                    modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(crow))),
                )
            )
            result.error shouldNotBe null
            game.isOnBattlefield("Storm Crow") shouldBe true
        }
    }
}
