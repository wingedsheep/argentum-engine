package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Unnatural Endurance — {B} Instant, devoid.
 * "Target creature gets +2/+0 until end of turn. Regenerate it."
 */
class UnnaturalEnduranceScenarioTest : ScenarioTestBase() {
    init {
        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Unnatural Endurance")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Swamp", 1)
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("target creature gets +2/+0") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!

            val result = game.castSpell(1, "Unnatural Endurance", bears)
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 4
            game.state.projectedState.getToughness(bears) shouldBe 2
        }

        test("the regeneration shield saves it from lethal damage, tapping it") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Unnatural Endurance", bears).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            withClue("regenerated instead of destroyed") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            }
            game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
        }
    }
}
