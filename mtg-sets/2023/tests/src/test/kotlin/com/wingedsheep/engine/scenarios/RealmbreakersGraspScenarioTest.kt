package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Realmbreaker's Grasp (MOM #33) — {1}{W} Enchantment — Aura.
 *
 *   Enchant artifact or creature
 *   Enchanted permanent can't attack or block, and its activated abilities can't be
 *   activated unless they're mana abilities.
 */
class RealmbreakersGraspScenarioTest : ScenarioTestBase() {

    private val elvesManaAbilityId =
        cardRegistry.getCard("Llanowar Elves")!!.script.activatedAbilities.first().id
    private val sorcererPingId =
        cardRegistry.getCard("Prodigal Sorcerer")!!.script.activatedAbilities.first().id

    init {
        context("Realmbreaker's Grasp") {

            test("enchanted creature can't attack or block") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(1, "Realmbreaker's Grasp", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Enchanted creature can't attack") {
                    game.state.projectedState.cantAttack(bears) shouldBe true
                }
                withClue("Enchanted creature can't block") {
                    game.state.projectedState.cantBlock(bears) shouldBe true
                }
            }

            test("enchanted creature's non-mana activated ability is locked") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(2, "Prodigal Sorcerer", summoningSickness = false)
                    .withCardAttachedTo(1, "Realmbreaker's Grasp", "Prodigal Sorcerer")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sorcerer = game.findPermanent("Prodigal Sorcerer")!!
                val ping = game.getLegalActions(2).find {
                    val a = it.action
                    a is ActivateAbility && a.sourceId == sorcerer && a.abilityId == sorcererPingId
                }
                withClue("The tap-to-ping ability should not be activatable") {
                    ping shouldBe null
                }
            }

            test("enchanted creature's mana ability still works") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(2, "Llanowar Elves", summoningSickness = false)
                    .withCardAttachedTo(1, "Realmbreaker's Grasp", "Llanowar Elves")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elves = game.findPermanent("Llanowar Elves")!!
                val manaActivation = game.getLegalActions(2).find {
                    val a = it.action
                    a is ActivateAbility && a.sourceId == elves && a.abilityId == elvesManaAbilityId
                }
                withClue("Mana abilities are exempt from the lock") {
                    (manaActivation != null) shouldBe true
                }
            }

            test("control: without the Aura the sorcerer can ping") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(2, "Prodigal Sorcerer", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sorcerer = game.findPermanent("Prodigal Sorcerer")!!
                val ping = game.getLegalActions(2).find {
                    val a = it.action
                    a is ActivateAbility && a.sourceId == sorcerer && a.abilityId == sorcererPingId
                }
                withClue("Without the Aura the ping is available") {
                    (ping != null) shouldBe true
                }
            }
        }
    }
}
