package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.MarchOfBurgeoningLife
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * March of Burgeoning Life — {X}{G} instant: "you may exile any number of green cards from your
 * hand. This spell costs {2} less to cast for each card exiled this way. Choose target creature with
 * mana value less than X. Search your library for a creature card with the same name as that
 * creature, put it onto the battlefield tapped, then shuffle."
 *
 * Pins the composition: the library search names the *target* (`sharingNameWith(target)`), and the
 * X bound on the target is strict ("less than X").
 */
class MarchOfBurgeoningLifeScenarioTest : FunSpec({

    val beast = card("Test Three Drop") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Beast"
        power = 3
        toughness = 3
    }

    val otherBeast = card("Test Other Three Drop") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Beast"
        power = 3
        toughness = 3
    }

    val greenCard = card("Test Green Card") {
        manaCost = "{G}"
        typeLine = "Creature — Elf"
        power = 1
        toughness = 1
    }

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MarchOfBurgeoningLife, beast, otherBeast, greenCard))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.battlefieldIdsNamed(name: String) =
        state.getBattlefield().filter { state.getEntity(it)?.get<CardComponent>()?.name == name }

    test("X = 4 with one green card exiled finds only the same-named creature and puts it in tapped") {
        val d = setup()
        val me = d.activePlayer!!
        val original = d.putCreatureOnBattlefield(me, "Test Three Drop")
        val decoy = d.putCardOnTopOfLibrary(me, "Test Other Three Drop")
        val copy = d.putCardOnTopOfLibrary(me, "Test Three Drop")
        val march = d.putCardInHand(me, "March of Burgeoning Life")
        val fodder = d.putCardInHand(me, "Test Green Card")
        d.giveMana(me, Color.GREEN, 3)

        val cast = d.submit(CastSpell(
            me, march, targets = listOf(ChosenTarget.Permanent(original)), xValue = 4,
            additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(fodder)),
            paymentStrategy = PaymentStrategy.FromPool,
        ))
        cast.outcome shouldBe Outcome.Done
        d.bothPass()

        val select = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        select.options shouldContainExactly listOf(copy)
        d.submitCardSelection(me, listOf(copy))

        val beasts = d.battlefieldIdsNamed("Test Three Drop")
        beasts.size shouldBe 2
        val fetched = beasts.single { it != original }
        d.isTapped(fetched) shouldBe true
        d.isTapped(original) shouldBe false
        d.battlefieldIdsNamed("Test Other Three Drop") shouldBe emptyList()
        (decoy in d.state.getBattlefield()) shouldBe false
        d.getExileCardNames(me) shouldBe listOf("Test Green Card")
    }

    test("an opponent's creature names the card searched for in your own library") {
        val d = setup()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        val theirs = d.putCreatureOnBattlefield(opponent, "Test Three Drop")
        val copy = d.putCardOnTopOfLibrary(me, "Test Three Drop")
        val march = d.putCardInHand(me, "March of Burgeoning Life")
        d.giveMana(me, Color.GREEN, 5)

        val cast = d.submit(CastSpell(
            me, march, targets = listOf(ChosenTarget.Permanent(theirs)), xValue = 4,
            paymentStrategy = PaymentStrategy.FromPool,
        ))
        cast.outcome shouldBe Outcome.Done
        d.bothPass()

        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().options shouldContainExactly listOf(copy)
        d.submitCardSelection(me, listOf(copy))

        d.findPermanent(me, "Test Three Drop") shouldBe d.battlefieldIdsNamed("Test Three Drop").single { it != theirs }
    }

    test("the bound is strict — a mana value 3 creature can't be targeted with X = 3") {
        val d = setup()
        val me = d.activePlayer!!
        val original = d.putCreatureOnBattlefield(me, "Test Three Drop")
        val march = d.putCardInHand(me, "March of Burgeoning Life")
        d.giveMana(me, Color.GREEN, 4)

        val cast = d.submit(CastSpell(
            me, march, targets = listOf(ChosenTarget.Permanent(original)), xValue = 3,
            paymentStrategy = PaymentStrategy.FromPool,
        ))
        (cast.outcome is Outcome.Rejected) shouldBe true
    }
})
