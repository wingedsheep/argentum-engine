package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c21.cards.GyomeMasterChef
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Gyome, Master Chef (C21 #5) — {2}{B}{G} 5/3 Legendary Creature — Troll Warlock, trample.
 *
 * "At the beginning of your end step, create a number of Food tokens equal to the number of
 *  nontoken creatures you had enter the battlefield under your control this turn.
 *  {1}, Sacrifice a Food: Target creature gains indestructible until end of turn. Tap it."
 *
 * Pins the `NONTOKEN_CREATURES_ENTERED_UNDER_CONTROL` count: Gyome counts itself on the turn it
 * enters, a creature token doesn't count, and a creature that has since left still does (2021-04-16
 * rulings). Then the Food ability grants indestructible and taps the target.
 */
class GyomeMasterChefScenarioTest : FunSpec({

    val makeToken = card("Gyome Test Token Maker") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Create a 1/1 Soldier creature token."
        spell { effect = Effects.CreateToken(power = 1, toughness = 1, creatureTypes = setOf("Soldier")) }
    }
    val bounce = card("Gyome Test Bounce") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Return target creature to its owner's hand."
        spell {
            val t = target(com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.Creature)
            effect = Effects.ReturnToHand(t)
        }
    }

    fun GameTestDriver.foods(playerId: EntityId) =
        getPermanents(playerId).filter { state.projectedState.hasSubtype(it, "Food") }

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GyomeMasterChef, makeToken, bounce) + PredefinedTokens.allTokens)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Swamp" to 20), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        repeat(3) { driver.putLandOnBattlefield(you, "Forest") }
        repeat(3) { driver.putLandOnBattlefield(you, "Swamp") }
        return driver to you
    }

    fun GameTestDriver.castAndResolve(playerId: EntityId, name: String, targets: List<EntityId> = emptyList()) {
        val id = putCardInHand(playerId, name)
        castSpell(playerId, id, targets).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("end step makes a Food per nontoken creature that entered this turn — Gyome itself, not tokens, leavers included") {
        val (driver, you) = setup()
        driver.castAndResolve(you, "Gyome, Master Chef")
        driver.castAndResolve(you, "Grizzly Bears")
        driver.castAndResolve(you, "Gyome Test Token Maker")
        val bears = driver.findPermanent(you, "Grizzly Bears")!!
        driver.castAndResolve(you, "Gyome Test Bounce", listOf(bears))
        withClue("the Bears have left the battlefield") {
            driver.findPermanent(you, "Grizzly Bears") shouldBe null
        }

        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        withClue("Gyome + Bears entered; the Soldier token doesn't count") {
            driver.foods(you).size shouldBe 2
        }
    }

    test("{1}, sacrifice a Food: target creature gains indestructible and is tapped") {
        val (driver, you) = setup()
        driver.castAndResolve(you, "Gyome, Master Chef")
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        val food = driver.foods(you).single()
        driver.state.getEntity(food)?.has<TokenComponent>() shouldBe true

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = driver.getOpponent(you))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = you)
        val gyome = driver.findPermanent(you, "Gyome, Master Chef")!!
        driver.submit(
            ActivateAbility(
                playerId = you,
                sourceId = gyome,
                abilityId = GyomeMasterChef.activatedAbilities.first().id,
                targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(gyome)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(food)),
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.state.projectedState.hasKeyword(gyome, Keyword.INDESTRUCTIBLE) shouldBe true
        driver.isTapped(gyome) shouldBe true
        driver.foods(you).size shouldBe 0
    }
})
