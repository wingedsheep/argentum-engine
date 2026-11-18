package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m15.cards.GeneratorServant
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Generator Servant — Magic 2015 #143.
 *
 * {T}, Sacrifice this creature: Add {C}{C}. If any of that mana is spent on a creature spell, it
 * gains haste until end of turn.
 *
 * What the script alone can't show: the Servant is gone before the mana is spent, so the haste has
 * to ride on the mana itself, and each {C} carries its own rider — split across two creature spells,
 * both gain haste (2018-12-07 ruling).
 */
class GeneratorServantScenarioTest : FunSpec({

    val projector = StateProjector()
    val servantAbilityId = GeneratorServant.activatedAbilities[0].id

    val ogre = CardDefinition.creature(
        name = "Test Servant Ogre",
        manaCost = ManaCost.parse("{2}"),
        subtypes = setOf(Subtype("Ogre")),
        power = 3,
        toughness = 3,
    )

    val goblin = CardDefinition.creature(
        name = "Test Servant Goblin",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Goblin")),
        power = 1,
        toughness = 1,
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GeneratorServant, ogre, goblin))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        return driver
    }

    fun sacrificeServant(driver: GameTestDriver, you: EntityId): EntityId {
        val servant = driver.putCreatureOnBattlefield(you, "Generator Servant")
        driver.removeSummoningSickness(servant)
        driver.submitSuccess(ActivateAbility(playerId = you, sourceId = servant, abilityId = servantAbilityId))
        return servant
    }

    test("both {C} spent on a creature spell: the Servant is gone, the creature has haste and attacks at once") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCardInHand(you, "Test Servant Ogre")
        sacrificeServant(driver, you)
        driver.getGraveyardCardNames(you).contains("Generator Servant") shouldBe true

        driver.castSpell(you, creature).error shouldBe null
        driver.bothPass()

        projector.project(driver.state).hasKeyword(creature, Keyword.HASTE) shouldBe true
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(creature), opponent).error shouldBe null
    }

    test("the mana split across two creature spells gives each of them haste") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val first = driver.putCardInHand(you, "Test Servant Goblin")
        val second = driver.putCardInHand(you, "Test Servant Goblin")
        sacrificeServant(driver, you)

        driver.castSpell(you, first).error shouldBe null
        driver.bothPass()
        driver.castSpell(you, second).error shouldBe null
        driver.bothPass()

        val projected = projector.project(driver.state)
        projected.hasKeyword(first, Keyword.HASTE) shouldBe true
        projected.hasKeyword(second, Keyword.HASTE) shouldBe true
    }

    test("a creature cast with ordinary mana has no haste") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCardInHand(you, "Test Servant Goblin")
        driver.giveMana(you, Color.BLUE, 1)
        driver.castSpell(you, creature).error shouldBe null
        driver.bothPass()

        projector.project(driver.state).hasKeyword(creature, Keyword.HASTE) shouldBe false
    }
})
