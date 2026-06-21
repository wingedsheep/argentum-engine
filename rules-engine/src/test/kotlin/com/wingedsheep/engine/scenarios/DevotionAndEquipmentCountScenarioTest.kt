package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.CoralSword
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for the §6 (Equipment / equipped-creature count) and §7 (devotion) DynamicAmount facades
 * from the FIN engine-gap warm-ups. Each is exercised through a characteristic-defining creature
 * whose power *is* the dynamic amount, so the projected power reads the value back.
 *
 * Devotion (CR 700.5): the number of mana symbols of the chosen color(s) among the mana costs of
 * permanents you control — plain colored, both halves of a two-color hybrid, monocolored twobrid,
 * and Phyrexian symbols all count; opponents' permanents and off-color symbols do not.
 */
class DevotionAndEquipmentCountScenarioTest : FunSpec({

    val projector = StateProjector()

    // ----- devotion fixtures -----------------------------------------------------------------

    // power = your devotion to red (toughness padded so the 0/0 case can't die to an SBA).
    val DevotionMeterRed = card("Devotion Meter Red") {
        manaCost = "{1}{R}" // contributes 1 to its controller's own red devotion
        typeLine = "Creature — Test"
        dynamicStats(DynamicAmounts.devotionTo(Color.RED), toughnessOffset = 20)
    }
    // power = your devotion to red and green (a symbol that is either colour counts once).
    val DevotionMeterRedGreen = card("Devotion Meter Red Green") {
        manaCost = "{G}{G}" // contributes 2 to red-or-green devotion
        typeLine = "Creature — Test"
        dynamicStats(DynamicAmounts.devotionTo(Color.RED, Color.GREEN), toughnessOffset = 20)
    }

    fun vanilla(name: String, cost: String) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse(cost),
        subtypes = emptySet(),
        power = 2, toughness = 2
    )
    val RedRed = vanilla("Devotion RedRed", "{R}{R}")     // +2 red
    val HybridRedGreen = vanilla("Devotion Hybrid RG", "{R/G}") // +1 red, +1 green, +1 red-or-green
    val TwobridRed = vanilla("Devotion Twobrid R", "{2/R}")     // +1 red
    val PhyrexianRed = vanilla("Devotion Phyrexian R", "{R/P}") // +1 red
    val BlueBlue = vanilla("Devotion BlueBlue", "{U}{U}")       // +0 red, +0 green

    // ----- equipment fixtures ----------------------------------------------------------------

    val EquipmentMeter = card("Equipment Meter") {
        manaCost = "{0}"
        typeLine = "Creature — Test"
        dynamicStats(DynamicAmounts.equipmentYouControl(), toughnessOffset = 20)
    }
    val EquippedMeter = card("Equipped Meter") {
        manaCost = "{0}"
        typeLine = "Creature — Test"
        dynamicStats(DynamicAmounts.equippedCreaturesYouControl(), toughnessOffset = 20)
    }

    fun devotionDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                DevotionMeterRed, DevotionMeterRedGreen,
                RedRed, HybridRedGreen, TwobridRed, PhyrexianRed, BlueBlue
            )
        )
        return driver
    }

    context("§7 devotion (CR 700.5)") {

        test("devotion to red counts every red symbol on your permanents, ignoring opponents and off-color") {
            val driver = devotionDriver()
            driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val me = driver.activePlayer!!
            val opp = driver.getOpponent(me)

            val meter = driver.putCreatureOnBattlefield(me, "Devotion Meter Red") // {1}{R} → +1
            driver.putCreatureOnBattlefield(me, "Devotion RedRed")                // {R}{R} → +2
            driver.putCreatureOnBattlefield(me, "Devotion Hybrid RG")             // {R/G} → +1
            driver.putCreatureOnBattlefield(me, "Devotion Twobrid R")             // {2/R} → +1
            driver.putCreatureOnBattlefield(me, "Devotion Phyrexian R")           // {R/P} → +1
            driver.putCreatureOnBattlefield(me, "Devotion BlueBlue")             // {U}{U} → +0
            driver.putCreatureOnBattlefield(opp, "Devotion RedRed")              // opponent's → ignored

            // 1 + 2 + 1 + 1 + 1 + 0 = 6
            projector.getProjectedPower(driver.state, meter) shouldBe 6
        }

        test("devotion to red and green counts a hybrid symbol once") {
            val driver = devotionDriver()
            driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val me = driver.activePlayer!!

            val meter = driver.putCreatureOnBattlefield(me, "Devotion Meter Red Green") // {G}{G} → +2
            driver.putCreatureOnBattlefield(me, "Devotion Meter Red")  // {1}{R} → +1 (red)
            driver.putCreatureOnBattlefield(me, "Devotion RedRed")     // {R}{R} → +2
            driver.putCreatureOnBattlefield(me, "Devotion Hybrid RG")  // {R/G} → +1 (counted once)
            driver.putCreatureOnBattlefield(me, "Devotion Twobrid R")  // {2/R} → +1
            driver.putCreatureOnBattlefield(me, "Devotion Phyrexian R")// {R/P} → +1
            driver.putCreatureOnBattlefield(me, "Devotion BlueBlue")  // {U}{U} → +0

            // 2 + 1 + 2 + 1 + 1 + 1 = 8
            projector.getProjectedPower(driver.state, meter) shouldBe 8
        }
    }

    context("§6 Equipment / equipped-creature counts") {

        test("equipmentYouControl and equippedCreaturesYouControl track a real equip") {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all + listOf(EquipmentMeter, EquippedMeter))
            driver.registerCard(CoralSword)
            driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val me = driver.activePlayer!!

            val equipmentMeter = driver.putCreatureOnBattlefield(me, "Equipment Meter")
            val equippedMeter = driver.putCreatureOnBattlefield(me, "Equipped Meter")
            val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser") // 3/3

            // No Equipment yet, no equipped creatures.
            projector.getProjectedPower(driver.state, equipmentMeter) shouldBe 0
            projector.getProjectedPower(driver.state, equippedMeter) shouldBe 0

            // Cast Coral Sword; its ETB attaches it to a creature you control (CR — equip).
            val sword = driver.putCardInHand(me, "Coral Sword")
            driver.giveMana(me, Color.RED, 1)
            driver.castSpell(me, sword)
            driver.bothPass() // resolve the artifact -> enters -> ETB trigger on stack
            driver.bothPass() // resolve ETB trigger -> pauses for target selection
            driver.submitTargetSelection(me, listOf(courser))
            driver.bothPass()

            // One Equipment on the battlefield; one equipped creature (the Courser).
            projector.getProjectedPower(driver.state, equipmentMeter) shouldBe 1
            projector.getProjectedPower(driver.state, equippedMeter) shouldBe 1
        }
    }
})
