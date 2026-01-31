package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.CreatureDamageFilter
import com.wingedsheep.sdk.scripting.DealDamageToGroupEffect
import com.wingedsheep.sdk.scripting.DealDamageToPlayersEffect
import com.wingedsheep.sdk.scripting.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for X cost spells (like Hurricane).
 */
class XCostSpellTest : FunSpec({

    // Test card: Hurricane - deals X damage to each creature with flying and each player
    val Hurricane = CardDefinition.sorcery(
        name = "Hurricane",
        manaCost = ManaCost.parse("{X}{G}"),
        oracleText = "Hurricane deals X damage to each creature with flying and each player.",
        script = CardScript.spell(
            effect = DealDamageToGroupEffect(
                DynamicAmount.XValue,
                CreatureDamageFilter.WithKeyword(Keyword.FLYING)
            ).then(DealDamageToPlayersEffect(DynamicAmount.XValue))
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Hurricane))
        return driver
    }

    test("Hurricane with X=4 deals 4 damage to each player") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Forest" to 20
            ),
            skipMulligans = true,
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Hurricane in hand and give mana
        val hurricane = driver.putCardInHand(activePlayer, "Hurricane")

        // Give 5 mana (1 green for the {G} + 4 for X=4)
        driver.giveMana(activePlayer, Color.GREEN, 5)

        // Cast Hurricane with X=4
        driver.castXSpell(activePlayer, hurricane, xValue = 4)
        driver.bothPass()

        // Both players should have taken 4 damage (20 - 4 = 16)
        driver.getLifeTotal(activePlayer) shouldBe 16
        driver.getLifeTotal(opponent) shouldBe 16
    }

    test("Hurricane with X=0 deals 0 damage") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Forest" to 20
            ),
            skipMulligans = true,
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Hurricane in hand and give just enough mana for X=0
        val hurricane = driver.putCardInHand(activePlayer, "Hurricane")
        driver.giveMana(activePlayer, Color.GREEN, 1)

        // Cast Hurricane with X=0
        driver.castXSpell(activePlayer, hurricane, xValue = 0)
        driver.bothPass()

        // No damage should have been dealt
        driver.getLifeTotal(activePlayer) shouldBe 20
        driver.getLifeTotal(opponent) shouldBe 20
    }

    test("ManaCost.hasX correctly detects X in cost") {
        val xCost = ManaCost.parse("{X}{G}")
        val normalCost = ManaCost.parse("{2}{G}")

        xCost.hasX shouldBe true
        normalCost.hasX shouldBe false
    }

    test("ManaSolver.getAvailableManaCount returns correct count") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Forest" to 20
            ),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put 5 forests on the battlefield
        repeat(5) {
            driver.putLandOnBattlefield(activePlayer, "Forest")
        }

        // Create a ManaSolver and check available mana count
        val registry = CardRegistry()
        registry.register(TestCards.all)
        val manaSolver = ManaSolver(registry)

        val count = manaSolver.getAvailableManaCount(driver.state, activePlayer)
        count shouldBe 5
    }

    test("ManaSolver calculates correct max X based on available mana") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Forest" to 20
            ),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put 5 forests on the battlefield
        repeat(5) {
            driver.putLandOnBattlefield(activePlayer, "Forest")
        }

        // Create a ManaSolver and calculate max X for Hurricane ({X}{G})
        val registry = CardRegistry()
        registry.register(TestCards.all)
        val manaSolver = ManaSolver(registry)

        val availableSources = manaSolver.getAvailableManaCount(driver.state, activePlayer)
        val hurricaneCost = ManaCost.parse("{X}{G}")
        val fixedCost = hurricaneCost.cmc  // X contributes 0 to CMC, so this is just the {G} = 1

        val maxX = (availableSources - fixedCost).coerceAtLeast(0)

        // With 5 forests and 1 green needed for the base cost, max X should be 4
        maxX shouldBe 4
    }
})
