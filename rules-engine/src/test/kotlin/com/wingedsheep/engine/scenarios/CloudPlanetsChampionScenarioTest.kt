package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.CloudPlanetsChampion
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Cloud, Planet's Champion (FIN).
 *
 * - "During your turn, as long as Cloud is equipped, it has double strike and indestructible."
 *   (conditional keyword grant: [com.wingedsheep.sdk.dsl.Conditions.IsYourTurn] AND source equipped).
 * - "Equip abilities you activate that target Cloud cost {2} less to activate."
 *   (the target-restricted form of [com.wingedsheep.sdk.scripting.ReduceEquipCost],
 *   `onlyIfTargetIsSource = true`).
 *
 * The discount is pinned with an inline Equip {3} equipment: equipping Cloud is payable with only
 * {1} ({3} − {2}), while equipping any other creature still costs the full {3}.
 */
class CloudPlanetsChampionScenarioTest : FunSpec({

    // Inline Equip {3} equipment so the {2} reduction is visible as a {1} payment.
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
        driver.registerCards(TestCards.all + listOf(CloudPlanetsChampion, testBlade))
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

    // Advance until it is player2's (the opponent's) turn, stopping at [targetStep].
    fun GameTestDriver.advanceToOpponentTurn(targetStep: Step) {
        passPriorityUntil(targetStep)
        var safety = 0
        while (activePlayer != player2 && safety < 50) {
            bothPass()
            passPriorityUntil(targetStep)
            safety++
        }
    }

    test("equip abilities that target Cloud cost {2} less: Equip {3} resolves paying {1}") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))

        val cloud = driver.putCreatureOnBattlefield(driver.player1, "Cloud, Planet's Champion")
        val blade = driver.putPermanentOnBattlefield(driver.player1, "Test Blade")

        driver.advanceToPlayer1(Step.PRECOMBAT_MAIN)

        // With only {1} available, Equip {3} reduced by Cloud's {2} is payable and resolves.
        driver.giveColorlessMana(driver.player1, 1)
        driver.submit(
            ActivateAbility(driver.player1, blade, equipId, targets = listOf(ChosenTarget.Permanent(cloud)))
        ).isSuccess shouldBe true
        driver.bothPass()
        driver.state.getEntity(blade)?.get<AttachedToComponent>()?.targetId shouldBe cloud
    }

    test("equip that targets a non-Cloud creature is NOT discounted: {2} is insufficient for Equip {3}") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))

        // Cloud is on the battlefield (so its static is active) but the equip targets the bear.
        driver.putCreatureOnBattlefield(driver.player1, "Cloud, Planet's Champion")
        val bear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val blade = driver.putPermanentOnBattlefield(driver.player1, "Test Blade")

        driver.advanceToPlayer1(Step.PRECOMBAT_MAIN)

        // Only {2} available; equipping the bear costs the full {3} (no discount) — must fail.
        driver.giveColorlessMana(driver.player1, 2)
        driver.submit(
            ActivateAbility(driver.player1, blade, equipId, targets = listOf(ChosenTarget.Permanent(bear)))
        ).isSuccess shouldBe false
    }

    test("during your turn, equipped Cloud has double strike and indestructible") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))

        val cloud = driver.putCreatureOnBattlefield(driver.player1, "Cloud, Planet's Champion")
        val blade = driver.putPermanentOnBattlefield(driver.player1, "Test Blade")

        driver.advanceToPlayer1(Step.PRECOMBAT_MAIN)
        driver.giveColorlessMana(driver.player1, 1)
        driver.submit(
            ActivateAbility(driver.player1, blade, equipId, targets = listOf(ChosenTarget.Permanent(cloud)))
        ).isSuccess shouldBe true
        driver.bothPass()

        val projected = projector.project(driver.state)
        projected.hasKeyword(cloud, Keyword.DOUBLE_STRIKE) shouldBe true
        projected.hasKeyword(cloud, Keyword.INDESTRUCTIBLE) shouldBe true
    }

    test("unequipped Cloud has neither keyword, even on your turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))

        val cloud = driver.putCreatureOnBattlefield(driver.player1, "Cloud, Planet's Champion")
        driver.advanceToPlayer1(Step.PRECOMBAT_MAIN)

        val projected = projector.project(driver.state)
        projected.hasKeyword(cloud, Keyword.DOUBLE_STRIKE) shouldBe false
        projected.hasKeyword(cloud, Keyword.INDESTRUCTIBLE) shouldBe false
    }

    test("equipped Cloud loses the keywords during the opponent's turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))

        val cloud = driver.putCreatureOnBattlefield(driver.player1, "Cloud, Planet's Champion")
        val blade = driver.putPermanentOnBattlefield(driver.player1, "Test Blade")

        driver.advanceToPlayer1(Step.PRECOMBAT_MAIN)
        driver.giveColorlessMana(driver.player1, 1)
        driver.submit(
            ActivateAbility(driver.player1, blade, equipId, targets = listOf(ChosenTarget.Permanent(cloud)))
        ).isSuccess shouldBe true
        driver.bothPass()

        // Hand the turn to the opponent; Cloud stays equipped but it's no longer "your turn".
        driver.advanceToOpponentTurn(Step.PRECOMBAT_MAIN)

        driver.state.getEntity(blade)?.get<AttachedToComponent>()?.targetId shouldBe cloud
        val projected = projector.project(driver.state)
        projected.hasKeyword(cloud, Keyword.DOUBLE_STRIKE) shouldBe false
        projected.hasKeyword(cloud, Keyword.INDESTRUCTIBLE) shouldBe false
    }
})
