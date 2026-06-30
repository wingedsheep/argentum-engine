package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.znr.cards.BrokenWings
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Broken Wings {2}{G} Instant (ZNR canonical; reprinted in FDN).
 *
 * Destroy target artifact, enchantment, or creature with flying.
 */
class BrokenWingsScenarioTest : FunSpec({

    val Flyer = CardDefinition.creature(
        name = "Test Flyer",
        manaCost = ManaCost.parse("{1}{U}"),
        subtypes = emptySet(),
        power = 1,
        toughness = 1,
        keywords = setOf(Keyword.FLYING)
    )

    val Grounder = CardDefinition.creature(
        name = "Test Grounder",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = emptySet(),
        power = 2,
        toughness = 2
    )

    val Rock = CardDefinition.artifact(
        name = "Test Rock",
        manaCost = ManaCost.parse("{2}")
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BrokenWings, Flyer, Grounder, Rock))
        return driver
    }

    fun castWings(driver: GameTestDriver, you: com.wingedsheep.sdk.model.EntityId) {
        driver.giveMana(you, Color.GREEN, 1)
        driver.giveColorlessMana(you, 2)
    }

    test("destroys a creature with flying") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val flyer = driver.putCreatureOnBattlefield(opp, "Test Flyer")
        castWings(driver, you)
        val wings = driver.putCardInHand(you, "Broken Wings")

        driver.castSpellWithTargets(you, wings, listOf(ChosenTarget.Permanent(flyer))).isSuccess shouldBe true
        driver.bothPass()

        driver.findPermanent(opp, "Test Flyer") shouldBe null
    }

    test("destroys an artifact") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val rock = driver.putPermanentOnBattlefield(opp, "Test Rock")
        castWings(driver, you)
        val wings = driver.putCardInHand(you, "Broken Wings")

        driver.castSpellWithTargets(you, wings, listOf(ChosenTarget.Permanent(rock))).isSuccess shouldBe true
        driver.bothPass()

        driver.findPermanent(opp, "Test Rock") shouldBe null
    }

    test("cannot target a creature without flying") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val grounder = driver.putCreatureOnBattlefield(opp, "Test Grounder")
        castWings(driver, you)
        val wings = driver.putCardInHand(you, "Broken Wings")

        val result = driver.submit(
            CastSpell(
                playerId = you,
                cardId = wings,
                targets = listOf(ChosenTarget.Permanent(grounder)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe false
        driver.findPermanent(opp, "Test Grounder") shouldBe grounder
    }
})
