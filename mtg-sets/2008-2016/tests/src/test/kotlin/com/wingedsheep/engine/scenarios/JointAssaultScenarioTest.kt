package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Joint Assault — "Target creature gets +2/+2 until end of turn. If it's paired with a creature,
 * that creature also gets +2/+2 until end of turn." The partner is found through
 * `GameObjectFilter.pairedWith(target)` (StatePredicate.IsPairedWith over IsEntity).
 */
class JointAssaultScenarioTest : ScenarioTestBase() {

    init {
        context("Joint Assault") {

            test("a paired target and its partner both get +2/+2") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardInHand(1, "Lightning Mauler")
                    .withCardInHand(1, "Joint Assault")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val lions = game.findPermanent("Savannah Lions")!!
                game.castSpell(1, "Lightning Mauler").error shouldBe null
                game.resolveStack()
                game.selectCards(listOf(bears))
                val mauler = game.findPermanent("Lightning Mauler")!!

                val cast = game.castSpell(1, "Joint Assault", targetId = bears)
                withClue("the cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("the target gets +2/+2") {
                    projected.getPower(bears) shouldBe 4
                    projected.getToughness(bears) shouldBe 4
                }
                withClue("the creature it's paired with also gets +2/+2") {
                    projected.getPower(mauler) shouldBe 4
                    projected.getToughness(mauler) shouldBe 3
                }
                withClue("an unrelated creature is untouched") { projected.getPower(lions) shouldBe 1 }
            }

            test("an unpaired target alone gets +2/+2") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Lightning Mauler")
                    .withCardInHand(1, "Joint Assault")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val mauler = game.findPermanent("Lightning Mauler")!!
                game.castSpell(1, "Joint Assault", targetId = bears).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                projected.getPower(bears) shouldBe 4
                withClue("nothing is paired with the target, so no one else grows") {
                    projected.getPower(mauler) shouldBe 2
                }
            }
        }
    }
}
