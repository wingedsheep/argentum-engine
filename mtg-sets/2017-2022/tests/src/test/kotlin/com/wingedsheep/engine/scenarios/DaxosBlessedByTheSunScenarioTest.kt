package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Daxos, Blessed by the Sun (THB #9) — {W}{W} 2/★ Legendary Enchantment Creature.
 * "Daxos's toughness is equal to your devotion to white. Whenever another creature you control
 * enters or dies, you gain 1 life."
 *
 * First card to feed devotion into a characteristic-defining toughness, and its life trigger is a
 * `Triggers.or` of an enters atom and a dies atom on `another(…)` — both paths are exercised here.
 */
class DaxosBlessedByTheSunScenarioTest : ScenarioTestBase() {

    init {
        test("toughness tracks devotion to white; another creature entering and dying each gain 1 life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Daxos, Blessed by the Sun")
                .withCardInHand(1, "Savannah Lions")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val daxos = game.findPermanent("Daxos, Blessed by the Sun")!!
            // Daxos's own {W}{W}.
            game.state.projectedState.getToughness(daxos) shouldBe 2
            val lifeBefore = game.getLifeTotal(1)

            game.castSpell(1, "Savannah Lions").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Savannah Lions") shouldBe true
            game.getLifeTotal(1) shouldBe lifeBefore + 1
            // {W}{W} + Savannah Lions' {W}.
            game.state.projectedState.getToughness(daxos) shouldBe 3

            game.castSpell(1, "Lightning Bolt", game.findPermanent("Savannah Lions")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Savannah Lions") shouldBe true
            game.getLifeTotal(1) shouldBe lifeBefore + 2
            game.state.projectedState.getToughness(daxos) shouldBe 2
        }

        test("an opponent's creature entering doesn't trigger") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Daxos, Blessed by the Sun")
                .withCardInHand(2, "Savannah Lions")
                .withLandsOnBattlefield(2, "Plains", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val lifeBefore = game.getLifeTotal(1)
            game.castSpell(2, "Savannah Lions").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Savannah Lions") shouldBe true
            game.getLifeTotal(1) shouldBe lifeBefore
            // Opponent's permanents don't add to your devotion.
            game.state.projectedState.getToughness(game.findPermanent("Daxos, Blessed by the Sun")!!) shouldBe 2
        }
    }
}
