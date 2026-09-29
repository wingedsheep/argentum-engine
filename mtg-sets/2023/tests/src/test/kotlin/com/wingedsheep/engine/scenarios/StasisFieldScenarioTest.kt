package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.StasisField
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Stasis Field — {1}{U} Aura: enchanted creature has base P/T 0/2, has defender,
 * and loses all other abilities.
 */
class StasisFieldScenarioTest : FunSpec({

    val FlyingCreature = CardDefinition.creature(
        name = "Stasis Test Flyer",
        manaCost = ManaCost.parse("{2}{R}"),
        subtypes = setOf(Subtype("Bird")),
        power = 3,
        toughness = 3,
        keywords = setOf(Keyword.FLYING)
    )

    test("enchanted creature becomes a 0/2 defender with no other abilities") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FlyingCreature, StasisField))
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Mountain" to 20))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(activePlayer, "Stasis Test Flyer")
        val aura = driver.putCardInHand(activePlayer, "Stasis Field")
        driver.giveMana(activePlayer, Color.BLUE, 1)
        driver.giveColorlessMana(activePlayer, 1)
        driver.castSpell(activePlayer, aura, listOf(creature))
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.getPower(creature) shouldBe 0
        projected.getToughness(creature) shouldBe 2
        projected.hasKeyword(creature, Keyword.DEFENDER) shouldBe true
        projected.hasKeyword(creature, Keyword.FLYING) shouldBe false
        projected.hasLostAllAbilities(creature) shouldBe true
    }
})
