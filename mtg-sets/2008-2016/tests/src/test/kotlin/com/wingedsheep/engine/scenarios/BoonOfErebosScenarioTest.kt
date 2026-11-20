package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Boon of Erebos (THS #80) — {B} Instant.
 * "Target creature gets +2/+0 until end of turn. Regenerate it. You lose 2 life."
 */
class BoonOfErebosScenarioTest : ScenarioTestBase() {
    init {
        test("pumps +2/+0, costs its caster 2 life, and the regeneration shield survives lethal damage") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Boon of Erebos")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Boon of Erebos", bears).error shouldBe null
            game.resolveStack()

            withClue("+2/+0 until end of turn") {
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 2
            }
            withClue("the caster loses 2 life, the opponent none") {
                game.getLifeTotal(1) shouldBe 18
                game.getLifeTotal(2) shouldBe 20
            }

            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            withClue("regenerated instead of destroyed, and tapped") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
            }
        }
    }
}
