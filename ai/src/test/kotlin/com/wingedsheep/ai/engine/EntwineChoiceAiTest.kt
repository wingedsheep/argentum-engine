package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh1.cards.KayasGuile
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Entwine (CR 702.42a) is offered as a second `CastSpellModal` variant beside the plain cast, so the
 * AI weighs "two modes for {1}{W}{B}" against "all four for {1}{W}{B} + {3}" like any two actions —
 * and must only ever submit a mode count the variant it picked allows.
 */
class EntwineChoiceAiTest : FunSpec({

    val cards = TestCards.all + listOf(KayasGuile)
    val registry = CardRegistry().apply { register(cards) }

    /** The active player holds Kaya's Guile with [lands] lands; the opponent has a Grizzly Bears. */
    fun board(lands: Int): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver().apply {
            registerCards(cards)
            initMirrorMatch(deck = Deck.of("Plains" to 20, "Swamp" to 20), skipMulligans = true, startingPlayer = 1)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val ai = d.state.activePlayerId!!
        val opponent = if (ai == d.player1) d.player2 else d.player1
        d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        repeat(lands - 1) { d.putLandOnBattlefield(ai, "Swamp") }
        d.putLandOnBattlefield(ai, "Plains")
        d.putCardInHand(ai, "Kaya's Guile")
        return d to ai
    }

    fun aiCast(d: GameTestDriver, ai: EntityId): CastSpell {
        val offered = d.legalActions(ai).filter { it.description.startsWith("Cast Kaya's Guile") }
        return AIPlayer.create(registry, ai, AiProfile.LIVE).chooseFrom(d.state, offered).action
            .shouldBeInstanceOf<CastSpell>()
    }

    test("with only three lands the AI casts the plain variant with exactly two modes") {
        val (d, ai) = board(lands = 3)
        val cast = aiCast(d, ai)
        cast.declaredCostSlot shouldBe null
        cast.chosenModes.toSet().size shouldBe 2
        cast.chosenModes.size shouldBe 2
        d.submit(cast).error shouldBe null
    }

    test("with six lands every cast it can pick is a legal two- or four-mode cast") {
        val (d, ai) = board(lands = 6)
        val cast = aiCast(d, ai)
        val entwined = cast.declaredCostSlot == ChoiceSlot.ENTWINED
        cast.chosenModes.size shouldBe (if (entwined) 4 else 2)
        cast.chosenModes.toSet().size shouldBe cast.chosenModes.size
        d.submit(cast).error shouldBe null
    }
})
