package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Panicked Bystander // Cackling Culprit (VOW #28).
 *
 *   Front — Panicked Bystander (2/2) — Whenever this or another creature you control dies, gain 1
 *           life. At the beginning of your end step, if you gained 3+ life this turn, transform.
 *   Back  — Cackling Culprit (3/5) — same dies → gain 1 life. {1}{B}: gains deathtouch until EOT.
 *
 * Exercises the dies → gain-1-life trigger, the intervening-if end-step transform gated on gaining
 * 3+ life this turn, and the back face's {1}{B} deathtouch ability.
 */
class PanickedBystanderScenarioTest : ScenarioTestBase() {

    init {
        context("Panicked Bystander") {

            test("after gaining 3+ life this turn, the end-step trigger transforms it") {
                // A Death Trigger Test Creature gains 3 life when it dies — meeting the threshold in
                // one shot (plus 1 more from the Bystander's own dies-trigger reaction).
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Panicked Bystander", summoningSickness = false)
                    .withCardOnBattlefield(1, "Death Trigger Test Creature", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bystander = game.findPermanent("Panicked Bystander")!!
                val fodder = game.findPermanent("Death Trigger Test Creature")!!

                // Kill the fodder: its death → gain 3 life; the Bystander's dies-trigger → gain 1 more.
                game.castSpell(1, "Lightning Bolt", targetId = fodder).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("gained 3+ life this turn") { game.getLifeTotal(1) shouldBe 24 }

                // End step: the intervening-if condition holds → transform.
                game.passUntilPhase(Phase.ENDING, Step.END)
                var guard = 0
                while (game.state.getEntity(bystander)!!.get<CardComponent>()!!.name == "Panicked Bystander" && guard++ < 10) {
                    game.resolveStack()
                }

                withClue("gained 3+ life → transformed to Cackling Culprit (3/5)") {
                    game.state.getEntity(bystander)!!.get<CardComponent>()!!.name shouldBe "Cackling Culprit"
                    game.state.projectedState.getPower(bystander) shouldBe 3
                    game.state.projectedState.getToughness(bystander) shouldBe 5
                }
            }

            test("without gaining 3 life, the end-step trigger does not transform it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Panicked Bystander", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bystander = game.findPermanent("Panicked Bystander")!!

                game.passUntilPhase(Phase.ENDING, Step.END)
                repeat(3) { game.resolveStack() }

                withClue("no life gained → stays Panicked Bystander") {
                    game.state.getEntity(bystander)!!.get<CardComponent>()!!.name shouldBe "Panicked Bystander"
                }
            }

            test("the Cackling Culprit back face can grant itself deathtouch for {1}{B}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cackling Culprit", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val culprit = game.findPermanent("Cackling Culprit")!!
                val deathtouchAbility = cardRegistry.getCard("Cackling Culprit")!!.activatedAbilities.first().id

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = culprit, abilityId = deathtouchAbility)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Cackling Culprit gained deathtouch until end of turn") {
                    game.state.projectedState.hasKeyword(culprit, Keyword.DEATHTOUCH) shouldBe true
                }
            }
        }
    }
}
