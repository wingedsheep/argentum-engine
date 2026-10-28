package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdc.cards.StewardOfTheHarvest
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Steward of the Harvest (TDC #48) — {3}{G} 3/3 Creature — Human Druid.
 *
 * "When this creature enters, exile up to three target land cards from your graveyard.
 *  Creatures you control have all activated abilities of all land cards exiled with this creature."
 */
class StewardOfTheHarvestScenarioTest : FunSpec({

    val lifeLand = card("Steward Test Sanctuary") {
        manaCost = ""
        typeLine = "Land"
        oracleText = "{1}: You gain 1 life."
        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.GainLife(1)
        }
    }
    val lifeAbilityId = lifeLand.activatedAbilities[0].id

    val gainLand = card("Steward Test Garden") {
        manaCost = ""
        typeLine = "Land"
        oracleText = "{2}: You gain 2 life."
        activatedAbility {
            cost = Costs.Mana("{2}")
            effect = Effects.GainLife(2)
        }
    }
    val gainTwoAbilityId = gainLand.activatedAbilities[0].id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(StewardOfTheHarvest, lifeLand, gainLand))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castSteward(driver: GameTestDriver, targets: List<EntityId>): EntityId {
        val me = driver.player1
        val steward = driver.putCardInHand(me, "Steward of the Harvest")
        driver.giveMana(me, Color.GREEN, 4)
        driver.castSpell(me, steward).outcome shouldBe Outcome.Done
        driver.bothPass() // Steward resolves; ETB trigger asks for targets
        driver.submitTargetSelection(me, targets)
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()
        return steward
    }

    fun activate(driver: GameTestDriver, source: EntityId, abilityId: AbilityId, mana: Int): Outcome {
        val me = driver.player1
        driver.giveMana(me, Color.GREEN, mana)
        val outcome = driver.submit(ActivateAbility(playerId = me, sourceId = source, abilityId = abilityId)).outcome
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()
        return outcome
    }

    test("exiles up to three land cards; every creature you control gains their activated abilities") {
        val driver = newDriver()
        val me = driver.player1
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val sanctuary = driver.putCardInGraveyard(me, "Steward Test Sanctuary")
        val garden = driver.putCardInGraveyard(me, "Steward Test Garden")

        val steward = castSteward(driver, listOf(sanctuary, garden))

        driver.state.getZone(me, Zone.EXILE).contains(sanctuary) shouldBe true
        driver.state.getZone(me, Zone.EXILE).contains(garden) shouldBe true

        val life = driver.getLifeTotal(me)
        activate(driver, bears, lifeAbilityId, 1) shouldBe Outcome.Done
        driver.getLifeTotal(me) shouldBe life + 1
        activate(driver, steward, gainTwoAbilityId, 2) shouldBe Outcome.Done
        driver.getLifeTotal(me) shouldBe life + 3
    }

    test("choosing no targets lends nothing") {
        val driver = newDriver()
        val me = driver.player1
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val sanctuary = driver.putCardInGraveyard(me, "Steward Test Sanctuary")

        castSteward(driver, emptyList())

        driver.state.getZone(me, Zone.GRAVEYARD).contains(sanctuary) shouldBe true
        activate(driver, bears, lifeAbilityId, 1) shouldNotBe Outcome.Done
    }

    test("creatures lose the abilities once Steward leaves the battlefield") {
        val driver = newDriver()
        val me = driver.player1
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val sanctuary = driver.putCardInGraveyard(me, "Steward Test Sanctuary")

        val steward = castSteward(driver, listOf(sanctuary))
        driver.moveToGraveyard(steward)

        activate(driver, bears, lifeAbilityId, 1) shouldNotBe Outcome.Done
    }
})
