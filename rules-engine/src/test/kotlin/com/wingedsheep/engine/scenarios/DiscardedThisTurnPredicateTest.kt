package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * `StatePredicate.DiscardedThisTurn` — "cards in your graveyard that you discarded this turn"
 * (Shadow of the Grave). Pins the object-identity edges of the discard record it reads: an uncast
 * madness card that went exile → graveyard still counts (Shadow of the Grave's ruling), while a
 * madness card that was cast — a new object by the time it resolves into the graveyard (CR 400.7)
 * — and a card put into the graveyard some other way don't.
 */
class DiscardedThisTurnPredicateTest : FunSpec({

    val madnessBoon = card("Madness Boon") {
        manaCost = "{4}{B}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(7) }
        madness("{B}")
    }

    val selfDiscard = card("Self Discard") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell { effect = Effects.Discard(1) }
    }

    val graveRecall = card("Grave Recall") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell {
            effect = Effects.Pipeline {
                val discarded = gather(
                    CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Any.discardedThisTurn())
                )
                toHand(discarded)
            }
        }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(madnessBoon, selfDiscard, graveRecall))
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.discard(player: EntityId, cardId: EntityId) {
        val spell = putCardInHand(player, "Self Discard")
        giveColorlessMana(player, 1)
        submitSuccess(CastSpell(player, spell, paymentStrategy = PaymentStrategy.FromPool))
        bothPass()
        submitCardSelection(player, listOf(cardId))
    }

    fun GameTestDriver.settle() {
        var guard = 0
        while (state.stack.isNotEmpty() && state.pendingDecision == null && guard++ < 20) bothPass()
    }

    fun GameTestDriver.recall(player: EntityId) {
        val spell = putCardInHand(player, "Grave Recall")
        giveColorlessMana(player, 1)
        submitSuccess(CastSpell(player, spell, paymentStrategy = PaymentStrategy.FromPool))
        settle()
    }

    test("a plainly discarded card is found; a card put into the graveyard otherwise is not") {
        val d = driver()
        val p = d.player1
        val discarded = d.putCardInHand(p, "Centaur Courser")
        val elsewhere = d.putCardInGraveyard(p, "Centaur Courser")
        d.discard(p, discarded)
        d.settle()

        d.recall(p)

        d.getHand(p) shouldContain discarded
        d.getGraveyard(p) shouldContain elsewhere
    }

    test("an uncast madness card that went exile to graveyard still counts as discarded") {
        val d = driver()
        val p = d.player1
        val boon = d.putCardInHand(p, "Madness Boon")
        d.discard(p, boon)
        d.settle()
        d.submitYesNo(p, false)
        d.settle()
        d.getGraveyard(p) shouldContain boon

        d.recall(p)

        d.getHand(p) shouldContain boon
    }

    test("a madness card that was cast is a new object once it resolves into the graveyard") {
        val d = driver()
        val p = d.player1
        val boon = d.putCardInHand(p, "Madness Boon")
        d.discard(p, boon)
        d.giveMana(p, Color.BLACK, 1)
        d.settle()
        d.submitYesNo(p, true)
        d.settle()
        d.getGraveyard(p) shouldContain boon

        d.recall(p)

        d.getHand(p) shouldNotContain boon
        d.getGraveyard(p) shouldContain boon
    }
})
