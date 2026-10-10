package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.AerieAuxiliary
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [AiProfile.fillUpToMaxTargets]: an "up to N targets" decision takes as many targets as help.
 *
 * The board is the 2026-10-10 AI-vs-AI log's game 3, turn 11: Aerie Auxiliary's support 2 with two
 * friendly creatures beside it put one counter on one of them. Each case asserts the old answer on
 * [AiProfile.PRODUCTION_CANDIDATE_EXPIRING] (the profile the log was played on) beside the new one
 * on [AiProfile.PRODUCTION_CANDIDATE_UPTO], so a pass is evidence the flag is what moved it.
 */
class UpToNTargetsAiTest : FunSpec({

    val cards = TestCards.all + listOf(AerieAuxiliary)
    val registry = CardRegistry().apply { register(cards) }

    val before = AiProfile.PRODUCTION_CANDIDATE_EXPIRING
    val after = AiProfile.PRODUCTION_CANDIDATE_UPTO

    class Support(val driver: GameTestDriver, val decision: ChooseTargetsDecision, val placed: List<EntityId>)

    /**
     * Cast Aerie Auxiliary for player 1 and stop at its support trigger's target decision, with the
     * creatures [setup] put onto the battlefield.
     */
    fun supportDecision(setup: GameTestDriver.() -> List<EntityId>): Support {
        val d = GameTestDriver().apply {
            registerCards(cards)
            initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        repeat(4) { d.putLandOnBattlefield(d.player1, "Plains") }
        val placed = d.setup()
        val aerie = d.putCardInHand(d.player1, "Aerie Auxiliary")
        d.castSpell(d.player1, aerie)
        repeat(10) {
            val pending = d.state.pendingDecision
            if (pending is ChooseTargetsDecision && pending.playerId == d.player1) return Support(d, pending, placed)
            if (pending != null) d.autoResolveDecision() else d.bothPass()
        }
        error("Aerie Auxiliary's target decision never arrived")
    }

    fun answer(profile: AiProfile, s: Support, decision: ChooseTargetsDecision = s.decision): List<EntityId> {
        val response = AIPlayer.create(registry, s.driver.player1, profile).respondToDecision(s.driver.state, decision)
        return response.shouldBeInstanceOf<TargetsResponse>().selectedTargets.getValue(0)
    }

    test("game 3 turn 11: support 2 puts a counter on both friendly creatures, not one") {
        val s = supportDecision {
            listOf(putCreatureOnBattlefield(player1, "Grizzly Bears"), putCreatureOnBattlefield(player1, "Hill Giant"))
        }
        s.decision.targetRequirements.single().maxTargets shouldBe 2

        withClue("the position must still reproduce the misplay") {
            answer(before, s) shouldHaveSize 1
        }
        answer(after, s) shouldContainExactlyInAnyOrder s.placed
    }

    test("a free slot is not filled with the opponent's creature") {
        val s = supportDecision {
            listOf(putCreatureOnBattlefield(player1, "Grizzly Bears"), putCreatureOnBattlefield(player2, "Hill Giant"))
        }
        answer(after, s) shouldBe listOf(s.placed[0])
    }

    test("a mandatory minimum above one is filled even when the extra target helps nobody") {
        val s = supportDecision {
            listOf(putCreatureOnBattlefield(player1, "Grizzly Bears"), putCreatureOnBattlefield(player2, "Hill Giant"))
        }
        // The same requirement made "two target creatures": one target is an illegal answer.
        val mandatory = s.decision.copy(
            targetRequirements = s.decision.targetRequirements.map { it.copy(minTargets = 2) },
        )
        withClue("the position must still reproduce the illegal single-target answer") {
            answer(before, s, mandatory) shouldHaveSize 1
        }
        answer(after, s, mandatory) shouldContainExactlyInAnyOrder s.placed
    }
})
