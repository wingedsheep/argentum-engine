package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.entitiesChoice
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.GainKeywordsOfCards
import com.wingedsheep.sdk.scripting.KeywordCardPool
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The delve payment record (CR 702.66a): the cards exiled from the graveyard to pay a spell's delve
 * are recorded on the spell (`SpellOnStackComponent.delvedCards`), carried onto the permanent it
 * becomes under [ChoiceSlot.DELVED_CARDS], counted by `DynamicAmount.CastChoice(DELVED_CARDS)`, and
 * read by `GainKeywordsOfCards(cards = CREATURE_CARDS_EXILED_WITH_DELVE)` (Soulflayer).
 */
class DelvedCardsRecordTest : FunSpec({

    val delveDemon = card("Delve Record Demon") {
        manaCost = "{4}{U}"
        typeLine = "Creature — Demon"
        power = 4
        toughness = 4
        oracleText = "Delve\nIf a creature card with flying was exiled with this creature's delve ability, this creature has flying."
        keywords(Keyword.DELVE)
        staticAbility {
            ability = GainKeywordsOfCards(
                keywords = listOf(Keyword.FLYING),
                cards = KeywordCardPool.CREATURE_CARDS_EXILED_WITH_DELVE,
            )
        }
    }

    val delveDraw = card("Delve Record Draw") {
        manaCost = "{4}{U}"
        typeLine = "Sorcery"
        oracleText = "Delve\nDraw a card for each card exiled with this spell's delve ability."
        keywords(Keyword.DELVE)
        spell { effect = Effects.DrawCards(DynamicAmounts.castChoice(ChoiceSlot.DELVED_CARDS)) }
    }

    val flyer = card("Delve Record Flyer") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Bird"
        power = 1
        toughness = 1
        keywords(Keyword.FLYING)
    }

    val bear = card("Delve Record Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(delveDemon, delveDraw, flyer, bear))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.cast(card: EntityId, delve: List<EntityId>) = submit(
        CastSpell(
            playerId = player1,
            cardId = card,
            paymentStrategy = PaymentStrategy.AutoPay,
            alternativePayment = AlternativePaymentChoice(delvedCards = delve),
        )
    )

    test("the spell records exactly the delved cards, and CastChoice counts them as it resolves") {
        val driver = newDriver()
        val spell = driver.putCardInHand(driver.player1, "Delve Record Draw")
        val a = driver.putCardInGraveyard(driver.player1, "Delve Record Bear")
        val b = driver.putCardInGraveyard(driver.player1, "Delve Record Flyer")
        val kept = driver.putCardInGraveyard(driver.player1, "Delve Record Bear")
        repeat(3) { driver.putLandOnBattlefield(driver.player1, "Island") }
        val handBefore = driver.getHandSize(driver.player1)

        driver.cast(spell, listOf(a, b)).error shouldBe null
        driver.state.getEntity(spell)!!.get<SpellOnStackComponent>()!!.delvedCards.keys shouldBe setOf(a, b)
        driver.getGraveyard(driver.player1).contains(kept) shouldBe true

        driver.bothPass()
        // -1 for the cast spell, +2 for the two delved cards.
        driver.getHandSize(driver.player1) shouldBe handBefore - 1 + 2
    }

    test("the permanent keeps the record; a delved creature card with flying grants flying") {
        val driver = newDriver()
        val demon = driver.putCardInHand(driver.player1, "Delve Record Demon")
        val bird = driver.putCardInGraveyard(driver.player1, "Delve Record Flyer")
        repeat(4) { driver.putLandOnBattlefield(driver.player1, "Island") }

        driver.cast(demon, listOf(bird)).error shouldBe null
        driver.bothPass()

        val permanent = driver.findPermanent(driver.player1, "Delve Record Demon")!!
        driver.state.getEntity(permanent)!!.entitiesChoice(ChoiceSlot.DELVED_CARDS)!!.entityIds shouldBe setOf(bird)
        driver.state.projectedState.hasKeyword(permanent, Keyword.FLYING) shouldBe true
    }

    test("'was exiled' is history: the delved card leaving exile doesn't take the keyword away") {
        val driver = newDriver()
        val demon = driver.putCardInHand(driver.player1, "Delve Record Demon")
        val bird = driver.putCardInGraveyard(driver.player1, "Delve Record Flyer")
        repeat(4) { driver.putLandOnBattlefield(driver.player1, "Island") }
        driver.cast(demon, listOf(bird)).error shouldBe null
        driver.bothPass()

        driver.replaceState(
            driver.state
                .removeFromZone(ZoneKey(driver.player1, Zone.EXILE), bird)
                .addToZone(ZoneKey(driver.player1, Zone.HAND), bird)
        )
        val permanent = driver.findPermanent(driver.player1, "Delve Record Demon")!!
        driver.state.projectedState.hasKeyword(permanent, Keyword.FLYING) shouldBe true
    }

    test("a cast paid without delve records nothing, even with a flyer in the graveyard") {
        val driver = newDriver()
        val demon = driver.putCardInHand(driver.player1, "Delve Record Demon")
        driver.putCardInGraveyard(driver.player1, "Delve Record Flyer")
        repeat(5) { driver.putLandOnBattlefield(driver.player1, "Island") }

        driver.cast(demon, emptyList()).error shouldBe null
        driver.state.getEntity(demon)!!.get<SpellOnStackComponent>()!!.delvedCards shouldBe emptyMap()
        driver.bothPass()

        val permanent = driver.findPermanent(driver.player1, "Delve Record Demon")!!
        driver.state.getEntity(permanent)!!.entitiesChoice(ChoiceSlot.DELVED_CARDS) shouldBe null
        driver.state.projectedState.hasKeyword(permanent, Keyword.FLYING) shouldBe false
    }

    test("a delved creature card without the keyword grants nothing") {
        val driver = newDriver()
        val demon = driver.putCardInHand(driver.player1, "Delve Record Demon")
        val bear1 = driver.putCardInGraveyard(driver.player1, "Delve Record Bear")
        repeat(4) { driver.putLandOnBattlefield(driver.player1, "Island") }
        driver.cast(demon, listOf(bear1)).error shouldBe null
        driver.bothPass()

        val permanent = driver.findPermanent(driver.player1, "Delve Record Demon")
        permanent shouldNotBe null
        driver.state.projectedState.hasKeyword(permanent!!, Keyword.FLYING) shouldBe false
    }
})
