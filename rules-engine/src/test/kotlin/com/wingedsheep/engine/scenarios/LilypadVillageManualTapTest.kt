package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.DruidOfTheSpade
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.LilypadVillage
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Reproduction for manually tapping Lilypad Village to cast Druid of the Spade.
 *
 * Lilypad Village has three activated abilities:
 *   1) {T}: Add {C}.
 *   2) {T}: Add {U}. Spend this mana only to cast a creature spell.
 *   3) {U}, {T}: Surveil 2.
 *
 * Druid of the Spade costs {2}{G}, which is a creature spell, so the {U}
 * produced by ability (2) must be eligible for the generic portion.
 */
class LilypadVillageManualTapTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(LilypadVillage)
        driver.registerCard(DruidOfTheSpade)
        return driver
    }

    test("Explicit payment with two Forests + Lilypad Village can cast Druid of the Spade") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forest1 = driver.putLandOnBattlefield(activePlayer, "Forest")
        val forest2 = driver.putLandOnBattlefield(activePlayer, "Forest")
        val lilypad = driver.putPermanentOnBattlefield(activePlayer, "Lilypad Village")

        val druid = driver.putCardInHand(activePlayer, "Druid of the Spade")

        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = druid,
                paymentStrategy = PaymentStrategy.Explicit(listOf(forest1, forest2, lilypad))
            )
        )
        result.isSuccess shouldBe true
    }

    test("Manual FromPool: tap two Forests and Lilypad's creature-only {U}, then cast Druid of the Spade") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forest1 = driver.putLandOnBattlefield(activePlayer, "Forest")
        val forest2 = driver.putLandOnBattlefield(activePlayer, "Forest")
        val lilypad = driver.putPermanentOnBattlefield(activePlayer, "Lilypad Village")

        // Ability IDs are generated per CardDefinition instance. Each basicLand("Forest") printing
        // has its own AbilityId, and CardRegistry keeps only the last one registered under "Forest".
        // Look up the ability from the registry the driver actually uses, not from TestCards.all.
        val forestAbility = driver.cardRegistry.requireCard("Forest").activatedAbilities[0].id
        val lilypadCreatureOnlyAbility = LilypadVillage.activatedAbilities[1].id

        driver.submit(ActivateAbility(activePlayer, forest1, forestAbility)).isSuccess shouldBe true
        driver.submit(ActivateAbility(activePlayer, forest2, forestAbility)).isSuccess shouldBe true
        driver.submit(ActivateAbility(activePlayer, lilypad, lilypadCreatureOnlyAbility)).isSuccess shouldBe true

        val pool = driver.state.getEntity(activePlayer)?.get<ManaPoolComponent>()
        pool!!.green shouldBe 2
        pool.restrictedMana.size shouldBe 1

        val druid = driver.putCardInHand(activePlayer, "Druid of the Spade")

        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = druid,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
    }
})
