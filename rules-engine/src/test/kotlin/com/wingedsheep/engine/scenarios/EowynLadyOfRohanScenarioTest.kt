package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.EowynLadyOfRohan
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Éowyn, Lady of Rohan (LTR).
 *
 * - At the beginning of combat on your turn, target creature gains your choice of first strike or
 *   vigilance until end of turn. If that creature is equipped, it gains both instead.
 * - "Equip abilities you activate cost {1} less to activate." (new [ReduceEquipCost] static).
 *
 * The equip-cost-reduction clause is pinned with an inline Equip {3} equipment so the {1} discount
 * is observable (the creature equips for {2} instead of {3}), distinct from Forge Anew's
 * free-first-equip behaviour.
 */
class EowynLadyOfRohanScenarioTest : FunSpec({

    // Inline Equip {3} equipment so the {1} reduction is visible as a {2} payment.
    val testBlade = card("Test Blade") {
        manaCost = "{1}"
        typeLine = "Artifact — Equipment"
        oracleText = "Equipped creature gets +1/+0.\nEquip {3}"
        equipAbility("{3}")
    }
    val equipId = testBlade.activatedAbilities.first().id

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EowynLadyOfRohan, testBlade))
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

    test("unequipped target: controller chooses first strike") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))

        val bear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.putCreatureOnBattlefield(driver.player1, "Éowyn, Lady of Rohan")

        driver.advanceToPlayer1(Step.BEGIN_COMBAT)

        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        driver.submitTargetSelection(driver.player1, listOf(bear))
        driver.bothPass()

        // Modal choice: option 0 = First strike, option 1 = Vigilance.
        val modeDecision = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(driver.player1, OptionChosenResponse(modeDecision.id, 0))

        val projected = projector.project(driver.state)
        projected.hasKeyword(bear, Keyword.FIRST_STRIKE) shouldBe true
        projected.hasKeyword(bear, Keyword.VIGILANCE) shouldBe false
    }

    test("unequipped target: controller chooses vigilance") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))

        val bear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.putCreatureOnBattlefield(driver.player1, "Éowyn, Lady of Rohan")

        driver.advanceToPlayer1(Step.BEGIN_COMBAT)

        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        driver.submitTargetSelection(driver.player1, listOf(bear))
        driver.bothPass()

        val modeDecision = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(driver.player1, OptionChosenResponse(modeDecision.id, 1))

        val projected = projector.project(driver.state)
        projected.hasKeyword(bear, Keyword.VIGILANCE) shouldBe true
        projected.hasKeyword(bear, Keyword.FIRST_STRIKE) shouldBe false
    }

    test("equipped target gains both first strike and vigilance, with no modal choice") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))

        val bear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.putCreatureOnBattlefield(driver.player1, "Éowyn, Lady of Rohan")
        val blade = driver.putPermanentOnBattlefield(driver.player1, "Test Blade")

        // Equip the blade onto the bear during player1's precombat main, then advance to combat.
        driver.advanceToPlayer1(Step.PRECOMBAT_MAIN)
        driver.giveColorlessMana(driver.player1, 2)
        driver.submit(
            ActivateAbility(driver.player1, blade, equipId, targets = listOf(ChosenTarget.Permanent(bear)))
        ).isSuccess shouldBe true
        driver.bothPass()
        driver.state.getEntity(blade)?.get<AttachedToComponent>()?.targetId shouldBe bear

        driver.passPriorityUntil(Step.BEGIN_COMBAT)

        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        driver.submitTargetSelection(driver.player1, listOf(bear))
        driver.bothPass()

        // Equipped: no modal decision — both keywords are granted directly.
        (driver.pendingDecision as? ChooseOptionDecision).shouldBeNull()

        val projected = projector.project(driver.state)
        projected.hasKeyword(bear, Keyword.FIRST_STRIKE) shouldBe true
        projected.hasKeyword(bear, Keyword.VIGILANCE) shouldBe true
    }

    test("equip abilities you activate cost {1} less: Equip {3} resolves paying {2}") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))

        val bear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.putCreatureOnBattlefield(driver.player1, "Éowyn, Lady of Rohan")
        val blade = driver.putPermanentOnBattlefield(driver.player1, "Test Blade")

        driver.advanceToPlayer1(Step.PRECOMBAT_MAIN)

        // With only {2} available, Equip {3} reduced by Éowyn's {1} is payable and resolves.
        driver.giveColorlessMana(driver.player1, 2)
        driver.submit(
            ActivateAbility(driver.player1, blade, equipId, targets = listOf(ChosenTarget.Permanent(bear)))
        ).isSuccess shouldBe true
        driver.bothPass()
        driver.state.getEntity(blade)?.get<AttachedToComponent>()?.targetId shouldBe bear
    }
})
