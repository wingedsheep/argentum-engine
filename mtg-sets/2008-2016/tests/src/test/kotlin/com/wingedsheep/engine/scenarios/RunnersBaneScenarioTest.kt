package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Runner's Bane (DGM #17) — {1}{U} Enchantment — Aura.
 *
 *   Enchant creature with power 3 or less
 *   When this Aura enters, tap enchanted creature.
 *   Enchanted creature doesn't untap during its controller's untap step.
 */
class RunnersBaneScenarioTest : ScenarioTestBase() {

    private fun buildGame(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Runner's Bane")
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Air Elemental")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("enchanting a power-3 creature taps it and keeps it from untapping") {
            val game = buildGame()
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Runner's Bane", giant).error shouldBe null
            game.resolveStack()

            withClue("the Aura is on the battlefield and its ETB tapped the creature") {
                game.isOnBattlefield("Runner's Bane") shouldBe true
                game.state.getEntity(giant)?.has<TappedComponent>() shouldBe true
            }
            withClue("the enchanted creature doesn't untap during its controller's untap step") {
                game.state.projectedState.hasKeyword(giant, AbilityFlag.DOESNT_UNTAP) shouldBe true
            }
        }

        test("can't enchant a creature with power greater than 3") {
            val game = buildGame()
            val elemental = game.findPermanent("Air Elemental")!!

            withClue("Air Elemental has power 4") {
                game.castSpell(1, "Runner's Bane", elemental).error.shouldNotBeNull()
            }
        }
    }
}
