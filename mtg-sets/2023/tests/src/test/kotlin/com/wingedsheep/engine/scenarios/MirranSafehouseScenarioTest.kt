package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.MirranSafehouse
import com.wingedsheep.mtg.sets.definitions.rav.cards.WateryGrave
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Mirran Safehouse — {3} Artifact (Phyrexia: All Will Be One #232).
 *
 * "As long as this artifact is on the battlefield, it has all activated abilities of all land
 *  cards in all graveyards."
 *
 * The `DonorCards.ALL_GRAVEYARDS` arm of `HasAllActivatedAbilitiesOfCards`: land cards in *any*
 * graveyard donate, nonland cards don't, and a land's intrinsic basic-land-type mana ability is
 * donated even when its definition declares none (the Plains ruling; Watery Grave here).
 */
class MirranSafehouseScenarioTest : FunSpec({

    val outpost = card("Safehouse Test Outpost") {
        typeLine = "Land"
        oracleText = "{T}: You gain 2 life."
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.GainLife(2)
        }
    }
    val outpostAbilityId = outpost.activatedAbilities[0].id

    // Same ability on a nonland card — the land filter must exclude it.
    val relic = card("Safehouse Test Relic") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "{T}: You gain 2 life."
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.GainLife(2)
        }
    }
    val relicAbilityId = relic.activatedAbilities[0].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MirranSafehouse, WateryGrave, outpost, relic))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (driver.state.stack.isNotEmpty() && driver.state.pendingDecision == null && guard++ < 20) {
            driver.bothPass()
        }
    }

    fun pool(driver: GameTestDriver, player: EntityId) = driver.state.getEntity(player)?.get<ManaPoolComponent>()!!

    test("a land card in an opponent's graveyard lends its activated ability, and the {T} taps the Safehouse") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val safehouse = driver.putPermanentOnBattlefield(you, "Mirran Safehouse")
        driver.putCardInGraveyard(opponent, "Safehouse Test Outpost")

        val lifeBefore = driver.getLifeTotal(you)
        driver.submitSuccess(ActivateAbility(playerId = you, sourceId = safehouse, abilityId = outpostAbilityId))
        resolveStack(driver)

        driver.getLifeTotal(you) shouldBe lifeBefore + 2
        driver.state.getEntity(safehouse)?.has<TappedComponent>() shouldBe true
    }

    test("a nonland card in a graveyard lends nothing") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val safehouse = driver.putPermanentOnBattlefield(you, "Mirran Safehouse")
        driver.putCardInGraveyard(you, "Safehouse Test Relic")

        driver.submitExpectFailure(ActivateAbility(playerId = you, sourceId = safehouse, abilityId = relicAbilityId))
    }

    test("with no land card in any graveyard the Safehouse has no abilities") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val safehouse = driver.putPermanentOnBattlefield(you, "Mirran Safehouse")

        driver.submitExpectFailure(ActivateAbility(playerId = you, sourceId = safehouse, abilityId = outpostAbilityId))
        driver.submitExpectFailure(
            ActivateAbility(playerId = you, sourceId = safehouse, abilityId = AbilityId.intrinsicMana(Color.GREEN.symbol))
        )
    }

    test("a basic Forest in a graveyard lends {T}: Add {G}") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val safehouse = driver.putPermanentOnBattlefield(you, "Mirran Safehouse")
        driver.putCardInGraveyard(opponent, "Forest")

        driver.submitSuccess(
            ActivateAbility(playerId = you, sourceId = safehouse, abilityId = AbilityId.intrinsicMana(Color.GREEN.symbol))
        )
        pool(driver, you).getAmount(Color.GREEN) shouldBe 1
    }

    test("Watery Grave's intrinsic Island and Swamp mana abilities are lent though it prints none") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val safehouse = driver.putPermanentOnBattlefield(you, "Mirran Safehouse")
        driver.putCardInGraveyard(you, "Watery Grave")

        driver.submitSuccess(
            ActivateAbility(playerId = you, sourceId = safehouse, abilityId = AbilityId.intrinsicMana(Color.BLACK.symbol))
        )
        pool(driver, you).getAmount(Color.BLACK) shouldBe 1
        // Tapped for {B} — the {U} sibling is offered but can't be paid again this turn.
        driver.submitExpectFailure(
            ActivateAbility(playerId = you, sourceId = safehouse, abilityId = AbilityId.intrinsicMana(Color.BLUE.symbol))
        )
    }
})
