package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lrw.cards.ColfenorsPlans
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.withClue

/**
 * Scenario test for Colfenor's Plans (LRW #106).
 *
 * "When this enchantment enters, exile the top seven cards of your library face down.
 *  You may look at the cards exiled with this enchantment, and you may play lands and cast spells
 *  from among those cards.
 *  Skip your draw step.
 *  You can't cast more than one spell each turn."
 *
 * The new vocabulary here is the standing `SkipStepOrPhase(DRAW_STEP)` static, and the thing worth pinning about
 * it is its **scope**: it is the controller's draw step, not everyone's. So the first test watches
 * both players' draw steps in one game — the opponent's draw is the half that a player-agnostic
 * read of the static would break, and it is invisible if you only assert on the controller.
 *
 * The skip is read off the enchantment as each draw step begins, so it stops when the enchantment
 * stops having the ability: the third test strips it ("loses all abilities") and the controller
 * draws again.
 *
 * The last test covers the pile the enchantment builds: seven cards, face down, and playable
 * from exile afterwards. "You may play lands" is the leg that needs `filter = Any` — a land is
 * played, never cast (CR 305.1) — and a Swamp deck makes the pile all lands, so the land play is
 * exactly what shows up in the legal actions. It is checked on a *later* turn on purpose: per the
 * 2007-10-01 ruling, the turn you cast this you have already used up your one spell.
 */
class ColfenorsPlansScenarioTest : FunSpec({

    /** A test-only instant that permanently strips every ability from target permanent. */
    val mute = card("Mute Plans") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Permanent)
            effect = Effects.RemoveAllAbilities(t, Duration.Permanent)
        }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + ColfenorsPlans + mute)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** The face-down cards in [player]'s exile that this enchantment's pile is made of. */
    fun GameTestDriver.exiledCards(player: EntityId): List<EntityId> = getExile(player).toList()

    test("the controller skips their draw step; the opponent still draws") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Colfenor's Plans")

        val opponentHandBefore = d.getHandSize(d.player2)
        val controllerHandBefore = d.getHandSize(d.player1)

        // Player 2's draw step (turn 2) — the enchantment is not theirs.
        d.passPriorityUntil(Step.DRAW)
        withClue("\"Skip your draw step\" binds the controller only") {
            d.getHandSize(d.player2) shouldBe opponentHandBefore + 1
        }

        // Player 1's draw step (turn 3) — the first one they actually get, since the starting
        // player skips their own turn-1 draw by CR 103.8a.
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.DRAW)
        withClue("the controller's draw step happened and drew nothing") {
            d.getHandSize(d.player1) shouldBe controllerHandBefore
        }

        // And it is standing, not a one-shot marker: the next one is skipped too.
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.DRAW) // player 2 draws
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.DRAW) // player 1 again
        withClue("a standing static skips every draw step, not just the next one") {
            d.getHandSize(d.player1) shouldBe controllerHandBefore
        }
    }

    test("once the enchantment loses all abilities, its controller draws again") {
        val d = driver()
        val plans = d.putPermanentOnBattlefield(d.player1, "Colfenor's Plans")
        d.castSpell(d.player1, d.putCardInHand(d.player1, mute.name), listOf(plans)).error shouldBe null
        d.bothPass()
        withClue("the enchantment has lost its abilities") {
            d.state.projectedState.hasLostAllAbilities(plans) shouldBe true
        }

        val controllerHandBefore = d.getHandSize(d.player1)
        d.passPriorityUntil(Step.DRAW) // player 2
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.DRAW) // player 1
        withClue("\"skip your draw step\" no longer applies, so the draw step draws a card") {
            d.getHandSize(d.player1) shouldBe controllerHandBefore + 1
        }
    }

    test("entering exiles the top seven face down, and they can be played later") {
        val d = driver()
        val plans = d.putCardInHand(d.player1, "Colfenor's Plans")
        d.giveMana(d.player1, Color.BLACK, 4)
        d.castSpell(d.player1, plans).error shouldBe null
        d.bothPass() // the enchantment resolves
        d.bothPass() // its enters trigger resolves

        val pile = d.exiledCards(d.player1)
        withClue("the top seven cards of your library") {
            pile.size shouldBe 7
        }
        withClue("face down") {
            pile.all { d.state.getEntity(it)?.has<FaceDownComponent>() == true } shouldBe true
        }

        // Round to player 1's next main phase — the cast turn is spent (2007-10-01 ruling).
        d.passPriorityUntil(Step.DRAW) // player 2
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.DRAW) // player 1, skipped
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        withClue("\"you may play lands … from among those cards\" — a land in the pile is offered " +
            "as a land play, which only a grant whose filter admits lands can do") {
            d.legalActions(d.player1).any { legal ->
                val action = legal.action
                action is PlayLand && action.cardId in pile
            } shouldBe true
        }
    }
})
