package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Nori, Teller of Tales (HOB) — {1}{R/W} Legendary Creature — Dwarf Bard 2/2.
 * "Whenever Nori attacks, target attacking creature gains first strike until end of turn."
 *
 * The target filter is "attacking", so a creature that stayed home must not be offered — and Nori
 * itself, being an attacker, must be a legal choice.
 */
class NoriTellerOfTalesScenarioTest : ScenarioTestBase() {

    init {
        context("Nori, Teller of Tales") {

            test("attacking grants first strike to a chosen attacking creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nori, Teller of Tales")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                game.state.projectedState.hasKeyword(courser, Keyword.FIRST_STRIKE) shouldBe false

                game.declareAttackers(
                    mapOf("Nori, Teller of Tales" to 2, "Centaur Courser" to 2)
                ).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                withClue("the attack trigger asks for its target") {
                    (decision is ChooseTargetsDecision) shouldBe true
                }
                game.selectTargets(listOf(courser)).error shouldBe null
                game.resolveStack()

                withClue("the chosen attacker gained first strike") {
                    game.state.projectedState.hasKeyword(courser, Keyword.FIRST_STRIKE) shouldBe true
                }
            }

            test("only attacking creatures are legal targets") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nori, Teller of Tales")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    // Stays home — must not be offered.
                    .withCardOnBattlefield(1, "Ordinary Bear")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val nori = game.findPermanent("Nori, Teller of Tales")!!
                val courser = game.findPermanent("Centaur Courser")!!
                val homebody = game.findPermanent("Ordinary Bear")!!

                game.declareAttackers(
                    mapOf("Nori, Teller of Tales" to 2, "Centaur Courser" to 2)
                ).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision() as ChooseTargetsDecision
                val legal = decision.legalTargets[0].orEmpty()
                withClue("the other attacker is offered") { legal shouldContain courser }
                withClue("Nori is attacking too, so it can target itself") { legal shouldContain nori }
                withClue("the creature that did not attack is not offered") {
                    legal shouldNotContain homebody
                }
            }
        }
    }
}
