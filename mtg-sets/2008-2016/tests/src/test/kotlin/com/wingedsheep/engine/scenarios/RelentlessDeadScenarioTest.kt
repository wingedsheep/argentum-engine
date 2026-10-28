package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Relentless Dead (SOI #131) — {B}{B} Creature — Zombie, 2/2, menace.
 *
 * "When this creature dies, you may pay {B}. If you do, return it to its owner's hand.
 *  When this creature dies, you may pay {X}. If you do, return another target Zombie creature
 *  card with mana value X from your graveyard to the battlefield."
 *
 * Pins the second trigger's shape: the target is chosen as the trigger goes on the stack (fixing
 * X, CR 107.3f) from *other* Zombie creature cards in your graveyard, and the payment of that
 * card's mana value is offered as it resolves.
 */
class RelentlessDeadScenarioTest : ScenarioTestBase() {

    private class Run(val game: TestGame) {
        var targetDecision: ChooseTargetsDecision? = null
    }

    private fun killRelentlessDead(): Run {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Relentless Dead")
            .withCardInGraveyard(1, "Diregraf Ghoul")
            .withCardInGraveyard(1, "Grizzly Bears")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        val dead = game.findPermanent("Relentless Dead")!!
        game.castSpell(1, "Lightning Bolt", dead).error shouldBe null
        game.resolveStack()
        return Run(game)
    }

    /** Drives every decision until the stack is empty, paying (or declining) each offer. */
    private fun Run.drive(pay: Boolean) {
        repeat(30) {
            when (val d = game.state.pendingDecision) {
                null -> if (game.state.stack.isEmpty()) return else game.resolveStack()
                is OrderObjectsDecision -> game.submitDecision(OrderedResponse(d.id, d.objects)).error shouldBe null
                is ChooseTargetsDecision -> {
                    targetDecision = d
                    val ghoul = game.findCardsInGraveyard(1, "Diregraf Ghoul").single()
                    game.selectTargets(listOf(ghoul)).error shouldBe null
                }
                is YesNoDecision -> game.answerYesNo(pay).error shouldBe null
                is SelectManaSourcesDecision -> game.submitManaSourcesAutoPay().error shouldBe null
                else -> error("Unexpected decision: $d")
            }
        }
    }

    init {
        context("Relentless Dead") {

            test("the X trigger targets only another Zombie creature card in your graveyard") {
                val run = killRelentlessDead()
                run.drive(pay = false)

                val decision = run.targetDecision
                withClue("the targeted dies trigger asked for its target") {
                    (decision != null) shouldBe true
                }
                val legal = decision!!.legalTargets.values.flatten()
                val game = run.game
                legal shouldContain game.findCardsInGraveyard(1, "Diregraf Ghoul").single()
                withClue("Relentless Dead can't target itself (\"another\")") {
                    legal shouldNotContain game.findCardsInGraveyard(1, "Relentless Dead").single()
                }
                withClue("a non-Zombie creature card isn't a legal target") {
                    legal shouldNotContain game.findCardsInGraveyard(1, "Grizzly Bears").single()
                }
            }

            test("paying both: Relentless Dead returns to hand and the Zombie returns to the battlefield") {
                val run = killRelentlessDead()
                run.drive(pay = true)
                val game = run.game

                withClue("paying {B} returns Relentless Dead to its owner's hand") {
                    game.isInHand(1, "Relentless Dead") shouldBe true
                }
                withClue("paying {1} (the Ghoul's mana value) returns the targeted Zombie") {
                    game.isOnBattlefield("Diregraf Ghoul") shouldBe true
                    game.isInGraveyard(1, "Diregraf Ghoul") shouldBe false
                }
                withClue("{B} plus the Ghoul's {1} taps both Swamps — the X payment isn't free") {
                    game.findAllPermanents("Swamp").forEach { swamp ->
                        game.state.getEntity(swamp)?.has<TappedComponent>() shouldBe true
                    }
                }
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }

            test("declining both payments leaves both cards in the graveyard") {
                val run = killRelentlessDead()
                run.drive(pay = false)
                val game = run.game

                game.isInGraveyard(1, "Relentless Dead") shouldBe true
                game.isInGraveyard(1, "Diregraf Ghoul") shouldBe true
                game.isOnBattlefield("Diregraf Ghoul") shouldBe false
            }
        }
    }
}
