package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.enduringstory.EnduringStoryService
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.DwarvenMauler
import com.wingedsheep.mtg.sets.definitions.hob.cards.KiliTheResourceful
import com.wingedsheep.mtg.sets.definitions.hob.cards.MyPrecious
import com.wingedsheep.mtg.sets.definitions.hob.cards.ThorinOakenshield
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Kíli the Resourceful — conditional first-equip alternative cost and a once-per-turn Dwarf or
 * Equipment enters trigger.
 *
 * My Precious is the regression fixture for the equip clause: its printed cost combines {2} and
 * paying 2 life. Kíli's {0} replaces that complete cost rather than acting as a mana reduction.
 */
class KiliTheResourcefulScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + KiliTheResourceful + DwarvenMauler + MyPrecious + ThorinOakenshield
        )
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    val equipId = MyPrecious.activatedAbilities.first().id

    test("with an enduring story the first composite equip costs exactly zero") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putCreatureOnBattlefield(you, "Kíli the Resourceful")
        driver.putCreatureOnBattlefield(you, "Thorin Oakenshield")
        val firstTarget = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val secondTarget = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val precious = driver.putPermanentOnBattlefield(you, "My Precious")

        EnduringStoryService.has(driver.state, you) shouldBe true

        // No mana is available. The alternative cost also replaces "Pay 2 life".
        driver.submit(
            ActivateAbility(you, precious, equipId, targets = listOf(ChosenTarget.Permanent(firstTarget)))
        ).isSuccess shouldBe true
        driver.bothPass()
        driver.state.lifeTotal(you) shouldBe 20
        driver.state.getEntity(precious)?.get<AttachedToComponent>()?.targetId shouldBe firstTarget

        // The use is spent: the next activation has the printed {2}, Pay 2 life cost.
        driver.submitExpectFailure(
            ActivateAbility(you, precious, equipId, targets = listOf(ChosenTarget.Permanent(secondTarget)))
        )
        driver.giveColorlessMana(you, 2)
        driver.submit(
            ActivateAbility(you, precious, equipId, targets = listOf(ChosenTarget.Permanent(secondTarget)))
        ).isSuccess shouldBe true
        driver.bothPass()
        driver.state.lifeTotal(you) shouldBe 18
        driver.state.getEntity(precious)?.get<AttachedToComponent>()?.targetId shouldBe secondTarget
    }

    test("without an enduring story My Precious keeps its full composite equip cost") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putCreatureOnBattlefield(you, "Kíli the Resourceful")
        val target = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val precious = driver.putPermanentOnBattlefield(you, "My Precious")

        EnduringStoryService.has(driver.state, you) shouldBe false
        driver.submitExpectFailure(
            ActivateAbility(you, precious, equipId, targets = listOf(ChosenTarget.Permanent(target)))
        )

        driver.giveColorlessMana(you, 2)
        driver.submit(
            ActivateAbility(you, precious, equipId, targets = listOf(ChosenTarget.Permanent(target)))
        ).isSuccess shouldBe true
        driver.bothPass()
        driver.state.lifeTotal(you) shouldBe 18
    }

    test("another Dwarf entering draws only once each turn") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putCreatureOnBattlefield(you, "Kíli the Resourceful")
        val firstDwarf = driver.putCardInHand(you, "Dwarven Mauler")
        val secondDwarf = driver.putCardInHand(you, "Dwarven Mauler")

        val handBeforeFirst = driver.getHandSize(you)
        driver.giveMana(you, Color.RED)
        driver.castSpell(you, firstDwarf).isSuccess shouldBe true
        driver.bothPass() // Resolve the Dwarf.
        driver.bothPass() // Resolve Kíli's draw trigger.
        driver.getHandSize(you) shouldBe handBeforeFirst

        val handBeforeSecond = driver.getHandSize(you)
        driver.giveMana(you, Color.RED)
        driver.castSpell(you, secondDwarf).isSuccess shouldBe true
        driver.bothPass()
        driver.getHandSize(you) shouldBe handBeforeSecond - 1
    }

    test("another Equipment entering draws a card") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putCreatureOnBattlefield(you, "Kíli the Resourceful")
        val equipment = driver.putCardInHand(you, "My Precious")

        val handBefore = driver.getHandSize(you)
        driver.giveColorlessMana(you, 3)
        driver.castSpell(you, equipment).isSuccess shouldBe true
        driver.bothPass() // Resolve the Equipment.
        driver.bothPass() // Resolve Kíli's draw trigger.
        driver.getHandSize(you) shouldBe handBefore
    }
})
