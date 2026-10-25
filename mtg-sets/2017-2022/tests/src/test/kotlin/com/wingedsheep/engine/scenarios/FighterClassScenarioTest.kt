package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.FighterClass
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fighter Class {R}{W} — Enchantment — Class (AFR #222).
 *
 * Pins level 2's equip discount against the Class's reached level: the controller-scoped
 * `ReduceEquipCost` lives inside `classLevel(2)`, so a level-1 Fighter Class must not discount
 * anything while a level-2 one takes {2} off an Equip {3}.
 */
class FighterClassScenarioTest : FunSpec({

    // Inline Equip {3} equipment so the {2} reduction is visible as a {1} payment.
    val testBlade = card("Test Blade") {
        manaCost = "{1}"
        typeLine = "Artifact — Equipment"
        oracleText = "Equipped creature gets +1/+0.\nEquip {3}"
        equipAbility("{3}")
    }
    val equipId = testBlade.activatedAbilities.first().id

    data class Board(val driver: GameTestDriver, val bear: EntityId, val blade: EntityId)

    fun setup(level: Int): Board {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FighterClass, testBlade))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val bear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val blade = driver.putPermanentOnBattlefield(you, "Test Blade")
        val fighter = driver.putPermanentOnBattlefield(you, "Fighter Class")
        driver.replaceState(driver.state.updateEntity(fighter) { it.with(ClassLevelComponent(currentLevel = level)) })
        return Board(driver, bear, blade)
    }

    test("level 1: equip costs are not reduced") {
        val (driver, bear, blade) = setup(level = 1)
        val you = driver.activePlayer!!

        driver.giveColorlessMana(you, 1)
        driver.submit(
            ActivateAbility(you, blade, equipId, targets = listOf(ChosenTarget.Permanent(bear)))
        ).outcome shouldNotBe Outcome.Done
    }

    test("level 2: Equip {3} costs {2} less and resolves paying {1}") {
        val (driver, bear, blade) = setup(level = 2)
        val you = driver.activePlayer!!

        driver.giveColorlessMana(you, 1)
        driver.submit(
            ActivateAbility(you, blade, equipId, targets = listOf(ChosenTarget.Permanent(bear)))
        ).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.state.getEntity(blade)?.get<AttachedToComponent>()?.targetId shouldBe bear
    }
})
