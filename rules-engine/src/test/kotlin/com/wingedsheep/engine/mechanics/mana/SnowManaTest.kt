package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Snow mana (CR 107.4h): a `{S}` pip is paid only with one mana of any type produced by a snow
 * source, and "{S} spent" counts every unit of snow mana spent on a cost. Pins the floating pool's
 * exact per-color snow tracking and the auto-tap solver's reservation of snow sources for `{S}`.
 */
class SnowManaTest : FunSpec({

    fun snowLand(name: String, subtype: Subtype) = CardDefinition(
        name = name,
        manaCost = ManaCost.ZERO,
        typeLine = TypeLine(supertypes = setOf(Supertype.SNOW, Supertype.BASIC), cardTypes = setOf(CardType.LAND), subtypes = setOf(subtype)),
        oracleText = "",
    )
    val snowIsland = snowLand("Test Snow Island", Subtype.ISLAND)
    val snowSwamp = snowLand("Test Snow Swamp", Subtype.SWAMP)
    val cards = TestCards.all + listOf(snowIsland, snowSwamp)

    fun solverFor(): ManaSolver =
        ManaSolver(CardRegistry().apply { register(cards) }, predicateEvaluator = PredicateEvaluator(cardRegistry = null))

    fun driverWith(vararg lands: String): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(cards)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        lands.forEach { driver.putLandOnBattlefield(driver.player1, it) }
        return driver
    }

    context("floating pool") {
        val oneSnowGreen = ManaPool(green = 2).markSnow(Color.GREEN, 1)

        test("a snow mark is capped at the mana that floats") {
            ManaPool(blue = 1).markSnow(Color.BLUE, 3).snowTotal shouldBe 1
            ManaPool(colorless = 2).markSnow(null, 1).snowColorless shouldBe 1
        }

        test("spending a color for anything but {S} spends its plain units first") {
            val afterOne = oneSnowGreen.spend(Color.GREEN)!!
            afterOne.snowMana[Color.GREEN] shouldBe 1
            afterOne.spend(Color.GREEN)!!.snowTotal shouldBe 0
        }

        test("{S} is paid only by a snow unit") {
            oneSnowGreen.canPay(ManaCost.parse("{S}")) shouldBe true
            oneSnowGreen.canPay(ManaCost.parse("{S}{S}")) shouldBe false
            ManaPool(green = 3).canPay(ManaCost.parse("{S}")) shouldBe false
        }

        test("a colored pip doesn't rob {S} of the only snow unit when a plain one will do") {
            oneSnowGreen.canPay(ManaCost.parse("{G}{S}")) shouldBe true
            oneSnowGreen.pay(ManaCost.parse("{G}{S}"))!!.total shouldBe 0
        }

        test("a colored pip that needs the snow unit leaves {S} unpayable") {
            // One snow {G} and one plain {U}: {G} must take the snow green, and plain blue can't pay {S}.
            ManaPool(green = 1, blue = 1).markSnow(Color.GREEN, 1).canPay(ManaCost.parse("{G}{S}")) shouldBe false
        }

        test("generic is paid after {S}, so it never takes the snow unit {S} needs") {
            val pool = ManaPool(colorless = 1, green = 1).markSnow(null, 1)
            pool.canPay(ManaCost.parse("{1}{S}")) shouldBe true
        }

        test("a partial payment leaves an unpayable {S} for the land pass") {
            val partial = ManaPool(green = 2).payPartial(ManaCost.parse("{1}{S}"))
            partial.remainingCost shouldBe ManaCost.parse("{S}")
        }
    }

    context("auto-tap solver") {
        test("{S} with no snow source can't be paid") {
            val driver = driverWith("Island", "Island")
            solverFor().solve(driver.state, driver.player1, ManaCost.parse("{1}{S}")).shouldBeNull()
        }

        test("the snow land pays {S} and the plain land the generic") {
            val driver = driverWith("Island", "Test Snow Island")
            val solution = solverFor().solve(driver.state, driver.player1, ManaCost.parse("{1}{S}")).shouldNotBeNull()
            solution.sources.map { it.name } shouldContainExactlyInAnyOrder listOf("Island", "Test Snow Island")
        }

        test("a snow land that is the only source of a needed color is not spent on {S}") {
            // {U}{S}: the snow Swamp must pay {S}, leaving the snow Island for {U}.
            val driver = driverWith("Test Snow Island", "Test Snow Swamp")
            val solution = solverFor().solve(driver.state, driver.player1, ManaCost.parse("{U}{S}")).shouldNotBeNull()
            val swamp = solution.sources.single { it.name == "Test Snow Swamp" }
            val island = solution.sources.single { it.name == "Test Snow Island" }
            solution.manaProduced.getValue(island.entityId).color shouldBe Color.BLUE
            solution.manaProduced.getValue(swamp.entityId).color shouldBe Color.BLACK
        }

        test("each {S} needs its own snow source") {
            val driver = driverWith("Test Snow Island", "Island")
            solverFor().solve(driver.state, driver.player1, ManaCost.parse("{S}{S}")).shouldBeNull()
        }

        test("snow mana tapped for a generic cost is marked snow for the {S}-spent tally") {
            val driver = driverWith("Test Snow Island")
            val solution = solverFor().solve(driver.state, driver.player1, ManaCost.parse("{1}")).shouldNotBeNull()
            solution.manaProduced.values.single().snow shouldBe true
        }
    }

    context("costs") {
        test("generic reductions don't reduce {S}") {
            ManaCost.parse("{2}{S}").reduceGeneric(5) shouldBe ManaCost.parse("{S}")
        }

        test("a reduction of {S} reduces generic instead (CR 118.7g)") {
            ManaCost.parse("{3}{U}").subtract(ManaCost.parse("{S}")) shouldBe ManaCost.parse("{2}{U}")
        }
    }
})
