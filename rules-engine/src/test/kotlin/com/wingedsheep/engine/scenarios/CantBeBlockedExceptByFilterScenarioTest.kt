package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A creature granted "can't be blocked except by creatures with <filter>" routes through the
 * projected evasion channel, so the block rules enforce it — but it carried no client badge,
 * leaving the restriction invisible to both players. Only the *colour* variant had one.
 *
 * Named for the mechanic, not a card, because it is driven through the bare effect: every
 * filter-based grant shares this path (Speed, Young Avenger's reflexive payoff; Resilient
 * Roadrunner's activated ability), and no single card's file would own it.
 */
class CantBeBlockedExceptByFilterScenarioTest : FunSpec({

    val grantHasteEvasion = card("Grant Haste Evasion Test") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            val victim = target("target creature", com.wingedsheep.sdk.dsl.Targets.Creature)
            effect = Effects.GrantCantBeBlockedExceptBy(
                victim,
                GameObjectFilter.Creature.withKeyword(Keyword.HASTE),
            )
        }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(grantHasteEvasion)
        initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("the granted evasion shows up as a badge on the creature") {
        val d = driver()
        val runner = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")

        val spell = d.putCardInHand(d.player1, "Grant Haste Evasion Test")
        d.giveMana(d.player1, com.wingedsheep.sdk.core.Color.RED, 1)
        d.castSpell(d.player1, spell, listOf(runner)).isSuccess shouldBe true
        // Stop as soon as the stack is empty: passing further would run the turn to cleanup and
        // expire the EndOfTurn floating effect we're trying to observe.
        repeat(12) {
            if (d.state.pendingDecision != null) d.autoResolveDecision()
            else if (d.stackSize > 0) d.bothPass()
            else return@repeat
        }

        val card = ClientStateTransformer(d.cardRegistry).transform(d.state, d.player1)
            .cards.getValue(runner)

        withClue("activeEffects=${card.activeEffects.map { it.effectId to it.description }}") {
            card.activeEffects.any { it.effectId == "cant_be_blocked_except_by" } shouldBe true
        }
    }
})
