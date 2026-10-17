package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.SatoruUmezawa
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.dsl.ninjutsuAbility
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class SatoruUmezawaScenarioTest : FunSpec({

    val grantedNinjutsu = ninjutsuAbility("{2}{U}{B}").id

    val printedNinja = card("Test Ninja") {
        manaCost = "{3}{U}"
        typeLine = "Creature — Human Ninja"
        power = 2
        toughness = 2
        ninjutsu("{U}")
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(SatoruUmezawa, printedNinja))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
    }

    /** Satoru plus [attackers] Savannah Lions, all attacking unblocked; priority back to player 1. */
    fun GameTestDriver.unblockedLions(attackers: Int): List<EntityId> {
        putCreatureOnBattlefield(player1, "Satoru Umezawa")
        val lions = List(attackers) { putCreatureOnBattlefield(player1, "Savannah Lions").also(::removeSummoningSickness) }
        val opponent = getOpponent(player1)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, lions.associateWith { opponent }).outcome shouldBe Outcome.Done
        passPriorityUntil(Step.DECLARE_BLOCKERS)
        declareBlockers(opponent, emptyMap()).outcome shouldBe Outcome.Done
        var guard = 0
        while (state.priorityPlayerId != player1 && state.step == Step.DECLARE_BLOCKERS && guard++ < 4) {
            passPriority(state.priorityPlayerId!!)
        }
        return lions
    }

    fun GameTestDriver.activateGranted(card: EntityId, returned: EntityId) {
        giveMana(player1, Color.BLUE, 3)
        giveMana(player1, Color.BLACK, 1)
        submit(
            ActivateAbility(
                playerId = player1,
                sourceId = card,
                abilityId = grantedNinjutsu,
                costPayment = AdditionalCostPayment(bouncedPermanents = listOf(returned)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).outcome shouldBe Outcome.Done
    }

    /** Resolve the stack, keeping the first offered card and counting Satoru's look-at-three prompts. */
    fun GameTestDriver.resolveAll(): Int {
        var looks = 0
        var guard = 0
        while ((state.stack.isNotEmpty() || pendingDecision != null) && guard++ < 20) {
            when (val d = pendingDecision) {
                is SelectCardsDecision -> {
                    looks++
                    d.options.size shouldBe 3
                    submitCardSelection(player1, d.options.take(1))
                }
                is ReorderLibraryDecision -> submitOrderedResponse(player1, d.cards)
                null -> bothPass()
                else -> error("unexpected decision $d")
            }
        }
        return looks
    }

    test("each creature card in hand has ninjutsu {2}{U}{B}; activating it triggers the look at three") {
        val d = driver()
        val (lion) = d.unblockedLions(1)
        val courser = d.putCardInHand(d.player1, "Centaur Courser")
        val handBefore = d.getHand(d.player1).size

        d.activateGranted(courser, lion)
        d.resolveAll() shouldBe 1

        val courserPerm = d.findPermanent(d.player1, "Centaur Courser").shouldNotBeNull()
        d.state.getEntity(courserPerm)?.get<AttackingComponent>()?.defenderId shouldBe d.getOpponent(d.player1)
        withClue("hand: -Courser +Lion +one card kept from the top three") {
            d.getHand(d.player1) shouldContain lion
            d.getHand(d.player1).size shouldBe handBefore + 1
        }
    }

    test("the grant reaches creature cards only") {
        val d = driver()
        d.unblockedLions(1)
        val courser = d.putCardInHand(d.player1, "Centaur Courser")
        val island = d.putCardInHand(d.player1, "Island")
        d.giveMana(d.player1, Color.BLUE, 3)
        d.giveMana(d.player1, Color.BLACK, 1)
        val sources = LegalActionEnumerator.create(d.cardRegistry).enumerate(d.state, d.player1)
            .mapNotNull { it.action as? ActivateAbility }
            .filter { it.abilityId == grantedNinjutsu }
            .map { it.sourceId }
        sources shouldContain courser
        (island in sources) shouldBe false
    }

    test("a printed ninjutsu triggers it too") {
        val d = driver()
        val (lion) = d.unblockedLions(1)
        val ninja = d.putCardInHand(d.player1, "Test Ninja")
        d.giveMana(d.player1, Color.BLUE, 1)
        val offered = LegalActionEnumerator.create(d.cardRegistry).enumerate(d.state, d.player1)
            .mapNotNull { it.action as? ActivateAbility }
            .filter { it.sourceId == ninja }
            .map { it.abilityId }
        offered shouldContain ninjutsuAbility("{U}").id
        d.submit(
            ActivateAbility(
                playerId = d.player1,
                sourceId = ninja,
                abilityId = ninjutsuAbility("{U}").id,
                costPayment = AdditionalCostPayment(bouncedPermanents = listOf(lion)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).outcome shouldBe Outcome.Done
        d.resolveAll() shouldBe 1
        d.findPermanent(d.player1, "Test Ninja").shouldNotBeNull()
    }

    test("triggers only once each turn") {
        val d = driver()
        val (first, second) = d.unblockedLions(2)
        val courser = d.putCardInHand(d.player1, "Centaur Courser")
        val lionsInHand = d.putCardInHand(d.player1, "Savannah Lions")

        d.activateGranted(courser, first)
        d.resolveAll() shouldBe 1
        d.activateGranted(lionsInHand, second)
        d.resolveAll() shouldBe 0
        d.findPermanent(d.player1, "Centaur Courser").shouldNotBeNull()
        withClue("the second ninjutsu still resolved") { d.getCreatures(d.player1).size shouldBe 3 }
    }
})
