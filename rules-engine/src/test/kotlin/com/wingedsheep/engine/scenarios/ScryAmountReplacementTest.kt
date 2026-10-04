package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.ScriedEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyScryAmount
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * [ModifyScryAmount] (CR 701.22): "If you would scry a number of cards, scry that many cards plus
 * one instead." Applied once at the scry pipeline's gather, so the look, the top/bottom choice and
 * the [ScriedEvent] count all see the modified number; a scry 0 is no scry event (CR 701.22b) and
 * is never modified.
 */
class ScryAmountReplacementTest : FunSpec({

    fun scrySpell(name: String, n: Int) = card(name) {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Patterns.Library.scry(n) }
    }
    val scryThree = scrySpell("Scry Three", 3)
    val scryZero = scrySpell("Scry Zero", 0)
    val scryX = card("Scry X") {
        manaCost = "{X}"
        typeLine = "Sorcery"
        spell { effect = Effects.Scry(DynamicAmounts.xValue()) }
    }
    val targetScryX = card("Target Scry X") {
        manaCost = "{X}"
        typeLine = "Sorcery"
        spell {
            val player = target(Targets.Player)
            effect = Effects.Scry(DynamicAmounts.xValue(), player)
        }
    }
    val yourLens = card("Your Scry Lens") {
        manaCost = "{0}"
        typeLine = "Artifact"
        replacementEffect(ModifyScryAmount(modifier = 1))
    }
    val opponentsLens = card("Opponents' Scry Lens") {
        manaCost = "{0}"
        typeLine = "Artifact"
        replacementEffect(ModifyScryAmount(modifier = 2, appliesTo = EventPattern.ScryEvent(Player.EachOpponent)))
    }

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(scryThree, scryZero, scryX, targetScryX, yourLens, opponentsLens))
        initMirrorMatch(deck = Deck.of("Mountain" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Answer every scry decision by keeping all cards on top; returns each look's option count. */
    fun GameTestDriver.answerScries(): List<Int> {
        val looks = mutableListOf<Int>()
        repeat(6) {
            when (val decision = pendingDecision) {
                is SelectCardsDecision -> {
                    looks += decision.options.size
                    decision.maxSelections shouldBe decision.options.size
                    submitDecision(decision.playerId, CardsSelectedResponse(decision.id, emptyList()))
                }
                is ReorderLibraryDecision ->
                    submitDecision(decision.playerId, OrderedResponse(decision.id, decision.cards))
                else -> return looks
            }
        }
        return looks
    }

    fun GameTestDriver.scriedCounts(before: Int): List<Int> =
        events.drop(before).filterIsInstance<ScriedEvent>().map { it.count }

    fun GameTestDriver.cast(player: EntityId, name: String): List<Int> {
        castSpell(player, putCardInHand(player, name))
        bothPass()
        return answerScries()
    }

    fun GameTestDriver.castX(player: EntityId, name: String, x: Int, targets: List<EntityId> = emptyList()): List<Int> {
        giveMana(player, Color.RED, x)
        castXSpell(player, putCardInHand(player, name), x, targets)
        bothPass()
        return answerScries()
    }

    test("a literal scry looks at one more card and reports it") {
        val d = setup()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(you, "Your Scry Lens")

        val before = d.events.size
        d.cast(you, "Scry Three") shouldBe listOf(4)
        d.scriedCounts(before) shouldBe listOf(4)
    }

    test("a dynamic scry X is modified too") {
        val d = setup()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(you, "Your Scry Lens")

        val before = d.events.size
        d.castX(you, "Scry X", 2) shouldBe listOf(3)
        d.scriedCounts(before) shouldBe listOf(3)
    }

    test("scry 0 is no scry event and is not modified") {
        val d = setup()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(you, "Your Scry Lens")

        val before = d.events.size
        d.cast(you, "Scry Zero").shouldBeEmpty()
        d.scriedCounts(before).shouldBeEmpty()
    }

    test("scry X with X = 0 stays a non-event") {
        val d = setup()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(you, "Your Scry Lens")

        val before = d.events.size
        d.castX(you, "Scry X", 0).shouldBeEmpty()
        d.scriedCounts(before).shouldBeEmpty()
    }

    test("the scrying player, not the caster, decides whose replacements apply") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(you, "Your Scry Lens")

        // You make your opponent scry: your "if you would scry" lens doesn't apply to them.
        d.castX(you, "Target Scry X", 2, listOf(opponent)) shouldBe listOf(2)
    }

    test("an opponent-scoped replacement modifies only opponents' scries") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(you, "Opponents' Scry Lens")

        // Your own scry first: once the opponent answers their scry, priority sits with them.
        d.cast(you, "Scry Three") shouldBe listOf(3)
        d.castX(you, "Target Scry X", 1, listOf(opponent)) shouldBe listOf(3)
    }
})
