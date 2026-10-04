package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.inv.cards.Opt
import com.wingedsheep.mtg.sets.definitions.j22.cards.KenessosPriestOfThassa
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ModifyScryAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Kenessos, Priest of Thassa (J22) —
 * If you would scry a number of cards, scry that many cards plus one instead.
 * {3}{G/U}: Look at the top card of your library. If it's a Kraken, Leviathan, Octopus, or Serpent
 * creature card, you may put it onto the battlefield. If you don't put the card onto the
 * battlefield, you may put it on the bottom of your library.
 */
class KenessosPriestOfThassaScenarioTest : FunSpec({

    val lookAbilityId = KenessosPriestOfThassa.activatedAbilities[0].id

    val seaSerpent = CardDefinition.creature(
        name = "Test Sea Serpent",
        manaCost = ManaCost.parse("{5}{U}"),
        subtypes = setOf(Subtype("Serpent")),
        power = 5,
        toughness = 5
    )

    // A second, non-legendary scry-plus-one source (Kenessos itself would die to the legend rule).
    val scryLens = card("Test Scry Lens") {
        manaCost = "{1}"
        typeLine = "Artifact"
        replacementEffect(ModifyScryAmount(modifier = 1))
    }

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + KenessosPriestOfThassa + Opt + seaSerpent + scryLens)
        initMirrorMatch(deck = Deck.of("Island" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Cast Opt, resolve it, and return how many cards its scry looked at; everything goes to the bottom. */
    fun GameTestDriver.optScryCount(caster: EntityId): Int {
        val opt = putCardInHand(caster, "Opt")
        giveMana(caster, Color.BLUE, 1)
        submit(CastSpell(caster, opt, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
        bothPass()
        val decision = pendingDecision as SelectCardsDecision
        decision.maxSelections shouldBe decision.options.size
        submitCardSelection(caster, decision.options)
        while (state.stack.isNotEmpty()) bothPass()
        return decision.options.size
    }

    fun GameTestDriver.activateLook(you: EntityId, kenessos: EntityId) {
        giveMana(you, Color.BLUE, 4)
        submit(
            ActivateAbility(playerId = you, sourceId = kenessos, abilityId = lookAbilityId, paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("scry 1 becomes scry 2, and every looked-at card may go to the bottom") {
        val d = setup()
        val you = d.activePlayer!!
        d.putCreatureOnBattlefield(you, "Kenessos, Priest of Thassa")
        val top = d.state.getLibrary(you).take(2)

        d.optScryCount(you) shouldBe 2

        d.state.getLibrary(you).takeLast(2).toSet() shouldBe top.toSet()
    }

    test("without Kenessos, Opt scries 1") {
        val d = setup()
        d.optScryCount(d.activePlayer!!) shouldBe 1
    }

    test("scry-plus-one replacements stack") {
        val d = setup()
        val you = d.activePlayer!!
        d.putCreatureOnBattlefield(you, "Kenessos, Priest of Thassa")
        d.putPermanentOnBattlefield(you, "Test Scry Lens")

        d.optScryCount(you) shouldBe 3
    }

    test("an opponent's Kenessos does not change your scry") {
        val d = setup()
        val you = d.activePlayer!!
        d.putCreatureOnBattlefield(d.getOpponent(you), "Kenessos, Priest of Thassa")

        d.optScryCount(you) shouldBe 1
    }

    test("kept cards are ordered back on top") {
        val d = setup()
        val you = d.activePlayer!!
        d.putCreatureOnBattlefield(you, "Kenessos, Priest of Thassa")
        val top = d.state.getLibrary(you).take(2)
        val opt = d.putCardInHand(you, "Opt")
        d.giveMana(you, Color.BLUE, 1)
        d.submit(CastSpell(you, opt, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
        d.bothPass()

        d.submitCardSelection(you, emptyList())
        val reorder = d.pendingDecision as ReorderLibraryDecision
        reorder.cards.toSet() shouldBe top.toSet()
        d.submitOrderedResponse(you, top.reversed())
        while (d.state.stack.isNotEmpty()) d.bothPass()

        // Opt drew the new top card — the one that was second before the scry.
        d.getHand(you) shouldContain top[1]
        d.state.getLibrary(you).first() shouldBe top[0]
    }

    test("look ability puts a Serpent creature card onto the battlefield") {
        val d = setup()
        val you = d.activePlayer!!
        val kenessos = d.putCreatureOnBattlefield(you, "Kenessos, Priest of Thassa")
        val serpent = d.putCardOnTopOfLibrary(you, "Test Sea Serpent")

        d.activateLook(you, kenessos)
        val decision = d.pendingDecision as SelectCardsDecision
        decision.options shouldBe listOf(serpent)
        d.submitCardSelection(you, listOf(serpent))
        while (d.isPaused) d.submitCardSelection(you, emptyList())

        d.state.getBattlefield() shouldContain serpent
    }

    test("look ability can't put a non-matching card onto the battlefield, but may bottom it") {
        val d = setup()
        val you = d.activePlayer!!
        val kenessos = d.putCreatureOnBattlefield(you, "Kenessos, Priest of Thassa")
        val bear = d.putCardOnTopOfLibrary(you, "Centaur Courser")

        d.activateLook(you, kenessos)
        val decision = d.pendingDecision as SelectCardsDecision
        decision.options shouldBe listOf(bear)
        d.submitCardSelection(you, listOf(bear)) // the "put on bottom" choice

        d.state.getBattlefield() shouldNotContain bear
        d.state.getLibrary(you).last() shouldBe bear
    }

    test("look ability may leave the card on top") {
        val d = setup()
        val you = d.activePlayer!!
        val kenessos = d.putCreatureOnBattlefield(you, "Kenessos, Priest of Thassa")
        val serpent = d.putCardOnTopOfLibrary(you, "Test Sea Serpent")

        d.activateLook(you, kenessos)
        while (d.isPaused) d.submitCardSelection(you, emptyList())

        d.state.getBattlefield() shouldNotContain serpent
        d.state.getLibrary(you).first() shouldBe serpent
    }
})
