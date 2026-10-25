package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.RangerClass
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ranger Class {1}{G} — Enchantment — Class (AFR #202).
 *
 * Pins the level gating of the two higher class abilities: the level-2 "whenever you attack"
 * counter trigger fires once per combat onto the chosen attacker, and the level-3 top-of-library
 * creature cast is offered only once the Class has reached level 3.
 */
class RangerClassScenarioTest : FunSpec({

    fun setup(level: Int): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(RangerClass)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        driver.putPermanentOnBattlefield(you, "Ranger Class", classLevel = level)
        return driver to you
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.canCastFromLibrary(you: EntityId, name: String): Boolean =
        legalActions(you).any {
            it.actionType == "CastSpell" && it.description.contains(name) && it.sourceZone == "LIBRARY"
        }

    test("level 1: entering creates a 2/2 green Wolf") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(RangerClass)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val ranger = driver.putCardInHand(you, "Ranger Class")
        driver.giveMana(you, Color.GREEN, 2)
        driver.castSpell(you, ranger).error shouldBe null
        driver.bothPass() // resolve the Class
        driver.bothPass() // resolve the enters trigger

        val wolf = driver.getCreatures(you).single { driver.state.projectedState.hasSubtype(it, "Wolf") }
        driver.state.projectedState.getPower(wolf) shouldBe 2
        driver.state.projectedState.getToughness(wolf) shouldBe 2
    }

    test("level 2: attacking puts one +1/+1 counter on the targeted attacker") {
        val (driver, you) = setup(level = 2)
        val first = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val second = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        driver.removeSummoningSickness(first)
        driver.removeSummoningSickness(second)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(first, second), driver.getOpponent(you)).error shouldBe null
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(you, listOf(second)).error shouldBe null
        driver.bothPass()

        driver.plusOneCounters(second) shouldBe 1
        driver.plusOneCounters(first) shouldBe 0
    }

    test("level 1: attacking does not trigger the level-2 ability") {
        val (driver, you) = setup(level = 1)
        val bears = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        driver.removeSummoningSickness(bears)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(bears), driver.getOpponent(you)).error shouldBe null

        driver.pendingDecision shouldBe null
        driver.stackSize shouldBe 0
        driver.plusOneCounters(bears) shouldBe 0
    }

    test("level 3: a creature on top of your library can be cast; at level 2 it can't") {
        val (driver, you) = setup(level = 2)
        val bears = driver.putCardOnTopOfLibrary(you, "Grizzly Bears")
        driver.giveMana(you, Color.GREEN, 2)
        driver.canCastFromLibrary(you, "Grizzly Bears") shouldBe false

        val ranger = driver.findPermanent(you, "Ranger Class")!!
        driver.replaceState(driver.state.updateEntity(ranger) { it.with(ClassLevelComponent(currentLevel = 3)) })
        driver.canCastFromLibrary(you, "Grizzly Bears") shouldBe true

        driver.castSpell(you, bears).error shouldBe null
        driver.bothPass()
        driver.findPermanent(you, "Grizzly Bears") shouldBe bears
    }
})
