package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetPermanent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

/**
 * Tests for the Everglove Courier card and the MAY_NOT_UNTAP / WhileSourceTapped mechanics.
 *
 * Everglove Courier: {2}{G}
 * Creature — Elf
 * 2/1
 * You may choose not to untap Everglove Courier during your untap step.
 * {2}{G}, {T}: Target Elf creature gets +2/+2 and gains trample for as long as
 * Everglove Courier remains tapped.
 */
class EvergloveCourierTest : FunSpec({

    val courierAbilityId = AbilityId(UUID.randomUUID().toString())

    val EvergloveCourier = CardDefinition.creature(
        name = "Everglove Courier",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype("Elf")),
        power = 2,
        toughness = 1,
        keywords = setOf(Keyword.MAY_NOT_UNTAP),
        oracleText = "You may choose not to untap Everglove Courier during your untap step.\n{2}{G}, {T}: Target Elf creature gets +2/+2 and gains trample for as long as Everglove Courier remains tapped.",
        script = CardScript.permanent(
            ActivatedAbility(
                id = courierAbilityId,
                cost = AbilityCost.Composite(listOf(
                    AbilityCost.Mana(ManaCost.parse("{2}{G}")),
                    AbilityCost.Tap
                )),
                effect = ModifyStatsEffect(2, 2, EffectTarget.ContextTarget(0), Duration.WhileSourceTapped()) then
                        GrantKeywordUntilEndOfTurnEffect(Keyword.TRAMPLE, EffectTarget.ContextTarget(0), Duration.WhileSourceTapped()),
                targetRequirement = TargetPermanent(
                    filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Elf"))
                )
            )
        )
    )

    // A simple Elf creature for testing
    val ElvishWarrior = CardDefinition.creature(
        name = "Elvish Warrior",
        manaCost = ManaCost.parse("{G}{G}"),
        subtypes = setOf(Subtype("Elf"), Subtype("Warrior")),
        power = 2,
        toughness = 3,
        oracleText = ""
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EvergloveCourier, ElvishWarrior))
        return driver
    }

    test("Activating courier gives target Elf +2/+2 and trample while courier is tapped") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put courier and an Elf on the battlefield
        val courier = driver.putCreatureOnBattlefield(activePlayer, "Everglove Courier")
        val elf = driver.putCreatureOnBattlefield(activePlayer, "Elvish Warrior")
        driver.removeSummoningSickness(courier)
        driver.removeSummoningSickness(elf)

        // Give mana for the activation cost
        driver.giveMana(activePlayer, Color.GREEN, 3)

        // Activate the courier's ability targeting the Elf
        val activateResult = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = courier,
                abilityId = courierAbilityId,
                targets = listOf(ChosenTarget.Permanent(elf))
            )
        )
        activateResult.isSuccess shouldBe true

        // Let the ability resolve
        driver.bothPass()

        // Courier should be tapped (from the tap cost)
        driver.state.getEntity(courier)?.has<TappedComponent>() shouldBe true

        // Elf should have +2/+2 (2/3 -> 4/5) and trample
        val projected = projector.project(driver.state)
        projector.getProjectedPower(driver.state, elf) shouldBe 4
        projector.getProjectedToughness(driver.state, elf) shouldBe 5
        projected.hasKeyword(elf, Keyword.TRAMPLE) shouldBe true
    }

    test("Buff persists when courier stays tapped through untap step") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put courier and an Elf on the battlefield
        val courier = driver.putCreatureOnBattlefield(activePlayer, "Everglove Courier")
        val elf = driver.putCreatureOnBattlefield(activePlayer, "Elvish Warrior")
        driver.removeSummoningSickness(courier)
        driver.removeSummoningSickness(elf)

        // Activate the courier's ability
        driver.giveMana(activePlayer, Color.GREEN, 3)
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = courier,
                abilityId = courierAbilityId,
                targets = listOf(ChosenTarget.Permanent(elf))
            )
        )
        driver.bothPass()

        // Verify buff is active
        projector.getProjectedPower(driver.state, elf) shouldBe 4
        projector.getProjectedToughness(driver.state, elf) shouldBe 5

        // Pass through rest of turn, opponent's turn, and into our next untap step
        // passPriorityUntil auto-resolves any cleanup discard decisions along the way
        // It stops at our UNTAP step because the MAY_NOT_UNTAP decision pauses the engine there
        driver.passPriorityUntil(Step.UNTAP)

        // MAY_NOT_UNTAP decision should be pending
        val selectDecision = driver.pendingDecision as SelectCardsDecision
        // Choose to keep the courier tapped
        driver.submitCardSelection(activePlayer, listOf(courier))

        // Courier should still be tapped
        driver.state.getEntity(courier)?.has<TappedComponent>() shouldBe true

        // Buff should still be active since courier is still tapped
        val projectedAfter = projector.project(driver.state)
        projector.getProjectedPower(driver.state, elf) shouldBe 4
        projector.getProjectedToughness(driver.state, elf) shouldBe 5
        projectedAfter.hasKeyword(elf, Keyword.TRAMPLE) shouldBe true
    }

    test("Buff does not stack across multiple turns when courier stays tapped") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put courier and an Elf on the battlefield
        val courier = driver.putCreatureOnBattlefield(activePlayer, "Everglove Courier")
        val elf = driver.putCreatureOnBattlefield(activePlayer, "Elvish Warrior")
        driver.removeSummoningSickness(courier)
        driver.removeSummoningSickness(elf)

        // Activate the courier's ability
        driver.giveMana(activePlayer, Color.GREEN, 3)
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = courier,
                abilityId = courierAbilityId,
                targets = listOf(ChosenTarget.Permanent(elf))
            )
        )
        driver.bothPass()

        // Verify buff is active: 2/3 -> 4/5
        projector.getProjectedPower(driver.state, elf) shouldBe 4
        projector.getProjectedToughness(driver.state, elf) shouldBe 5

        // -- First untap cycle: keep courier tapped --
        driver.passPriorityUntil(Step.UNTAP)
        driver.submitCardSelection(activePlayer, listOf(courier))

        // Buff should still be +2/+2, NOT +4/+4
        projector.getProjectedPower(driver.state, elf) shouldBe 4
        projector.getProjectedToughness(driver.state, elf) shouldBe 5

        // Verify floating effects count
        val effectsAfterTurn1 = driver.state.floatingEffects.size

        // -- Second untap cycle: keep courier tapped again --
        driver.passPriorityUntil(Step.UNTAP)
        driver.submitCardSelection(activePlayer, listOf(courier))

        // Buff should STILL be +2/+2, NOT +4/+4
        projector.getProjectedPower(driver.state, elf) shouldBe 4
        projector.getProjectedToughness(driver.state, elf) shouldBe 5

        // Floating effects count should not have grown
        driver.state.floatingEffects.size shouldBe effectsAfterTurn1

        // -- Third untap cycle: keep courier tapped yet again --
        driver.passPriorityUntil(Step.UNTAP)
        driver.submitCardSelection(activePlayer, listOf(courier))

        // Buff should STILL be +2/+2, NOT +6/+6
        projector.getProjectedPower(driver.state, elf) shouldBe 4
        projector.getProjectedToughness(driver.state, elf) shouldBe 5
    }

    test("Buff does not accumulate when courier is untapped and re-activated each turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put courier and an Elf on the battlefield
        val courier = driver.putCreatureOnBattlefield(activePlayer, "Everglove Courier")
        val elf = driver.putCreatureOnBattlefield(activePlayer, "Elvish Warrior")
        driver.removeSummoningSickness(courier)
        driver.removeSummoningSickness(elf)

        // -- Turn 1: Activate courier --
        driver.giveMana(activePlayer, Color.GREEN, 3)
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = courier,
                abilityId = courierAbilityId,
                targets = listOf(ChosenTarget.Permanent(elf))
            )
        )
        driver.bothPass()

        // Elf should have +2/+2 (2/3 -> 4/5)
        projector.getProjectedPower(driver.state, elf) shouldBe 4
        projector.getProjectedToughness(driver.state, elf) shouldBe 5

        // -- Turn 2: Untap courier and re-activate --
        driver.passPriorityUntil(Step.UNTAP)
        // Choose NOT to keep courier tapped (untap it)
        driver.submitCardSelection(activePlayer, emptyList())

        // After untapping, old floating effects should be cleaned up
        driver.state.getEntity(courier)?.has<TappedComponent>() shouldBe false
        projector.getProjectedPower(driver.state, elf) shouldBe 2
        projector.getProjectedToughness(driver.state, elf) shouldBe 3

        // Now re-activate in precombat main
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(activePlayer, Color.GREEN, 3)
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = courier,
                abilityId = courierAbilityId,
                targets = listOf(ChosenTarget.Permanent(elf))
            )
        )
        driver.bothPass()

        // Elf should have +2/+2, NOT +4/+4
        projector.getProjectedPower(driver.state, elf) shouldBe 4
        projector.getProjectedToughness(driver.state, elf) shouldBe 5

        // -- Turn 3: Untap and re-activate again --
        driver.passPriorityUntil(Step.UNTAP)
        driver.submitCardSelection(activePlayer, emptyList())

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(activePlayer, Color.GREEN, 3)
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = courier,
                abilityId = courierAbilityId,
                targets = listOf(ChosenTarget.Permanent(elf))
            )
        )
        driver.bothPass()

        // Elf should STILL have +2/+2, NOT +6/+6
        projector.getProjectedPower(driver.state, elf) shouldBe 4
        projector.getProjectedToughness(driver.state, elf) shouldBe 5
    }

    test("Buff removed when courier is untapped during untap step") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put courier and an Elf on the battlefield
        val courier = driver.putCreatureOnBattlefield(activePlayer, "Everglove Courier")
        val elf = driver.putCreatureOnBattlefield(activePlayer, "Elvish Warrior")
        driver.removeSummoningSickness(courier)
        driver.removeSummoningSickness(elf)

        // Activate the courier's ability
        driver.giveMana(activePlayer, Color.GREEN, 3)
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = courier,
                abilityId = courierAbilityId,
                targets = listOf(ChosenTarget.Permanent(elf))
            )
        )
        driver.bothPass()

        // Verify buff is active
        projector.getProjectedPower(driver.state, elf) shouldBe 4

        // Pass through rest of turn, opponent's turn, and into our next untap step
        driver.passPriorityUntil(Step.UNTAP)

        // MAY_NOT_UNTAP decision should be pending
        (driver.pendingDecision is SelectCardsDecision) shouldBe true

        // Choose NOT to keep the courier tapped (select nothing = untap everything)
        driver.submitCardSelection(activePlayer, emptyList())

        // Courier should now be untapped
        driver.state.getEntity(courier)?.has<TappedComponent>() shouldBe false

        // Buff should be gone since courier is no longer tapped
        val projectedAfter = projector.project(driver.state)
        projector.getProjectedPower(driver.state, elf) shouldBe 2
        projector.getProjectedToughness(driver.state, elf) shouldBe 3
        projectedAfter.hasKeyword(elf, Keyword.TRAMPLE) shouldBe false
    }
})
