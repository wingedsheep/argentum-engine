package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Archipelagore (IKO #41) — {5}{U}{U} Creature — Leviathan 7/7.
 *
 *   Mutate {5}{U}
 *   Whenever this creature mutates, tap up to X target creatures, where X is the number of times
 *   this creature has mutated. Those creatures don't untap during their controller's next untap step.
 *
 * X caps the trigger's target count and is read off the merged permanent as the trigger is put on
 * the stack: one target on the first mutation, two when the permanent had already mutated once
 * (Porcuparrot first). Cast normally it doesn't mutate and nothing triggers.
 */
class ArchipelagoreScenarioTest : ScenarioTestBase() {

    private fun TestGame.mutateOnto(cardName: String, host: EntityId) {
        val offer = getLegalActions(1).first {
            val cast = it.action as? CastSpell ?: return@first false
            cast.alternativeCostType == AlternativeCostType.MUTATE &&
                state.getEntity(cast.cardId)?.get<CardComponent>()?.name == cardName
        }
        execute((offer.action as CastSpell).copy(targets = listOf(ChosenTarget.Permanent(host)))).error shouldBe null
        resolveStack()
        answerYesNo(false)
    }

    private fun build() = scenario()
        .withPlayers("P1", "P2")
        .withCardInHand(1, "Archipelagore")
        .withCardInHand(1, "Porcuparrot")
        .withLandsOnBattlefield(1, "Island", 6)
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
        .withCardOnBattlefield(2, "Ornithopter", summoningSickness = false)
        .withCardOnBattlefield(2, "Memnite", summoningSickness = false)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.tapped(id: EntityId) = state.getEntity(id)?.has<TappedComponent>() == true
    private fun TestGame.locked(id: EntityId) = state.projectedState.hasKeyword(id, AbilityFlag.DOESNT_UNTAP)

    init {
        context("Archipelagore") {

            test("the first mutation taps up to one target creature and locks it down") {
                val game = build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val memnite = game.findPermanent("Memnite")!!
                game.mutateOnto("Archipelagore", bears)

                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("X = 1 after the first mutation") {
                    decision.targetRequirements[0].maxTargets shouldBe 1
                    decision.targetRequirements[0].minTargets shouldBe 0
                }
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant is tapped and won't untap next untap step") {
                    game.tapped(giant) shouldBe true
                    game.locked(giant) shouldBe true
                }
                withClue("Memnite is untouched") {
                    game.tapped(memnite) shouldBe false
                    game.locked(memnite) shouldBe false
                }
            }

            test("mutating onto an already-mutated creature allows two targets") {
                val game = build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val memnite = game.findPermanent("Memnite")!!
                val thopter = game.findPermanent("Ornithopter")!!

                game.mutateOnto("Porcuparrot", bears)
                game.resolveStack()
                game.mutateOnto("Archipelagore", bears)

                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("X = 2: the permanent has mutated twice") {
                    decision.targetRequirements[0].maxTargets shouldBe 2
                }
                game.selectTargets(listOf(giant, memnite)).error shouldBe null
                game.resolveStack()

                game.tapped(giant) shouldBe true
                game.locked(giant) shouldBe true
                game.tapped(memnite) shouldBe true
                game.locked(memnite) shouldBe true
                game.tapped(thopter) shouldBe false
            }

            test("cast normally it doesn't mutate and nothing triggers") {
                val game = build()
                game.castSpell(1, "Archipelagore").error shouldBe null
                game.resolveStack()
                withClue("no target decision is pending") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe false
                }
                game.tapped(game.findPermanent("Hill Giant")!!) shouldBe false
            }
        }
    }
}
