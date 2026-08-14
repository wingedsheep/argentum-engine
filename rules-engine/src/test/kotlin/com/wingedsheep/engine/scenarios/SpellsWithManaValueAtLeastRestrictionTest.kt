package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.basicLand
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for the [ManaRestriction.SpellsWithManaValueAtLeast] mana spending restriction.
 *
 * Used by Ashling, Rimebound: "Add two mana of any one color. Spend this mana only to cast
 * spells with mana value 4 or greater."
 */
class SpellsWithManaValueAtLeastRestrictionTest : FunSpec({

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
        driver.giveRestrictedMana(player, Color.RED, 3, ManaRestriction.SpellsWithManaValueAtLeast(4))

        val cardId = driver.putCardInHand(player, "Cheap Instant")
        val result = driver.submit(
            CastSpell(playerId = player, cardId = cardId, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe false

        val pool = driver.state.getEntity(player)?.get<ManaPoolComponent>()!!
        pool.restrictedMana.size shouldBe 3
    }

    test("tapping a Mountain preserves previously-added restricted mana (Ashling, Rimebound)") {
        // Regression: after Ashling, Rimebound adds 2 restricted RR to the pool, the
        // player should be able to tap a Mountain for {R} without losing the restricted
        // mana. The reported bug: tapping the Mountain replaced the 2 RR restricted with
        // a single unrestricted R.
        val TestMountain = basicLand("Mountain") {}
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TestMountain))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20), skipMulligans = true)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mountain = driver.putPermanentOnBattlefield(player, "Mountain")

        driver.giveRestrictedMana(player, Color.RED, 2, ManaRestriction.SpellsWithManaValueAtLeast(4))

        val before = driver.state.getEntity(player)?.get<ManaPoolComponent>()!!
        before.red shouldBe 0
        before.restrictedMana.size shouldBe 2

        val manaAbilityId = TestMountain.activatedAbilities[0].id
        val result = driver.submit(
            ActivateAbility(playerId = player, sourceId = mountain, abilityId = manaAbilityId)
        )
        result.isSuccess shouldBe true

        val after = driver.state.getEntity(player)?.get<ManaPoolComponent>()!!
        after.red shouldBe 1
        after.restrictedMana.size shouldBe 2
    }

    test("restricted MV4+ mana CAN pay for a 4-mana spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20), skipMulligans = true)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveRestrictedMana(player, Color.RED, 4, ManaRestriction.SpellsWithManaValueAtLeast(4))

        val cardId = driver.putCardInHand(player, "Expensive Sorcery")
        val result = driver.submit(
            CastSpell(playerId = player, cardId = cardId, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe true

        val pool = driver.state.getEntity(player)?.get<ManaPoolComponent>()!!
        pool.restrictedMana.size shouldBe 0
    }
})
