package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.player.MayCastCreaturesFromGraveyardWithForageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.BonebindOrator
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.OsteomancerAdept
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

class OsteomancerAdeptForageCastTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(OsteomancerAdept, BonebindOrator, PredefinedTokens.Food))
        return driver
    }

    test("Tapping Osteomancer Adept grants MayCastCreaturesFromGraveyardWithForage permission") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val adept = driver.putCreatureOnBattlefield(active, "Osteomancer Adept")
        driver.removeSummoningSickness(adept)

        val abilityId = OsteomancerAdept.activatedAbilities.first().id
        driver.submitSuccess(ActivateAbility(playerId = active, sourceId = adept, abilityId = abilityId))
        driver.bothPass() // resolve the ability

        val playerEntity = driver.state.getEntity(active)!!
        playerEntity.has<MayCastCreaturesFromGraveyardWithForageComponent>() shouldBe true
    }

    test("Casting a creature from graveyard via forage with a Food in play succeeds (regression: card not in hand)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val adept = driver.putCreatureOnBattlefield(active, "Osteomancer Adept")
        driver.removeSummoningSickness(adept)
        val food = driver.putPermanentOnBattlefield(active, "Food")
        val orator = driver.putCardInGraveyard(active, "Bonebind Orator")

        val abilityId = OsteomancerAdept.activatedAbilities.first().id
        driver.submitSuccess(ActivateAbility(playerId = active, sourceId = adept, abilityId = abilityId))
        driver.bothPass() // resolve the ability — permission granted

        // Provide {1}{B} for Bonebind Orator's mana cost
        driver.giveMana(active, Color.BLACK, 2)

        // Cast Bonebind Orator from the graveyard via the forage permission
        val castResult = driver.castSpell(active, orator)
        castResult.isSuccess shouldBe true

        // Food sacrificed as the forage cost
        driver.state.getZone(com.wingedsheep.engine.state.ZoneKey(active, Zone.GRAVEYARD)) shouldContain food
        driver.state.getBattlefield(active) shouldNotContain food

        // Orator is on the stack (removed from graveyard)
        driver.state.getZone(com.wingedsheep.engine.state.ZoneKey(active, Zone.GRAVEYARD)) shouldNotContain orator
        driver.state.stack shouldContain orator
    }

    test("Casting from graveyard via forage with no Food exiles 3 other graveyard cards") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val adept = driver.putCreatureOnBattlefield(active, "Osteomancer Adept")
        driver.removeSummoningSickness(adept)
        val orator = driver.putCardInGraveyard(active, "Bonebind Orator")
        val filler = (1..3).map { driver.putCardInGraveyard(active, "Swamp") }

        val abilityId = OsteomancerAdept.activatedAbilities.first().id
        driver.submitSuccess(ActivateAbility(playerId = active, sourceId = adept, abilityId = abilityId))
        driver.bothPass()

        driver.giveMana(active, Color.BLACK, 2)
        val castResult = driver.castSpell(active, orator)
        castResult.isSuccess shouldBe true

        val exile = driver.state.getZone(com.wingedsheep.engine.state.ZoneKey(active, Zone.EXILE))
        exile.size shouldBe 3
        filler.forEach { exile shouldContain it }
        // The creature itself is on the stack, not exiled.
        exile shouldNotContain orator
        driver.state.stack shouldContain orator
    }

    test("Casting from graveyard via forage fails when no Food and fewer than 3 other graveyard cards") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val adept = driver.putCreatureOnBattlefield(active, "Osteomancer Adept")
        driver.removeSummoningSickness(adept)
        val orator = driver.putCardInGraveyard(active, "Bonebind Orator")
        driver.putCardInGraveyard(active, "Swamp")
        driver.putCardInGraveyard(active, "Swamp")

        val abilityId = OsteomancerAdept.activatedAbilities.first().id
        driver.submitSuccess(ActivateAbility(playerId = active, sourceId = adept, abilityId = abilityId))
        driver.bothPass()

        driver.giveMana(active, Color.BLACK, 2)
        val castResult = driver.castSpell(active, orator)
        castResult.isSuccess shouldBe false
    }
})
