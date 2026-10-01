package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Planar Disruption (ONE #28) — {1}{W} Enchantment — Aura.
 *
 *   Enchant artifact, creature, or planeswalker
 *   Enchanted permanent can't attack or block, and its activated abilities can't be activated.
 */
class PlanarDisruptionScenarioTest : ScenarioTestBase() {

    init {
        context("Planar Disruption") {

            test("enchanted creature can't attack or block and can't activate abilities") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(2, "Llanowar Elves", summoningSickness = false)
                    .withCardAttachedTo(1, "Planar Disruption", "Llanowar Elves")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elves = game.findPermanent("Llanowar Elves")!!
                game.state.projectedState.cantAttack(elves) shouldBe true
                game.state.projectedState.cantBlock(elves) shouldBe true
                withClue("mana abilities are locked too") {
                    game.getLegalActions(2).any {
                        val a = it.action
                        a is ActivateAbility && a.sourceId == elves
                    } shouldBe false
                }
            }

            test("enchanted planeswalker's loyalty abilities can't be activated") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(2, "Jace Beleren")
                    .withCardAttachedTo(1, "Planar Disruption", "Jace Beleren")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jace = game.findPermanent("Jace Beleren")!!
                game.getLegalActions(2).any {
                    val a = it.action
                    a is ActivateAbility && a.sourceId == jace
                } shouldBe false
            }

            test("control: an unenchanted planeswalker can activate loyalty abilities") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(2, "Jace Beleren")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jace = game.findPermanent("Jace Beleren")!!
                game.getLegalActions(2).any {
                    val a = it.action
                    a is ActivateAbility && a.sourceId == jace
                } shouldBe true
            }

            test("can enchant an artifact but not an enchantment") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Planar Disruption")
                    .withCardInHand(1, "Planar Disruption")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withCardOnBattlefield(2, "Pacifism")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pacifism = game.findPermanent("Pacifism")!!
                game.castSpell(1, "Planar Disruption", pacifism).error shouldNotBe null

                val thopter = game.findPermanent("Ornithopter")!!
                game.castSpell(1, "Planar Disruption", thopter).error shouldBe null
                game.resolveStack()
                game.state.projectedState.cantAttack(thopter) shouldBe true
            }

            test("locks a noncreature artifact's activated abilities") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Planar Disruption")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(1, "Millstone")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val millstone = game.findPermanent("Millstone")!!
                fun canActivate() = game.getLegalActions(1).any {
                    val a = it.action
                    a is ActivateAbility && a.sourceId == millstone
                }
                withClue("Millstone is activatable before the Aura") { canActivate() shouldBe true }

                game.castSpell(1, "Planar Disruption", millstone).error shouldBe null
                game.resolveStack()

                canActivate() shouldBe false
            }
        }
    }
}
