package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blc.cards.HazelsBrewmaster
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hazel's Brewmaster (BLC #17) — {3}{B} 3/4 Creature — Squirrel Warlock.
 *
 * "Menace / Whenever this creature enters or attacks, exile up to one target card from a graveyard
 *  and create a Food token. / Foods you control have all activated abilities of all creature cards
 *  exiled with this creature."
 */
class HazelsBrewmasterScenarioTest : FunSpec({

    val lifeMystic = card("Brewmaster Test Mystic") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Human Wizard"
        power = 1
        toughness = 1
        oracleText = "{R}: You gain 1 life."
        activatedAbility {
            cost = Costs.Mana("{R}")
            effect = Effects.GainLife(1)
        }
    }
    val mysticAbilityId = lifeMystic.activatedAbilities[0].id

    // Same ability on a noncreature card: "all creature cards exiled" must not lend it.
    val lifeTrinket = card("Brewmaster Test Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "{R}: You gain 1 life."
        activatedAbility {
            cost = Costs.Mana("{R}")
            effect = Effects.GainLife(1)
        }
    }
    val trinketAbilityId = lifeTrinket.activatedAbilities[0].id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + PredefinedTokens.allTokens + listOf(HazelsBrewmaster, lifeMystic, lifeTrinket))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun attackWith(driver: GameTestDriver, brewmaster: EntityId, targets: List<EntityId>) {
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(brewmaster), driver.player2)
        driver.submitTargetSelection(driver.player1, targets)
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()
    }

    test("attack trigger exiles a creature card from an opponent's graveyard; Foods gain its activated ability") {
        val driver = newDriver()
        val me = driver.player1

        val brewmaster = driver.putCreatureOnBattlefield(me, "Hazel's Brewmaster")
        driver.removeSummoningSickness(brewmaster)
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val mystic = driver.putCardInGraveyard(driver.player2, "Brewmaster Test Mystic")

        attackWith(driver, brewmaster, listOf(mystic))

        driver.state.getZone(driver.player2, Zone.EXILE).contains(mystic) shouldBe true
        val food = driver.findPermanent(me, "Food").shouldNotBeNull()

        // The Food has the exiled creature's "{R}: You gain 1 life."
        driver.giveMana(me, Color.RED, 1)
        val lifeBefore = driver.getLifeTotal(me)
        driver.submit(ActivateAbility(playerId = me, sourceId = food, abilityId = mysticAbilityId))
            .outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()
        driver.getLifeTotal(me) shouldBe lifeBefore + 1

        // A non-Food creature you control does not gain it.
        driver.giveMana(me, Color.RED, 1)
        driver.submit(ActivateAbility(playerId = me, sourceId = bears, abilityId = mysticAbilityId))
            .outcome shouldNotBe Outcome.Done
    }

    test("choosing no target still creates a Food, which gains nothing") {
        val driver = newDriver()
        val me = driver.player1

        val brewmaster = driver.putCreatureOnBattlefield(me, "Hazel's Brewmaster")
        driver.removeSummoningSickness(brewmaster)
        val mystic = driver.putCardInGraveyard(me, "Brewmaster Test Mystic")

        attackWith(driver, brewmaster, emptyList())

        driver.state.getZone(me, Zone.GRAVEYARD).contains(mystic) shouldBe true
        val food = driver.findPermanent(me, "Food").shouldNotBeNull()

        driver.giveMana(me, Color.RED, 1)
        driver.submit(ActivateAbility(playerId = me, sourceId = food, abilityId = mysticAbilityId))
            .outcome shouldNotBe Outcome.Done
    }

    test("a noncreature card exiled with it lends nothing") {
        val driver = newDriver()
        val me = driver.player1

        val brewmaster = driver.putCreatureOnBattlefield(me, "Hazel's Brewmaster")
        driver.removeSummoningSickness(brewmaster)
        val trinket = driver.putCardInGraveyard(me, "Brewmaster Test Trinket")

        attackWith(driver, brewmaster, listOf(trinket))

        driver.state.getZone(me, Zone.EXILE).contains(trinket) shouldBe true
        val food = driver.findPermanent(me, "Food").shouldNotBeNull()

        driver.giveMana(me, Color.RED, 1)
        driver.submit(ActivateAbility(playerId = me, sourceId = food, abilityId = trinketAbilityId))
            .outcome shouldNotBe Outcome.Done
    }
})
