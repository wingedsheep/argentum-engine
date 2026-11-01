package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.war.cards.FinaleOfPromise
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Finale of Promise (WAR #127) — {X}{R}{R} Sorcery.
 *
 *   You may cast up to one target instant card and/or up to one target sorcery card from your
 *   graveyard each with mana value X or less without paying their mana costs. If a spell cast this
 *   way would be put into your graveyard, exile it instead. If X is 10 or more, copy each of those
 *   spells twice. You may choose new targets for the copies.
 *
 * Exercises the composition: per-target X caps, the free-cast loop taking the targets in the
 * caster's order, the exile-instead rider, and "those spells" (targets minus what is still in the
 * graveyard) each copied twice at X ≥ 10.
 */
class FinaleOfPromiseScenarioTest : FunSpec({

    val instant = card("Test Quickening") {
        manaCost = "{1}{W}"
        colorIdentity = "W"
        typeLine = "Instant"
        oracleText = "You gain 2 life."
        spell { effect = Effects.GainLife(2) }
    }
    val sorcery = card("Test Blessing") {
        manaCost = "{2}{W}"
        colorIdentity = "W"
        typeLine = "Sorcery"
        oracleText = "You gain 3 life."
        spell { effect = Effects.GainLife(3) }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(FinaleOfPromise)
        registerCard(instant)
        registerCard(sorcery)
        initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.nameOf(id: EntityId): String? = state.getEntity(id)?.get<CardComponent>()?.name

    fun GameTestDriver.offered(): Set<String> =
        (state.pendingDecision as SelectCardsDecision).options.mapNotNull { nameOf(it) }.toSet()

    fun GameTestDriver.pick(name: String) {
        val decision = state.pendingDecision as SelectCardsDecision
        submitCardSelection(player1, listOf(decision.options.first { nameOf(it) == name })).error shouldBe null
    }

    fun GameTestDriver.resolveStack() {
        repeat(40) {
            if (state.pendingDecision != null || stackSize == 0) return
            bothPass()
        }
    }

    fun GameTestDriver.castFinale(x: Int, targets: List<EntityId>) = run {
        val finale = putCardInHand(player1, "Finale of Promise")
        giveMana(player1, Color.RED, 2 + x)
        submit(
            CastSpell(
                playerId = player1,
                cardId = finale,
                targets = targets.map { ChosenTarget.Card(it, player1, Zone.GRAVEYARD) },
                xValue = x,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
    }

    test("casts both targets free in the chosen order and exiles them; no copies below X 10") {
        val d = driver()
        val quickening = d.putCardInGraveyard(d.player1, "Test Quickening")
        val blessing = d.putCardInGraveyard(d.player1, "Test Blessing")
        d.castFinale(3, listOf(quickening, blessing)).error shouldBe null
        d.bothPass()

        withClue("both targets are offered; the sorcery may go first") {
            d.offered() shouldBe setOf("Test Quickening", "Test Blessing")
        }
        d.pick("Test Blessing")
        d.pick("Test Quickening")
        withClue("both spells are on the stack, uncopied") {
            d.state.pendingDecision shouldBe null
            d.getStackSpellNames().toSet() shouldBe setOf("Test Quickening", "Test Blessing")
            d.stackSize shouldBe 2
        }
        d.resolveStack()

        d.getLifeTotal(d.player1) shouldBe 25
        withClue("spells cast this way are exiled; Finale of Promise itself goes to the graveyard") {
            d.getExileCardNames(d.player1).toSet() shouldBe setOf("Test Quickening", "Test Blessing")
            d.getGraveyardCardNames(d.player1) shouldBe listOf("Finale of Promise")
        }
    }

    test("a card with mana value above X can't be targeted") {
        val d = driver()
        val blessing = d.putCardInGraveyard(d.player1, "Test Blessing")
        d.castFinale(2, listOf(blessing)).error shouldNotBe null
    }

    test("X 10 or more copies each spell cast this way twice") {
        val d = driver()
        val quickening = d.putCardInGraveyard(d.player1, "Test Quickening")
        val blessing = d.putCardInGraveyard(d.player1, "Test Blessing")
        d.castFinale(10, listOf(quickening, blessing)).error shouldBe null
        d.bothPass()

        d.pick("Test Quickening")
        d.pick("Test Blessing")
        withClue("the caster orders the copies") {
            d.offered() shouldBe setOf("Test Quickening", "Test Blessing")
        }
        d.pick("Test Blessing")
        withClue("two originals and four copies") { d.stackSize shouldBe 6 }
        d.resolveStack()

        withClue("each spell resolved three times: 3 × 2 + 3 × 3") {
            d.getLifeTotal(d.player1) shouldBe 35
        }
        d.getExileCardNames(d.player1).toSet() shouldBe setOf("Test Quickening", "Test Blessing")
    }

    test("X 10: a declined target is not copied") {
        val d = driver()
        val quickening = d.putCardInGraveyard(d.player1, "Test Quickening")
        val blessing = d.putCardInGraveyard(d.player1, "Test Blessing")
        d.castFinale(10, listOf(quickening, blessing)).error shouldBe null
        d.bothPass()

        d.pick("Test Blessing")
        withClue("stop after one cast") {
            d.submitCardSelection(d.player1, emptyList()).error shouldBe null
        }
        withClue("only the cast sorcery is copied — no order prompt, three objects on the stack") {
            d.state.pendingDecision shouldBe null
            d.stackSize shouldBe 3
        }
        d.resolveStack()

        d.getLifeTotal(d.player1) shouldBe 29
        d.getGraveyardCardNames(d.player1).toSet() shouldBe setOf("Test Quickening", "Finale of Promise")
        d.getExileCardNames(d.player1) shouldBe listOf("Test Blessing")
    }
})
