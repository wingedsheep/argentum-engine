package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for the [ManaRestriction.SpellsMV4OrGreater] mana spending restriction.
 *
 * Used by Ashling, Rimebound: "Add two mana of any one color. Spend this mana only to cast
 * spells with mana value 4 or greater."
 */
class SpellsMV4OrGreaterRestrictionTest : FunSpec({

    val cheapInstant = card("Cheap Instant") {
        manaCost = "{2}{R}"
        typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }

    val expensiveSorcery = card("Expensive Sorcery") {
        manaCost = "{3}{R}"
        typeLine = "Sorcery"
        spell { effect = Effects.DrawCards(2) }
    }

    fun createDriver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(cheapInstant, expensiveSorcery))
    }

    test("restricted MV4+ mana cannot pay for a 3-mana spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20), skipMulligans = true)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Pool: 3 unrestricted red mana, all from restricted source.
        driver.giveRestrictedMana(player, Color.RED, 3, ManaRestriction.SpellsMV4OrGreater)

        val cardId = driver.putCardInHand(player, "Cheap Instant")
        val result = driver.submit(
            CastSpell(playerId = player, cardId = cardId, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe false

        val pool = driver.state.getEntity(player)?.get<ManaPoolComponent>()!!
        pool.restrictedMana.size shouldBe 3
    }

    test("restricted MV4+ mana CAN pay for a 4-mana spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20), skipMulligans = true)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveRestrictedMana(player, Color.RED, 4, ManaRestriction.SpellsMV4OrGreater)

        val cardId = driver.putCardInHand(player, "Expensive Sorcery")
        val result = driver.submit(
            CastSpell(playerId = player, cardId = cardId, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe true

        val pool = driver.state.getEntity(player)?.get<ManaPoolComponent>()!!
        pool.restrictedMana.size shouldBe 0
    }
})
