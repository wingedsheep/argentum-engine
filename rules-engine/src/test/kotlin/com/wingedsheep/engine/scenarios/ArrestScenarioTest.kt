package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Arrest (MMQ #4, reprinted in MRD) — {2}{W} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature can't attack or block, and its activated abilities can't be activated.
 *
 * Same three-static shape as Petrify, but restricted to creature hosts. The activation lock
 * is deliberately not `nonManaAbilitiesOnly`, so mana abilities are locked too.
 */
class ArrestScenarioTest : ScenarioTestBase() {

    private val elvesManaAbilityId =
        cardRegistry.getCard("Llanowar Elves")!!.script.activatedAbilities.first().id

    init {
        context("Arrest") {

            test("enchanted creature can't attack or block") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(1, "Arrest", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("Arrest should be on the battlefield") {
                    game.isOnBattlefield("Arrest") shouldBe true
                }
                withClue("Enchanted creature can't attack") {
                    game.state.projectedState.cantAttack(bears) shouldBe true
                }
                withClue("Enchanted creature can't block") {
                    game.state.projectedState.cantBlock(bears) shouldBe true
                }
            }

            test("enchanted creature's activated abilities can't be activated") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(2, "Llanowar Elves", summoningSickness = false)
                    .withCardAttachedTo(1, "Arrest", "Llanowar Elves")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elves = game.findPermanent("Llanowar Elves")!!
                val elvesActivation = game.getLegalActions(2).find {
                    val a = it.action
                    a is ActivateAbility && a.sourceId == elves && a.abilityId == elvesManaAbilityId
                }
                withClue("Arrest should lock the enchanted creature's mana ability") {
                    elvesActivation shouldBe null
                }
            }

            test("the lock is scoped to the host: another creature is unaffected") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Llanowar Elves", summoningSickness = false)
                    .withCardAttachedTo(1, "Arrest", "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elves = game.findPermanent("Llanowar Elves")!!
                withClue("The unenchanted creature can still attack") {
                    game.state.projectedState.cantAttack(elves) shouldBe false
                }
                val elvesActivation = game.getLegalActions(2).find {
                    val a = it.action
                    a is ActivateAbility && a.sourceId == elves && a.abilityId == elvesManaAbilityId
                }
                withClue("The unenchanted creature's mana ability is still available") {
                    (elvesActivation != null) shouldBe true
                }
            }
        }
    }
}
