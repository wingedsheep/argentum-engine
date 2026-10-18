package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.DokuchiSilencer
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Dokuchi Silencer — {1}{B} Creature — Human Ninja 2/1
 * Ninjutsu {1}{B}
 * Whenever this creature deals combat damage to a player, you may discard a creature card. When you
 * do, destroy target creature or planeswalker that player controls.
 *
 * Pins the CR 603.12 gate (the discard must be a creature card; with none in hand there's no prompt)
 * and the reflexive target scoped to the damaged player's permanents.
 */
class DokuchiSilencerScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + DokuchiSilencer)
        initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun attackUnblocked(d: GameTestDriver, attacker: EntityId) {
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(attacker), d.player2).outcome shouldBe Outcome.Done
        d.bothPass()
        d.declareNoBlockers(d.player2)
    }

    /** Resolve combat, answering the may with yes; returns how many yes/no prompts appeared. */
    fun settle(d: GameTestDriver, discard: EntityId? = null, target: EntityId? = null, mustNotTarget: EntityId? = null): Int {
        var prompts = 0
        var guard = 0
        while (guard++ < 30 && (d.state.stack.isNotEmpty() || d.pendingDecision != null ||
                d.state.step != Step.POSTCOMBAT_MAIN)) {
            when (val pd = d.pendingDecision) {
                is YesNoDecision -> { prompts++; d.submitYesNo(pd.playerId, true).error shouldBe null }
                is SelectCardsDecision -> {
                    pd.options shouldContain discard!!
                    d.submitCardSelection(pd.playerId, listOf(discard)).error shouldBe null
                }
                is ChooseTargetsDecision -> {
                    val legal = pd.legalTargets.getValue(0)
                    legal shouldContain target!!
                    if (mustNotTarget != null) legal shouldNotContain mustNotTarget
                    d.submitTargetSelection(pd.playerId, listOf(target)).error shouldBe null
                }
                null -> d.bothPass()
                else -> error("unexpected decision $pd")
            }
        }
        return prompts
    }

    test("discarding a creature card destroys a creature the damaged player controls") {
        val d = driver()
        val toDiscard = d.putCardInHand(d.player1, "Grizzly Bears")
        val ownBears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val theirBears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val ninja = d.putCreatureOnBattlefield(d.player1, DokuchiSilencer.name).also(d::removeSummoningSickness)
        attackUnblocked(d, ninja)

        settle(d, discard = toDiscard, target = theirBears, mustNotTarget = ownBears) shouldBe 1

        d.getGraveyardCardNames(d.player1) shouldContain "Grizzly Bears"
        d.getGraveyardCardNames(d.player2) shouldContain "Grizzly Bears"
        d.findPermanent(d.player1, "Grizzly Bears") shouldBe ownBears
        d.getLifeTotal(d.player2) shouldBe 18
    }

    test("with no creature card in hand there is no prompt and nothing is destroyed") {
        val d = driver()
        val swamp = d.putCardInHand(d.player1, "Swamp")
        val theirBears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val ninja = d.putCreatureOnBattlefield(d.player1, DokuchiSilencer.name).also(d::removeSummoningSickness)
        attackUnblocked(d, ninja)

        settle(d) shouldBe 0

        d.findPermanent(d.player2, "Grizzly Bears") shouldBe theirBears
        d.getHand(d.player1) shouldContain swamp
    }
})
