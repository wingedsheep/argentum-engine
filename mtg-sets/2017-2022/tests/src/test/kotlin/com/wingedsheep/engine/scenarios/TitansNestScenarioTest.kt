package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.iko.cards.TitansNest
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Titans' Nest — "Exile a card from your graveyard: Add {C}. Spend this mana only to cast a spell
 * that's one or more colors without {X} in its mana cost."
 */
class TitansNestScenarioTest : FunSpec({

    val colorlessTrinket = CardDefinition.artifact(name = "Nest Colorless Trinket", manaCost = ManaCost.parse("{1}"))
    val greenX = card("Nest Green X Spell") {
        manaCost = "{X}{G}"
        typeLine = "Sorcery"
        oracleText = "You gain X life."
        spell { effect = Effects.GainLife(1) }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TitansNest, colorlessTrinket, greenX))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Puts the Nest on the battlefield and activates it once, exiling a fresh graveyard card. */
    fun GameTestDriver.makeNestMana(playerId: EntityId) {
        val nest = putPermanentOnBattlefield(playerId, "Titans' Nest")
        val fodder = putCardInGraveyard(playerId, "Forest")
        submitSuccess(
            ActivateAbility(
                playerId,
                nest,
                TitansNest.activatedAbilities.single().id,
                costPayment = AdditionalCostPayment(exiledCards = listOf(fodder)),
            )
        )
        getExile(playerId) shouldContain fodder
        state.getEntity(playerId)!!.get<ManaPoolComponent>()!!.restrictedMana.size shouldBe 1
    }

    test("the mana pays the generic part of a colored spell") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.makeNestMana(me)
        driver.giveMana(me, Color.GREEN, 1)
        val bears = driver.putCardInHand(me, "Grizzly Bears")

        val result = driver.submit(CastSpell(playerId = me, cardId = bears, paymentStrategy = PaymentStrategy.FromPool))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.state.getEntity(me)!!.get<ManaPoolComponent>()!!.restrictedMana.size shouldBe 0
    }

    test("the mana can't pay for a colorless spell") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.makeNestMana(me)
        val trinket = driver.putCardInHand(me, "Nest Colorless Trinket")

        val result = driver.submit(CastSpell(playerId = me, cardId = trinket, paymentStrategy = PaymentStrategy.FromPool))
        result.outcome shouldNotBe Outcome.Done
    }

    test("the mana can't pay for a colored spell with {X} in its mana cost") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.makeNestMana(me)
        driver.giveMana(me, Color.GREEN, 1)
        val xSpell = driver.putCardInHand(me, "Nest Green X Spell")

        val result = driver.submit(
            CastSpell(playerId = me, cardId = xSpell, xValue = 1, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.outcome shouldNotBe Outcome.Done
    }
})
