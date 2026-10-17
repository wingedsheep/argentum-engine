package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.InvokeCalamity
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Invoke Calamity (NEO #147) — {1}{R}{R}{R}{R} Instant.
 *
 *   You may cast up to two instant and/or sorcery spells with total mana value 6 or less from your
 *   graveyard and/or hand without paying their mana costs. If those spells would be put into your
 *   graveyard, exile them instead. Exile Invoke Calamity.
 *
 * The first card to cast from a hand and a graveyard through the capped free-cast loop, and the
 * first to combine its two budgets (count and total mana value) with the exile-instead rider.
 */
class InvokeCalamityScenarioTest : FunSpec({

    // Untargeted sorceries of distinct mana values, each gaining a recognisable amount of life.
    // A sorcery is the hard case: it is cast while Invoke Calamity is still on the stack.
    fun lifeSorcery(name: String, cost: String, life: Int) = card(name) {
        manaCost = cost
        colorIdentity = "W"
        typeLine = "Sorcery"
        oracleText = "You gain $life life."
        spell { effect = Effects.GainLife(life) }
    }

    val one = lifeSorcery("Test Mote", "{W}", 1)
    val three = lifeSorcery("Test Blessing", "{2}{W}", 3)
    val five = lifeSorcery("Test Revival", "{4}{W}", 5)
    val oneMore = lifeSorcery("Test Spark", "{W}", 10)
    val instant = card("Test Quickening") {
        manaCost = "{1}{W}"
        colorIdentity = "W"
        typeLine = "Instant"
        oracleText = "You gain 2 life."
        spell { effect = Effects.GainLife(2) }
    }
    val bear = card("Test Bear") {
        manaCost = "{1}{G}"
        colorIdentity = "G"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(InvokeCalamity)
        listOf(one, three, five, oneMore, instant, bear).forEach { registerCard(it) }
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

    /** Cast Invoke Calamity with exactly its own cost in the pool, and resolve it up to the loop. */
    fun GameTestDriver.castCalamity() {
        val calamity = putCardInHand(player1, "Invoke Calamity")
        giveMana(player1, Color.RED, 4)
        giveMana(player1, Color.BLACK, 1)
        castSpell(player1, calamity).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("offers instants and sorceries from both hand and graveyard, casts them free, exiles them and itself") {
        val d = driver()
        d.putCardInHand(d.player1, "Test Blessing")
        d.putCardInHand(d.player1, "Test Bear")
        d.putCardInGraveyard(d.player1, "Test Quickening")
        d.putCardInGraveyard(d.player1, "Test Bear")
        d.castCalamity()

        withClue("both zones are offered, creatures are not") {
            d.offered() shouldBe setOf("Test Blessing", "Test Quickening")
        }
        d.pick("Test Blessing")
        d.pick("Test Quickening")
        withClue("two casts spend the count budget") { d.state.pendingDecision shouldBe null }
        d.resolveStack()

        withClue("both spells resolved without mana — the pool held only Invoke Calamity's cost") {
            d.getLifeTotal(d.player1) shouldBe 25
        }
        withClue("each spell cast this way is exiled instead of going to the graveyard, and so is Invoke Calamity") {
            d.getExileCardNames(d.player1).toSet() shouldBe setOf("Test Blessing", "Test Quickening", "Invoke Calamity")
            d.getGraveyardCardNames(d.player1) shouldBe listOf("Test Bear")
        }
    }

    test("the total mana value budget narrows the offer after each cast") {
        val d = driver()
        d.putCardInGraveyard(d.player1, "Test Revival")
        d.putCardInGraveyard(d.player1, "Test Blessing")
        d.putCardInHand(d.player1, "Test Mote")
        d.castCalamity()

        d.offered() shouldBe setOf("Test Revival", "Test Blessing", "Test Mote")
        d.pick("Test Revival")
        withClue("5 of 6 spent: only the mana value 1 card still fits") {
            d.offered() shouldBe setOf("Test Mote")
        }
        d.pick("Test Mote")
        d.resolveStack()

        d.getLifeTotal(d.player1) shouldBe 26
        withClue("the card that never fit stays in the graveyard") {
            d.getGraveyardCardNames(d.player1) shouldBe listOf("Test Blessing")
        }
    }

    test("the two-spell cap ends the loop even when another card still fits the budget") {
        val d = driver()
        d.putCardInHand(d.player1, "Test Mote")
        d.putCardInHand(d.player1, "Test Spark")
        d.putCardInGraveyard(d.player1, "Test Blessing")
        d.castCalamity()

        d.pick("Test Mote")
        d.pick("Test Blessing")
        withClue("4 of 6 spent, Test Spark fits, but two spells have been cast") {
            d.state.pendingDecision shouldBe null
        }
        d.resolveStack()

        d.getLifeTotal(d.player1) shouldBe 24
        d.getHand(d.player1).mapNotNull { d.nameOf(it) }.filter { it != "Mountain" } shouldBe listOf("Test Spark")
    }

    test("declining casts nothing, leaves the cards where they are, and still exiles Invoke Calamity") {
        val d = driver()
        d.putCardInHand(d.player1, "Test Mote")
        d.putCardInGraveyard(d.player1, "Test Blessing")
        d.castCalamity()

        d.submitCardSelection(d.player1, emptyList()).error shouldBe null
        d.resolveStack()

        d.getLifeTotal(d.player1) shouldBe 20
        d.getHand(d.player1).mapNotNull { d.nameOf(it) }.filter { it != "Mountain" } shouldBe listOf("Test Mote")
        d.getGraveyardCardNames(d.player1) shouldBe listOf("Test Blessing")
        d.getExileCardNames(d.player1) shouldBe listOf("Invoke Calamity")
    }
})
