package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ori.cards.FlameshadowConjuring
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
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
 * Flameshadow Conjuring (ORI #147) — {3}{R} Enchantment
 *
 * "Whenever a nontoken creature you control enters, you may pay {R}. If you do, create a token
 *  that's a copy of that creature. That token gains haste. Exile it at the beginning of the next
 *  end step."
 *
 * Covers the optional {R} (paid and declined), the hasty token copy and its end-step exile, the
 * token not re-triggering the enchantment, the copy reading last-known information when the
 * creature has already left, and haste being a grant rather than a copiable value (a Clone copying
 * the token has no haste — 2015-06-22 ruling).
 */
class FlameshadowConjuringScenarioTest : FunSpec({

    val bear = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FlameshadowConjuring, bear))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castBear(driver: GameTestDriver, playerId: EntityId): EntityId {
        val cardId = driver.putCardInHand(playerId, "Test Bear")
        driver.giveColorlessMana(playerId, 1)
        driver.castSpell(playerId, cardId)
        driver.bothPass()
        return cardId
    }

    /**
     * Drive the Flameshadow trigger to resolution: answer its pay question with [pay], tap the
     * Mountain for the {R} when paying, and pass priority until the stack is empty.
     * Returns whether the yes/no question was asked.
     */
    fun resolveTrigger(driver: GameTestDriver, playerId: EntityId, pay: Boolean): Boolean {
        var asked = false
        var guard = 0
        while (guard++ < 40) {
            when (driver.pendingDecision) {
                is YesNoDecision -> {
                    asked = true
                    driver.submitYesNo(playerId, pay)
                }
                is SelectManaSourcesDecision -> driver.submitManaAutoPayOrDecline(playerId, true)
                null -> if (driver.state.stack.isNotEmpty()) driver.bothPass() else break
                else -> error("Unexpected decision: ${driver.pendingDecision}")
            }
        }
        return asked
    }

    fun tokensNamed(driver: GameTestDriver, playerId: EntityId, name: String): List<EntityId> =
        driver.getPermanents(playerId).filter { id ->
            val entity = driver.state.getEntity(id)
            entity?.has<TokenComponent>() == true && entity.get<CardComponent>()?.name == name
        }

    fun nontokensNamed(driver: GameTestDriver, playerId: EntityId, name: String): List<EntityId> =
        driver.getPermanents(playerId).filter { id ->
            val entity = driver.state.getEntity(id)
            entity?.has<TokenComponent>() == false && entity.get<CardComponent>()?.name == name
        }

    test("paying {R} creates a hasty token copy that is exiled at the next end step") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putPermanentOnBattlefield(you, "Flameshadow Conjuring")
        driver.putLandOnBattlefield(you, "Mountain")

        val bearId = castBear(driver, you)
        resolveTrigger(driver, you, pay = true) shouldBe true

        val tokens = tokensNamed(driver, you, "Test Bear")
        tokens shouldHaveSize 1
        val token = tokens.single()
        driver.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe true
        driver.state.projectedState.getPower(token) shouldBe 2
        driver.state.projectedState.getToughness(token) shouldBe 2
        // Only the token gains haste, not the original.
        driver.state.projectedState.hasKeyword(bearId, Keyword.HASTE) shouldBe false
        // The token entering is a token: it doesn't trigger the enchantment again.
        driver.state.stack.shouldBeEmpty()
        driver.pendingDecision shouldBe null

        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        tokensNamed(driver, you, "Test Bear").shouldBeEmpty()
        nontokensNamed(driver, you, "Test Bear") shouldHaveSize 1
    }

    test("declining the {R} creates no token") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putPermanentOnBattlefield(you, "Flameshadow Conjuring")
        driver.putLandOnBattlefield(you, "Mountain")

        castBear(driver, you)
        resolveTrigger(driver, you, pay = false) shouldBe true

        tokensNamed(driver, you, "Test Bear").shouldBeEmpty()
        nontokensNamed(driver, you, "Test Bear") shouldHaveSize 1
    }

    test("a creature killed with the trigger on the stack is still copied from last-known information") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putPermanentOnBattlefield(you, "Flameshadow Conjuring")
        driver.putLandOnBattlefield(you, "Mountain")

        val bearId = castBear(driver, you)
        driver.state.stack.size shouldBe 1

        // Bolt the Bear in response to the trigger.
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(bearId))
        driver.bothPass()
        driver.getGraveyardCardNames(you).contains("Test Bear") shouldBe true

        resolveTrigger(driver, you, pay = true) shouldBe true

        val tokens = tokensNamed(driver, you, "Test Bear")
        tokens shouldHaveSize 1
        driver.state.projectedState.getPower(tokens.single()) shouldBe 2
        driver.state.projectedState.hasKeyword(tokens.single(), Keyword.HASTE) shouldBe true
    }

    test("a creature that copies the token doesn't get haste") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putPermanentOnBattlefield(you, "Flameshadow Conjuring")
        driver.putLandOnBattlefield(you, "Mountain")

        castBear(driver, you)
        resolveTrigger(driver, you, pay = true) shouldBe true
        val token = tokensNamed(driver, you, "Test Bear").single()

        // Clone enters as a copy of the hasty token. Its own Flameshadow trigger can't be paid
        // (the Mountain is tapped), so no question is asked.
        val clone = driver.putCardInHand(you, "Clone")
        driver.giveMana(you, Color.BLUE, 4)
        driver.castSpell(you, clone)
        driver.bothPass()
        val decision = driver.pendingDecision
        (decision is SelectCardsDecision) shouldBe true
        driver.submitCardSelection(you, listOf(token))
        resolveTrigger(driver, you, pay = false) shouldBe false

        driver.state.getEntity(clone)?.get<CardComponent>()?.name shouldBe "Test Bear"
        driver.state.projectedState.hasKeyword(clone, Keyword.HASTE) shouldBe false
        driver.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe true
    }
})
