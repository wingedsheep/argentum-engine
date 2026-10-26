package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c17.cards.KindredDiscovery
import com.wingedsheep.sdk.core.Color
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
 * Kindred Discovery (C17 #11) — "As this enchantment enters, choose a creature type. Whenever a
 * creature you control of the chosen type enters or attacks, draw a card."
 *
 * Proves the chosen-type gate on both halves of the disjunctive trigger: entering and attacking.
 */
class KindredDiscoveryScenarioTest : FunSpec({

    val goblin = CardDefinition.creature(
        name = "Test Goblin",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Goblin")),
        power = 2,
        toughness = 1
    )

    val bear = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(KindredDiscovery, goblin, bear))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castKindredDiscovery(driver: GameTestDriver, playerId: EntityId, type: String) {
        val cardId = driver.putCardInHand(playerId, "Kindred Discovery")
        driver.giveMana(playerId, Color.BLUE, 5)
        driver.castSpell(playerId, cardId)
        driver.bothPass()
        val decision = driver.pendingDecision as ChooseOptionDecision
        val index = decision.options.indexOfFirst { it.equals(type, ignoreCase = true) }
        driver.submitDecision(playerId, OptionChosenResponse(decision.id, index))
    }

    fun castAndResolve(driver: GameTestDriver, playerId: EntityId, cardName: String): EntityId {
        val cardId = driver.putCardInHand(playerId, cardName)
        driver.giveColorlessMana(playerId, 1)
        driver.castSpell(playerId, cardId)
        driver.bothPass()
        return cardId
    }

    test("a creature of the chosen type entering draws a card; another type does not") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        castKindredDiscovery(driver, you, "Goblin")

        castAndResolve(driver, you, "Test Bear")
        driver.state.stack.shouldBeEmpty()

        val handBefore = driver.getHandSize(you)
        castAndResolve(driver, you, "Test Goblin")
        driver.state.stack.size shouldBe 1
        driver.bothPass()
        driver.getHandSize(you) shouldBe handBefore + 1
    }

    test("a creature of the chosen type attacking draws a card") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        castKindredDiscovery(driver, you, "Goblin")
        val goblinId = driver.putCreatureOnBattlefield(you, "Test Goblin")
        val bearId = driver.putCreatureOnBattlefield(you, "Test Bear")
        driver.removeSummoningSickness(goblinId)
        driver.removeSummoningSickness(bearId)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val handBefore = driver.getHandSize(you)
        driver.declareAttackers(you, listOf(goblinId, bearId), driver.getOpponent(you)).error shouldBe null
        driver.state.stack.size shouldBe 1
        driver.bothPass()
        driver.getHandSize(you) shouldBe handBefore + 1
    }
})
