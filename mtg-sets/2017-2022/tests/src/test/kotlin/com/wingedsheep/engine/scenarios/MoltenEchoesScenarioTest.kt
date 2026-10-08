package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c20.cards.MoltenEchoes
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Molten Echoes (C20 #54) — {2}{R}{R} Enchantment
 *
 * "As this enchantment enters, choose a creature type.
 *  Whenever a nontoken creature you control of the chosen type enters, create a token that's a copy
 *  of that creature. That token gains haste. Exile it at the beginning of the next end step."
 *
 * Exercises the chosen-type gate, the hasty token copy, the end-step exile, and that the token
 * itself — a creature of the chosen type — doesn't re-trigger the enchantment (it's a token).
 */
class MoltenEchoesScenarioTest : FunSpec({

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
        driver.registerCards(TestCards.all + listOf(MoltenEchoes, goblin, bear))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castAndResolve(driver: GameTestDriver, playerId: EntityId, cardName: String): EntityId {
        val cardId = driver.putCardInHand(playerId, cardName)
        driver.giveColorlessMana(playerId, 1)
        driver.castSpell(playerId, cardId)
        driver.bothPass()
        return cardId
    }

    fun castMoltenEchoes(driver: GameTestDriver, playerId: EntityId, type: String) {
        val cardId = driver.putCardInHand(playerId, "Molten Echoes")
        driver.giveMana(playerId, Color.RED, 4)
        driver.castSpell(playerId, cardId)
        driver.bothPass()
        val decision = driver.pendingDecision as ChooseOptionDecision
        val index = decision.options.indexOfFirst { it.equals(type, ignoreCase = true) }
        driver.submitDecision(playerId, OptionChosenResponse(decision.id, index))
    }

    fun tokensNamed(driver: GameTestDriver, playerId: EntityId, name: String): List<EntityId> =
        driver.getPermanents(playerId).filter { id ->
            val entity = driver.state.getEntity(id)
            entity?.has<TokenComponent>() == true && entity.get<CardComponent>()?.name == name
        }

    test("a nontoken creature of the chosen type gets a hasty token copy that is exiled at the next end step") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        castMoltenEchoes(driver, you, "Goblin")

        castAndResolve(driver, you, "Test Goblin")
        // The trigger is on the stack; resolve it.
        driver.bothPass()

        val tokens = tokensNamed(driver, you, "Test Goblin")
        tokens shouldHaveSize 1
        val token = tokens.single()
        driver.state.projectedState.hasKeyword(token, com.wingedsheep.sdk.core.Keyword.HASTE) shouldBe true
        driver.state.projectedState.getPower(token) shouldBe 2

        // The token is itself a Goblin creature entering, but it's a token: no second copy.
        driver.state.stack.shouldBeEmpty()

        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        tokensNamed(driver, you, "Test Goblin").shouldBeEmpty()
        // The original stays.
        driver.getPermanents(you).count { driver.getCardName(it) == "Test Goblin" } shouldBe 1
    }

    test("a creature of another type does not trigger") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        castMoltenEchoes(driver, you, "Goblin")

        castAndResolve(driver, you, "Test Bear")
        driver.state.stack.shouldBeEmpty()
        tokensNamed(driver, you, "Test Bear").shouldBeEmpty()
    }

    test("a creature killed with the trigger on the stack is still copied from last-known information") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        castMoltenEchoes(driver, you, "Goblin")

        val goblinId = castAndResolve(driver, you, "Test Goblin")
        driver.state.stack.size shouldBe 1

        // Bolt the Goblin in response to the trigger.
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(goblinId))
        driver.bothPass()
        driver.getGraveyardCardNames(you).contains("Test Goblin") shouldBe true

        driver.bothPass()
        val tokens = tokensNamed(driver, you, "Test Goblin")
        tokens shouldHaveSize 1
        driver.state.projectedState.getPower(tokens.single()) shouldBe 2
        driver.state.projectedState.hasKeyword(tokens.single(), com.wingedsheep.sdk.core.Keyword.HASTE) shouldBe true
    }
})
