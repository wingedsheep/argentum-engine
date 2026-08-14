package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Henrika Domnathi // Henrika, Infernal Seer (VOW #119).
 *
 *   Front — Henrika Domnathi (1/3, Flying) — At the beginning of combat on your turn, choose one
 *           that hasn't been chosen — sacrifice / draw-and-lose-1 / transform.
 *   Back  — Henrika, Infernal Seer (3/4, Flying, deathtouch, lifelink) — {1}{B}: Each creature you
 *           control with flying, deathtouch, and/or lifelink gets +1/+0 until end of turn.
 *
 * Exercises the "choose one that hasn't been chosen" modal begin-combat trigger — the draw/lose
 * mode, then the transform mode (and that a spent mode is never re-offered) — and the back's
 * keyword-union pump.
 */
class HenrikaDomnathiScenarioTest : ScenarioTestBase() {

    private val sacrificeMode = "Each player sacrifices a creature of their choice"
    private val drawMode = "You draw a card and you lose 1 life"
    private val transformMode = "Transform Henrika"

    private fun TestGame.resolveToModeChoice(): ChooseOptionDecision {
        resolveStack()
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        return decision as ChooseOptionDecision
    }

    private fun TestGame.chooseMode(decision: ChooseOptionDecision, description: String) {
        val index = decision.options.indexOf(description)
        check(index >= 0) { "Mode '$description' not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    init {
        context("Henrika Domnathi — begin-combat modal") {

            test("the draw/lose-1 mode resolves and is not offered again next combat") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Henrika Domnathi", summoningSickness = false)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                // Library fuel so neither player decks out over a full turn cycle (the draw mode
                // draws once, and each player has a draw step on the way back to our next combat).
                repeat(10) { builder = builder.withCardInLibrary(1, "Swamp") }
                repeat(10) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                val firstChoice = game.resolveToModeChoice()

                withClue("all three modes are offered the first time") {
                    firstChoice.options shouldContain sacrificeMode
                    firstChoice.options shouldContain drawMode
                    firstChoice.options shouldContain transformMode
                }

                val handBefore = game.handSize(1)
                game.chooseMode(firstChoice, drawMode)
                game.resolveStack()

                withClue("drew a card (hand +1)") { game.handSize(1) shouldBe handBefore + 1 }
                withClue("lost 1 life (20 -> 19)") { game.getLifeTotal(1) shouldBe 19 }

                // Cycle a full turn back to our next begin-combat: finish our turn, run the
                // opponent's turn, arrive at our combat again. passUntilPhase is forward-only and
                // no-ops when already at the target step, so it must leave BEGIN_COMBAT first.
                game.passUntilPhase(Phase.ENDING, Step.END)          // finish our turn
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT) // opponent's combat (no trigger)
                game.passUntilPhase(Phase.ENDING, Step.END)          // finish opponent's turn
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT) // our next combat
                val secondChoice = game.resolveToModeChoice()
                withClue("the chosen draw mode is no longer offered") {
                    secondChoice.options shouldNotContain drawMode
                    secondChoice.options shouldContain transformMode
                }
            }

            test("the transform mode flips Henrika to her Infernal Seer back face") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Henrika Domnathi", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val henrika = game.findPermanent("Henrika Domnathi")!!

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                val choice = game.resolveToModeChoice()
                game.chooseMode(choice, transformMode)
                game.resolveStack()

                withClue("Henrika transformed into Henrika, Infernal Seer (3/4)") {
                    game.state.getEntity(henrika)!!.get<CardComponent>()!!.name shouldBe "Henrika, Infernal Seer"
                    game.state.projectedState.getPower(henrika) shouldBe 3
                    game.state.projectedState.getToughness(henrika) shouldBe 4
                }
            }
        }

        context("Henrika, Infernal Seer — keyword-union pump") {

            test("{1}{B} pumps every creature with flying, deathtouch, and/or lifelink by +1/+0") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Henrika, Infernal Seer", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val henrika = game.findPermanent("Henrika, Infernal Seer")!!
                val abilityId = cardRegistry.getCard("Henrika, Infernal Seer")!!.activatedAbilities.first().id

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = henrika, abilityId = abilityId)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Henrika (she has flying) gets +1/+0: 3/4 -> 4/4") {
                    game.state.projectedState.getPower(henrika) shouldBe 4
                    game.state.projectedState.getToughness(henrika) shouldBe 4
                }
            }
        }
    }
}
