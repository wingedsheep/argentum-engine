package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Heliod's Intervention (THB #19) — {X}{W}{W} Instant, choose one:
 * destroy X target artifacts and/or enchantments, or target player gains twice X life.
 *
 * Exercises an exactly-X target count inside a mode (CR 601.2c) and the doubled-X life mode.
 */
class HeliodsInterventionScenarioTest : ScenarioTestBase() {

    private val destroyMode = 0
    private val lifeMode = 1

    init {
        test("destroy mode destroys exactly X artifacts and/or enchantments") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Heliod's Intervention")
                .withCardOnBattlefield(2, "Ornithopter")
                .withCardOnBattlefield(2, "Glorious Anthem")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val spell = game.findCardsInHand(1, "Heliod's Intervention").single()
            val targets = listOf(
                ChosenTarget.Permanent(game.findPermanent("Ornithopter")!!),
                ChosenTarget.Permanent(game.findPermanent("Glorious Anthem")!!),
            )
            game.execute(
                CastSpell(
                    game.player1Id, spell, targets,
                    xValue = 2,
                    chosenModes = listOf(destroyMode),
                    modeTargetsOrdered = listOf(targets),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Ornithopter") shouldBe true
            game.isInGraveyard(2, "Glorious Anthem") shouldBe true
        }

        test("destroy mode rejects a target count other than X") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Heliod's Intervention")
                .withCardOnBattlefield(2, "Ornithopter")
                .withCardOnBattlefield(2, "Glorious Anthem")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val spell = game.findCardsInHand(1, "Heliod's Intervention").single()
            val targets = listOf(ChosenTarget.Permanent(game.findPermanent("Ornithopter")!!))
            game.execute(
                CastSpell(
                    game.player1Id, spell, targets,
                    xValue = 2,
                    chosenModes = listOf(destroyMode),
                    modeTargetsOrdered = listOf(targets),
                )
            ).error shouldNotBe null
        }

        test("life mode gives the target player twice X life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Heliod's Intervention")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val spell = game.findCardsInHand(1, "Heliod's Intervention").single()
            val target = listOf(ChosenTarget.Player(game.player2Id))
            val lifeBefore = game.getLifeTotal(2)
            game.execute(
                CastSpell(
                    game.player1Id, spell, target,
                    xValue = 3,
                    chosenModes = listOf(lifeMode),
                    modeTargetsOrdered = listOf(target),
                )
            ).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe lifeBefore + 6
        }
    }
}
