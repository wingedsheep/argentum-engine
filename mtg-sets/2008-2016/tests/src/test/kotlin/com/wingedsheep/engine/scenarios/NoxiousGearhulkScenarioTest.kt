package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Noxious Gearhulk (KLD #96) — {4}{B}{B} 5/4 menace.
 *
 *   When this creature enters, you may destroy another target creature. If a creature is
 *   destroyed this way, you gain life equal to its toughness.
 *
 * Covers the two rulings the script has to get right: the life gained is the creature's
 * toughness as it last existed on the battlefield (counters included), and an indestructible
 * target isn't destroyed this way, so no life is gained.
 */
class NoxiousGearhulkScenarioTest : ScenarioTestBase() {

    private fun castGearhulkTargeting(game: TestGame, targetName: String) {
        val target = game.findPermanent(targetName)!!
        game.castSpell(1, "Noxious Gearhulk").error shouldBe null
        game.resolveStack()
        game.selectTargets(listOf(target)).error shouldBe null
        game.resolveStack()
        game.answerYesNo(true)
        game.resolveStack()
    }

    init {
        test("destroys the target and gains life equal to its last-known toughness") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Noxious Gearhulk")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(bears) { container ->
                container.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 2))
            }

            castGearhulkTargeting(game, "Grizzly Bears")

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            // 2/2 with two +1/+1 counters: last-known toughness 4, not the printed 2.
            game.getLifeTotal(1) shouldBe 24
        }

        test("an indestructible target isn't destroyed this way, so no life is gained") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Noxious Gearhulk")
                .withCardOnBattlefield(2, "Darksteel Myr")
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            castGearhulkTargeting(game, "Darksteel Myr")

            game.isOnBattlefield("Darksteel Myr") shouldBe true
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
