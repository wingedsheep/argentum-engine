package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.DwarvenMauler
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Dwarven Mauler (HOB #95) — "Equip abilities you activate that target this creature cost {2} less
 * to activate."
 *
 * The discount is target-scoped, not controller-scoped: an Equip {3} blade costs {1} when it
 * targets the Mauler and the full {3} when it targets anything else. An inline Equip {3} Equipment
 * pins both halves — the same shape Éowyn, Lady of Rohan uses for the untargeted variant.
 */
class DwarvenMaulerScenarioTest : FunSpec({

    val testBlade = card("Test Blade") {
        manaCost = "{1}"
        typeLine = "Artifact — Equipment"
        oracleText = "Equipped creature gets +1/+0.\nEquip {3}"
        equipAbility("{3}")
    }
    val equipId = testBlade.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DwarvenMauler, testBlade))
        return driver
    }

    // Player 1 may not be active at game start (random turn order) — advance until it is.
    fun GameTestDriver.advanceToPlayer1(targetStep: Step) {
        passPriorityUntil(targetStep)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(targetStep)
            safety++
        }
    }

    test("Equip {3} targeting the Mauler resolves paying only {1}") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))

        val mauler = driver.putCreatureOnBattlefield(driver.player1, "Dwarven Mauler")
        val blade = driver.putPermanentOnBattlefield(driver.player1, "Test Blade")

        driver.advanceToPlayer1(Step.PRECOMBAT_MAIN)

        driver.giveColorlessMana(driver.player1, 1)
        driver.submit(
            ActivateAbility(driver.player1, blade, equipId, targets = listOf(ChosenTarget.Permanent(mauler)))
        ).isSuccess shouldBe true
        driver.bothPass()

        withClue("{3} reduced by the Mauler's {2} is payable with a single mana") {
            driver.state.getEntity(blade)?.get<AttachedToComponent>()?.targetId shouldBe mauler
        }
    }

    test("the discount does not apply to an equip targeting another creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))

        driver.putCreatureOnBattlefield(driver.player1, "Dwarven Mauler")
        val bear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val blade = driver.putPermanentOnBattlefield(driver.player1, "Test Blade")

        driver.advanceToPlayer1(Step.PRECOMBAT_MAIN)

        driver.giveColorlessMana(driver.player1, 1)
        driver.submitExpectFailure(
            ActivateAbility(driver.player1, blade, equipId, targets = listOf(ChosenTarget.Permanent(bear)))
        )

        withClue("only the Mauler discounts the equip, so {1} cannot pay Equip {3} onto the Bears") {
            driver.state.getEntity(blade)?.get<AttachedToComponent>().shouldBeNull()
        }

        // With the full {3} available it equips the Bears normally.
        driver.giveColorlessMana(driver.player1, 2)
        driver.submit(
            ActivateAbility(driver.player1, blade, equipId, targets = listOf(ChosenTarget.Permanent(bear)))
        ).isSuccess shouldBe true
        driver.bothPass()
        driver.state.getEntity(blade)?.get<AttachedToComponent>()?.targetId shouldBe bear
    }
})
