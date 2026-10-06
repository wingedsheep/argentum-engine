package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * `DealDamage(damageDealtVariable = …)` — "you gain life equal to the damage dealt this way".
 *
 * The number is read off the damage actually dealt, so it must survive the optional-redirect pause
 * Blood of the Martyr puts in front of the damage: the executor is re-run from the resumer, and the
 * stored number has to reach the composite step after it.
 */
class DamageDealtThisWayScenarioTest : ScenarioTestBase() {

    private val drainBolt = card("Test Drain Bolt") {
        manaCost = "{R}"
        typeLine = "Instant"
        oracleText = "Test Drain Bolt deals 3 damage to target creature. You gain life equal to the damage dealt this way."
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.Pipeline {
                val dealt = runStoringNumber { Effects.DealDamage(3, creature, damageDealtVariable = it) }
                run(Effects.GainLife(dealt.amount))
            }
        }
    }

    init {
        cardRegistry.register(drainBolt)

        context("damage dealt this way") {

            fun game(): TestGame {
                val game = scenario()
                    .withPlayers("Martyr", "Burner")
                    .withCardInHand(1, "Blood of the Martyr")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Test Drain Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpell(1, "Blood of the Martyr").error shouldBe null
                game.resolveStack()
                game.passPriority()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(2, "Test Drain Bolt", targetId = bears).error shouldBe null
                game.resolveStack()
                return game
            }

            test("redirected damage still counts, across the redirect question") {
                val game = game()
                game.answerYesNo(true)
                game.resolveStack()

                withClue("the 3 damage went to the Martyr instead of the Bears") {
                    game.getLifeTotal(1) shouldBe 17
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("3 damage was dealt this way, so the Burner gains 3") {
                    game.getLifeTotal(2) shouldBe 23
                }
            }

            test("declining the redirect deals the damage to the creature and still gains it") {
                val game = game()
                game.answerYesNo(false)
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 23
            }
        }
    }
}
