package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdc.cards.ThundermaneDragon
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Thundermane Dragon (TDC #38) — {3}{R} Creature — Dragon, 4/4.
 *
 * "Flying. You may look at the top card of your library any time. You may cast creature spells
 *  with power 4 or greater from the top of your library. If you cast a creature spell this way, it
 *  gains haste until end of turn."
 *
 * Covers the permission's power filter and its cast-this-way haste rider: granted only to a spell
 * cast from the top, kept when the Dragon leaves before the spell resolves, gone after the turn.
 */
class ThundermaneDragonScenarioTest : FunSpec({

    val giant = CardDefinition.creature(
        name = "Test Giant",
        manaCost = ManaCost.parse("{1}"),
        subtypes = emptySet(),
        power = 4,
        toughness = 4,
    )
    val bear = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}"),
        subtypes = emptySet(),
        power = 3,
        toughness = 3,
    )

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ThundermaneDragon, giant, bear))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val dragon = driver.putCreatureOnBattlefield(you, "Thundermane Dragon")
        return Triple(driver, you, dragon)
    }

    fun offered(driver: GameTestDriver, you: EntityId, name: String): Boolean =
        driver.legalActions(you).any { it.actionType == "CastSpell" && it.description.contains(name) }

    fun cast(driver: GameTestDriver, you: EntityId, cardId: EntityId) =
        driver.submit(CastSpell(you, cardId, paymentStrategy = PaymentStrategy.FromPool))

    test("a power-4 creature cast from the top gains haste until end of turn") {
        val (driver, you, _) = setup()
        val top = driver.putCardOnTopOfLibrary(you, "Test Giant")
        driver.giveColorlessMana(you, 1)

        offered(driver, you, "Test Giant") shouldBe true
        cast(driver, you, top).error shouldBe null
        driver.bothPass()

        driver.state.getBattlefield(you).contains(top) shouldBe true
        driver.state.projectedState.hasKeyword(top, Keyword.HASTE) shouldBe true

        driver.passPriorityUntil(Step.UPKEEP, driver.getOpponent(you))
        driver.state.projectedState.hasKeyword(top, Keyword.HASTE) shouldBe false
    }

    test("a creature with power less than 4 on top is neither offered nor castable") {
        val (driver, you, _) = setup()
        val top = driver.putCardOnTopOfLibrary(you, "Test Bear")
        driver.giveColorlessMana(you, 1)

        offered(driver, you, "Test Bear") shouldBe false
        cast(driver, you, top).error shouldNotBe null
        driver.state.getLibrary(you).first() shouldBe top
    }

    test("the same creature cast from hand does not gain haste") {
        val (driver, you, _) = setup()
        val inHand = driver.putCardInHand(you, "Test Giant")
        driver.giveColorlessMana(you, 1)

        cast(driver, you, inHand).error shouldBe null
        driver.bothPass()

        driver.state.getBattlefield(you).contains(inHand) shouldBe true
        driver.state.projectedState.hasKeyword(inHand, Keyword.HASTE) shouldBe false
    }

    test("the creature keeps haste when the Dragon leaves before the spell resolves") {
        val (driver, you, dragon) = setup()
        val top = driver.putCardOnTopOfLibrary(you, "Test Giant")
        driver.giveColorlessMana(you, 1)

        cast(driver, you, top).error shouldBe null
        driver.moveToGraveyard(dragon)
        driver.bothPass()

        driver.state.getBattlefield(you).contains(top) shouldBe true
        driver.state.projectedState.hasKeyword(top, Keyword.HASTE) shouldBe true
    }
})
