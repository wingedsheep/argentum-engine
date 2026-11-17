package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Fruit of the First Tree (FRF #132) — {3}{G} Enchantment — Aura.
 *
 * "Enchant creature
 *  When enchanted creature dies, you gain X life and draw X cards, where X is its toughness."
 *
 * X is the creature's toughness as it last existed on the battlefield, and the Aura's controller
 * gains and draws even when it enchants an opponent's creature (2014-11-24 rulings).
 */
class FruitOfTheFirstTreeScenarioTest : ScenarioTestBase() {

    init {
        context("Fruit of the First Tree") {

            test("gains life and draws cards equal to the dead creature's toughness") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardAttachedTo(1, "Fruit of the First Tree", "Hill Giant")
                    .withCardInHand(2, "Doom Blade")
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder.withCardInLibrary(1, "Forest") }
                val game = builder.build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(2, "Doom Blade", giant).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Hill Giant") shouldBe true
                withClue("Hill Giant had toughness 3") {
                    game.getLifeTotal(1) shouldBe 23
                    game.handSize(1) shouldBe 3
                }
            }

            test("uses last-known toughness and rewards the Aura's controller on an opponent's creature") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardAttachedTo(1, "Fruit of the First Tree", "Hill Giant")
                    .withCardInHand(1, "Giant Growth")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(8) { builder.withCardInLibrary(1, "Forest") }
                repeat(8) { builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Giant Growth", giant).error shouldBe null
                game.resolveStack()
                game.castSpell(1, "Doom Blade", giant).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Hill Giant") shouldBe true
                withClue("3 base toughness + 3 from Giant Growth goes to the Aura's controller") {
                    game.getLifeTotal(1) shouldBe 26
                    game.handSize(1) shouldBe 6
                }
                withClue("the creature's controller gets nothing") {
                    game.getLifeTotal(2) shouldBe 20
                    game.handSize(2) shouldBe 0
                }
            }
        }
    }
}
