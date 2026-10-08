package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.rna.cards.GuardianProject
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Guardian Project (RNA #130) — {3}{G} Enchantment
 *
 * "Whenever a nontoken creature you control enters, if it doesn't have the same name as another
 *  creature you control or a creature card in your graveyard, draw a card."
 *
 * The intervening "if" is a name comparison against the triggering creature, checked as it enters
 * and again on resolution (CR 603.4).
 */
class GuardianProjectScenarioTest : FunSpec({

    val bear = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    val wolf = CardDefinition.creature(
        name = "Test Wolf",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Wolf")),
        power = 2,
        toughness = 2
    )

    fun createDriver(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GuardianProject, bear, wolf))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        driver.putPermanentOnBattlefield(you, "Guardian Project")
        return driver to you
    }

    /** Cast [name] and let the spell resolve; any trigger it causes is left on the stack. */
    fun cast(driver: GameTestDriver, playerId: EntityId, name: String): EntityId {
        val cardId = driver.putCardInHand(playerId, name)
        driver.giveColorlessMana(playerId, 1)
        driver.castSpell(playerId, cardId)
        driver.bothPass()
        return cardId
    }

    test("the first creature of its name draws a card") {
        val (driver, you) = createDriver()
        val handBefore = driver.getHandSize(you)
        cast(driver, you, "Test Bear")
        driver.bothPass()
        // Put in hand (+1), cast (-1), the trigger drew (+1).
        driver.getHandSize(you) shouldBe handBefore + 1
    }

    test("a creature sharing a name with another creature you control doesn't trigger") {
        val (driver, you) = createDriver()
        driver.putCreatureOnBattlefield(you, "Test Bear")
        val handBefore = driver.getHandSize(you)
        cast(driver, you, "Test Bear")
        driver.state.stack.shouldBeEmpty()
        driver.getHandSize(you) shouldBe handBefore
    }

    test("a creature sharing a name with a creature card in your graveyard doesn't trigger") {
        val (driver, you) = createDriver()
        driver.putCardInGraveyard(you, "Test Bear")
        val handBefore = driver.getHandSize(you)
        cast(driver, you, "Test Bear")
        driver.state.stack.shouldBeEmpty()
        driver.getHandSize(you) shouldBe handBefore

        // A different name still draws.
        cast(driver, you, "Test Wolf")
        driver.bothPass()
        driver.getHandSize(you) shouldBe handBefore + 1
    }

    test("an opponent's creature with the same name doesn't stop the draw") {
        val (driver, you) = createDriver()
        driver.putCreatureOnBattlefield(driver.getOpponent(you), "Test Bear")
        val handBefore = driver.getHandSize(you)
        cast(driver, you, "Test Bear")
        driver.bothPass()
        driver.getHandSize(you) shouldBe handBefore + 1
    }

    test("if the creature dies with the trigger on the stack, the recheck finds it in the graveyard") {
        val (driver, you) = createDriver()
        val handBefore = driver.getHandSize(you)
        val bearId = cast(driver, you, "Test Bear")
        driver.state.stack.size shouldBe 1
        driver.moveToGraveyard(bearId)
        driver.bothPass()
        driver.state.stack.shouldBeEmpty()
        // Put in hand (+1), cast (-1), no draw.
        driver.getHandSize(you) shouldBe handBefore
    }
})
