package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Clone
import com.wingedsheep.mtg.sets.definitions.lea.cards.SerraAngel
import com.wingedsheep.mtg.sets.definitions.sld.cards.DeadpoolTradingCard
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Deadpool, Trading Card (SLD #1753) — {2}{B}{R} 5/3 Legendary Creature — Mutant Mercenary Hero.
 *
 * "As Deadpool enters, you may exchange his text box and another creature's.
 *  At the beginning of your upkeep, you lose 3 life.
 *  {3}, Sacrifice this creature: Each other player draws a card."
 *
 * Exchanging text boxes (CR 612.5, 701.12h) swaps rules text only: names, power/toughness and
 * types stay put. The exchange outlives either side leaving (the one that leaves is a new object
 * with its own text, CR 400.7), and it is not a copiable value (CR 707.2).
 */
class DeadpoolTradingCardScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(DeadpoolTradingCard, SerraAngel, Clone))
        d.initMirrorMatch(
            deck = Deck.of("Mountain" to 30, "Grizzly Bears" to 30),
            skipMulligans = true,
            startingPlayer = 0
        )
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.castDeadpool(exchangeWith: EntityId?): EntityId {
        val deadpool = putCardInHand(player1, "Deadpool, Trading Card")
        giveMana(player1, Color.BLACK, 2)
        giveMana(player1, Color.RED, 2)
        submit(CastSpell(playerId = player1, cardId = deadpool))
        bothPass()
        val sel = pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        submitDecision(player1, CardsSelectedResponse(decisionId = sel.id, selectedCards = listOfNotNull(exchangeWith)))
        isPaused shouldBe false
        return findPermanent(player1, "Deadpool, Trading Card").shouldNotBeNull()
    }

    fun GameTestDriver.oracle(id: EntityId): String = state.getEntity(id)!!.get<CardComponent>()!!.oracleText

    test("exchanging moves only the rules text: keywords and abilities swap, names and stats stay") {
        val d = newDriver()
        val angel = d.putCreatureOnBattlefield(d.player2, "Serra Angel")

        val deadpool = d.castDeadpool(exchangeWith = angel)

        val projected = d.state.projectedState
        withClue("Deadpool keeps his name and 5/3 body but has Serra Angel's text") {
            d.getCardName(deadpool) shouldBe "Deadpool, Trading Card"
            projected.getPower(deadpool) shouldBe 5
            projected.getToughness(deadpool) shouldBe 3
            projected.hasKeyword(deadpool, Keyword.FLYING) shouldBe true
            projected.hasKeyword(deadpool, Keyword.VIGILANCE) shouldBe true
            d.oracle(deadpool) shouldBe SerraAngel.oracleText
        }
        withClue("Serra Angel keeps her name and 4/4 body but loses flying and vigilance") {
            d.getCardName(angel) shouldBe "Serra Angel"
            projected.getPower(angel) shouldBe 4
            projected.hasKeyword(angel, Keyword.FLYING) shouldBe false
            projected.hasKeyword(angel, Keyword.VIGILANCE) shouldBe false
            d.oracle(angel) shouldStartWith "As Deadpool enters"
        }

        withClue("the upkeep life loss now belongs to the Angel's controller") {
            d.passPriorityUntil(Step.DRAW, d.player2)
            d.getLifeTotal(d.player2) shouldBe 17
            d.passPriorityUntil(Step.DRAW, d.player1)
            d.getLifeTotal(d.player1) shouldBe 20
        }
    }

    test("declining the exchange keeps Deadpool's own text and upkeep life loss") {
        val d = newDriver()
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")

        val deadpool = d.castDeadpool(exchangeWith = null)

        d.oracle(deadpool) shouldStartWith "As Deadpool enters"
        d.oracle(bear) shouldBe ""
        d.passPriorityUntil(Step.DRAW, d.player1)
        d.getLifeTotal(d.player1) shouldBe 17
    }

    test("the Angel can use Deadpool's sacrifice ability; the exchange outlives her and her card has its own text") {
        val d = newDriver()
        val angel = d.putCreatureOnBattlefield(d.player2, "Serra Angel")
        val deadpool = d.castDeadpool(exchangeWith = angel)
        val handBefore = d.getHandSize(d.player1)

        d.passPriority(d.player1)
        d.giveColorlessMana(d.player2, 3)
        d.submitSuccess(ActivateAbility(
            playerId = d.player2,
            sourceId = angel,
            abilityId = DeadpoolTradingCard.script.activatedAbilities.single().id,
        ))
        d.bothPass()

        withClue("Each other player — Deadpool's controller — draws; the Angel is sacrificed") {
            d.getHandSize(d.player1) shouldBe handBefore + 1
            d.getGraveyard(d.player2) shouldContain angel
        }
        withClue("off the battlefield the Angel is a new object with her own text (CR 400.7)") {
            d.oracle(angel) shouldBe SerraAngel.oracleText
        }
        withClue("Deadpool keeps the text he got") {
            d.state.projectedState.hasKeyword(deadpool, Keyword.FLYING) shouldBe true
            d.passPriorityUntil(Step.DRAW, d.player1)
            d.getLifeTotal(d.player1) shouldBe 20
        }
    }

    test("a copy of an exchanged Deadpool copies his printed text, not the exchanged one") {
        val d = newDriver()
        val angel = d.putCreatureOnBattlefield(d.player2, "Serra Angel")
        val deadpool = d.castDeadpool(exchangeWith = angel)

        val clone = d.putCardInHand(d.player1, "Clone")
        d.giveMana(d.player1, Color.BLUE, 4)
        d.submit(CastSpell(playerId = d.player1, cardId = clone))
        d.bothPass()
        val cloneChoice = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(d.player1, CardsSelectedResponse(decisionId = cloneChoice.id, selectedCards = listOf(deadpool)))
        // Entering as a Deadpool, the Clone meets his "as enters" exchange too (CR 614.12); decline.
        val exchangeChoice = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(d.player1, CardsSelectedResponse(decisionId = exchangeChoice.id, selectedCards = emptyList()))

        d.oracle(clone) shouldStartWith "As Deadpool enters"
        d.state.projectedState.hasKeyword(clone, Keyword.FLYING) shouldBe false
        d.state.projectedState.getPower(clone) shouldBe 5
    }
})
