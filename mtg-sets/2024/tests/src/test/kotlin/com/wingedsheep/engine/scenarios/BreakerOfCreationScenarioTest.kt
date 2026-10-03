package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Breaker of Creation (MH3 #1) — {6}{C}{C} Creature — Eldrazi 8/4
 *
 *   When you cast this spell, you gain 1 life for each colorless permanent you control.
 *   Hexproof from each color
 *   Annihilator 2
 */
class BreakerOfCreationScenarioTest : ScenarioTestBase() {

    init {
        context("Breaker of Creation") {

            test("casting it gains 1 life per colorless permanent you control") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Breaker of Creation")
                    .withLandsOnBattlefield(1, "Wastes", 8)
                    .withCardOnBattlefield(1, "Artisan of Kozilek")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Wastes", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Breaker of Creation").error shouldBe null
                withClue("cast trigger sits above the creature spell") { game.state.stack.size shouldBe 2 }
                game.resolveStack()

                game.findPermanent("Breaker of Creation").shouldNotBeNull()
                withClue("8 Wastes + Artisan of Kozilek; green Bears and the opponent's lands don't count") {
                    game.getLifeTotal(1) shouldBe 29
                }
                game.getLifeTotal(2) shouldBe 20
            }

            test("an opponent's colored spell can't target it") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Breaker of Creation")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val breaker = game.findPermanent("Breaker of Creation")!!
                game.castSpell(2, "Lightning Bolt", breaker).error shouldNotBe null
                game.isInHand(2, "Lightning Bolt") shouldBe true
            }

            test("its controller can still target it with a colored spell") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Breaker of Creation")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val breaker = game.findPermanent("Breaker of Creation")!!
                game.castSpell(1, "Lightning Bolt", breaker).error shouldBe null
            }

            test("attacking makes the defending player sacrifice two permanents of their choice") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Breaker of Creation", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val forest = game.findPermanents("Forest").first()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Breaker of Creation" to 2)).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the defending player chooses") { decision.playerId shouldBe game.player2Id }
                withClue("any permanent is a legal sacrifice") { decision.options.size shouldBe 3 }

                game.selectCards(listOf(bears, forest)).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.findPermanents("Forest").size shouldBe 1
            }
        }
    }
}
