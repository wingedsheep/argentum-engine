package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fem.cards.Seasinger
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Seasinger (Fallen Empires).
 *
 * Seasinger: {1}{U}{U}
 * Creature — Merfolk
 * 0/1
 * When you control no Islands, sacrifice this creature.
 * You may choose not to untap this creature during your untap step.
 * {T}: Gain control of target creature whose controller controls an Island for as long as you
 * control this creature and this creature remains tapped.
 *
 * Two pieces of SDK vocabulary are exercised here rather than the card's plumbing:
 *  - `StatePredicate.ControllerControls`, which binds the nested filter's "you" to the *candidate's*
 *    controller, so "whose controller controls an Island" is about the creature's side of the table.
 *  - `Duration.WhileYouControlSourceAndSourceTapped`, whose two halves must each end the steal on
 *    their own, permanently (CR 611.2b).
 */
class SeasingerScenarioTest : FunSpec({

    val abilityId = Seasinger.activatedAbilities.first().id
    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Seasinger)
        return driver
    }

    test("steals a creature whose controller controls an Island") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putLandOnBattlefield(alice, "Island")
        driver.putLandOnBattlefield(bob, "Island")

        val seasinger = driver.putCreatureOnBattlefield(alice, "Seasinger")
        driver.removeSummoningSickness(seasinger)
        val target = driver.putCreatureOnBattlefield(bob, "Elvish Warrior")

        driver.submitSuccess(
            ActivateAbility(
                playerId = alice,
                sourceId = seasinger,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        driver.bothPass()

        projector.project(driver.state).getController(target) shouldBe alice
    }

    test("cannot target a creature whose controller controls no Island") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Only Alice controls an Island. Bob's creature is therefore not a legal target, even
        // though Alice — the ability's controller — does control one.
        driver.putLandOnBattlefield(alice, "Island")

        val seasinger = driver.putCreatureOnBattlefield(alice, "Seasinger")
        driver.removeSummoningSickness(seasinger)
        val target = driver.putCreatureOnBattlefield(bob, "Elvish Warrior")

        driver.submit(
            ActivateAbility(
                playerId = alice,
                sourceId = seasinger,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        ).isSuccess shouldBe false
    }

    test("control returns when Seasinger untaps") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putLandOnBattlefield(alice, "Island")
        driver.putLandOnBattlefield(bob, "Island")

        val seasinger = driver.putCreatureOnBattlefield(alice, "Seasinger")
        driver.removeSummoningSickness(seasinger)
        val target = driver.putCreatureOnBattlefield(bob, "Elvish Warrior")

        driver.submitSuccess(
            ActivateAbility(
                playerId = alice,
                sourceId = seasinger,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        driver.bothPass()
        projector.project(driver.state).getController(target) shouldBe alice

        // Next untap step — decline the MAY_NOT_UNTAP option, so Seasinger untaps.
        driver.passPriorityUntil(Step.UNTAP)
        driver.submitCardSelection(alice, emptyList())

        driver.state.getEntity(seasinger)?.has<TappedComponent>() shouldBe false
        projector.project(driver.state).getController(target) shouldBe bob
    }

    test("control persists while Seasinger stays tapped") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putLandOnBattlefield(alice, "Island")
        driver.putLandOnBattlefield(bob, "Island")

        val seasinger = driver.putCreatureOnBattlefield(alice, "Seasinger")
        driver.removeSummoningSickness(seasinger)
        val target = driver.putCreatureOnBattlefield(bob, "Elvish Warrior")

        driver.submitSuccess(
            ActivateAbility(
                playerId = alice,
                sourceId = seasinger,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        driver.bothPass()

        driver.passPriorityUntil(Step.UNTAP)
        driver.submitCardSelection(alice, listOf(seasinger))

        driver.state.getEntity(seasinger)?.has<TappedComponent>() shouldBe true
        projector.project(driver.state).getController(target) shouldBe alice
    }

    test("CR 611.2b — losing control of Seasinger ends the steal, and it does not come back") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putLandOnBattlefield(alice, "Island")
        driver.putLandOnBattlefield(bob, "Island")

        val seasinger = driver.putCreatureOnBattlefield(alice, "Seasinger")
        driver.removeSummoningSickness(seasinger)
        val target = driver.putCreatureOnBattlefield(bob, "Elvish Warrior")

        driver.submitSuccess(
            ActivateAbility(
                playerId = alice,
                sourceId = seasinger,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        driver.bothPass()
        projector.project(driver.state).getController(target) shouldBe alice

        // Bob takes Seasinger. The other half of the duration — "for as long as you control this
        // creature" — has now failed, even though Seasinger is still tapped.
        driver.replaceState(driver.state.updateEntity(seasinger) { c -> c.with(ControllerComponent(bob)) })
        projector.project(driver.state).getController(target) shouldBe bob

        val sbaChecker = com.wingedsheep.engine.mechanics.StateBasedActionChecker(
            cardRegistry = driver.cardRegistry
        )
        driver.replaceState(sbaChecker.checkAndApply(driver.state).newState)

        // The floating control effect is physically gone, not merely gated off...
        driver.state.floatingEffects.none {
            it.duration is Duration.WhileYouControlSourceAndSourceTapped
        } shouldBe true

        // ...so Alice taking Seasinger back does not re-steal the Warrior.
        driver.replaceState(driver.state.updateEntity(seasinger) { c -> c.with(ControllerComponent(alice)) })
        driver.replaceState(sbaChecker.checkAndApply(driver.state).newState)
        projector.project(driver.state).getController(target) shouldBe bob
    }

    test("state trigger sacrifices Seasinger when its controller has no Islands") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val seasinger = driver.putCreatureOnBattlefield(alice, "Seasinger")

        // The state trigger (CR 603.8) is noticed the next time triggers are checked, and the
        // ability then has to resolve off the stack.
        driver.passPriorityUntil(Step.END)

        driver.state.getBattlefield().contains(seasinger) shouldBe false
        driver.assertInGraveyard(alice, "Seasinger")
    }
})
