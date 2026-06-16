package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.BoneyardDesecrator
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Boneyard Desecrator (OTJ #81).
 *
 * {1}{B}, Sacrifice another creature: Put a +1/+1 counter on this creature. If an outlaw
 * was sacrificed this way, create a Treasure token.
 *
 * The outlaw rider is an OR over the five OTJ outlaw subtypes against the cost-sacrificed
 * permanent's snapshot (SacrificedHadSubtype). Proven both ways:
 *  - Sacrifice a Pirate (an outlaw) → counter added AND a Treasure created.
 *  - Sacrifice a plain Beast (not an outlaw) → counter added, NO Treasure.
 */
class BoneyardDesecratorTest : FunSpec({

    val abilityId = BoneyardDesecrator.activatedAbilities.first().id

    val pirateFodder = CardDefinition.creature(
        name = "Pirate Fodder",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype.PIRATE),
        power = 1,
        toughness = 1,
    )

    val beastFodder = CardDefinition.creature(
        name = "Beast Fodder",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Beast")),
        power = 1,
        toughness = 1,
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BoneyardDesecrator, pirateFodder, beastFodder, PredefinedTokens.Treasure))
        return driver
    }

    fun treasureCount(driver: GameTestDriver, player: com.wingedsheep.sdk.model.EntityId): Int =
        driver.state.getBattlefield().count { id ->
            driver.state.projectedState.getController(id) == player &&
                driver.state.getEntity(id)
                    ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Treasure"
        }

    test("sacrificing an outlaw adds a counter AND creates a Treasure") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val desecrator = driver.putCreatureOnBattlefield(you, "Boneyard Desecrator")
        driver.removeSummoningSickness(desecrator)
        val pirate = driver.putCreatureOnBattlefield(you, "Pirate Fodder")

        val treasuresBefore = treasureCount(driver, you)
        driver.giveMana(you, Color.BLACK, 1)
        driver.giveColorlessMana(you, 1)

        val result = driver.submit(
            ActivateAbility(
                playerId = you,
                sourceId = desecrator,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(pirate))
            )
        )
        result.isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        // +1/+1 counter on Boneyard Desecrator.
        driver.state.getEntity(desecrator)
            ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        // A Treasure was created (outlaw sacrificed).
        (treasureCount(driver, you) - treasuresBefore) shouldBe 1
    }

    test("sacrificing a non-outlaw adds a counter but creates NO Treasure") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val desecrator = driver.putCreatureOnBattlefield(you, "Boneyard Desecrator")
        driver.removeSummoningSickness(desecrator)
        val beast = driver.putCreatureOnBattlefield(you, "Beast Fodder")

        val treasuresBefore = treasureCount(driver, you)
        driver.giveMana(you, Color.BLACK, 1)
        driver.giveColorlessMana(you, 1)

        val result = driver.submit(
            ActivateAbility(
                playerId = you,
                sourceId = desecrator,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(beast))
            )
        )
        result.isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        // +1/+1 counter still added.
        driver.state.getEntity(desecrator)
            ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        // No Treasure — the sacrificed creature was not an outlaw.
        (treasureCount(driver, you) - treasuresBefore) shouldBe 0
    }
})
