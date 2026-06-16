package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.arn.cards.OldManOfTheSea
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Old Man of the Sea (Arabian Nights).
 *
 * Old Man of the Sea: {1}{U}{U}
 * Creature — Djinn
 * 2/3
 * You may choose not to untap this creature during your untap step.
 * {T}: Gain control of target creature with power less than or equal to this creature's
 * power for as long as this creature remains tapped and that creature's power remains
 * less than or equal to this creature's power.
 */
class OldManOfTheSeaScenarioTest : FunSpec({

    val abilityId = OldManOfTheSea.activatedAbilities.first().id
    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Tap to steal a creature with power equal to source's power") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val oldMan = driver.putCreatureOnBattlefield(activePlayer, "Old Man of the Sea")
        driver.removeSummoningSickness(oldMan)

        // Elvish Warrior is 2/3 — power 2 equals Old Man's 2, so it's a legal target.
        val target = driver.putCreatureOnBattlefield(opponent, "Elvish Warrior")

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = oldMan,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        val projected = projector.project(driver.state)
        projected.getController(target) shouldBe activePlayer
    }

    test("Cannot target a creature with power greater than source's power") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val oldMan = driver.putCreatureOnBattlefield(activePlayer, "Old Man of the Sea")
        driver.removeSummoningSickness(oldMan)

        // Centaur Courser is 3/3 — power 3 exceeds Old Man's 2.
        val target = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = oldMan,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        result.isSuccess shouldBe false
    }

    test("Control returns when Old Man of the Sea is untapped") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val oldMan = driver.putCreatureOnBattlefield(activePlayer, "Old Man of the Sea")
        driver.removeSummoningSickness(oldMan)
        val target = driver.putCreatureOnBattlefield(opponent, "Elvish Warrior")

        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = oldMan,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        driver.bothPass()
        projector.project(driver.state).getController(target) shouldBe activePlayer

        // Next untap step — decline the MAY_NOT_UNTAP option (select empty list).
        driver.passPriorityUntil(Step.UNTAP)
        driver.submitCardSelection(activePlayer, emptyList())

        driver.state.getEntity(oldMan)?.has<TappedComponent>() shouldBe false
        projector.project(driver.state).getController(target) shouldBe opponent
    }

    test("Control persists while Old Man of the Sea remains tapped") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val oldMan = driver.putCreatureOnBattlefield(activePlayer, "Old Man of the Sea")
        driver.removeSummoningSickness(oldMan)
        val target = driver.putCreatureOnBattlefield(opponent, "Elvish Warrior")

        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = oldMan,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        driver.bothPass()

        // Next untap step — choose to keep Old Man tapped.
        driver.passPriorityUntil(Step.UNTAP)
        driver.submitCardSelection(activePlayer, listOf(oldMan))

        driver.state.getEntity(oldMan)?.has<TappedComponent>() shouldBe true
        projector.project(driver.state).getController(target) shouldBe activePlayer
    }

    test("Control reverts when stolen creature gains +1/+1 counters past source's power") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val oldMan = driver.putCreatureOnBattlefield(activePlayer, "Old Man of the Sea")
        driver.removeSummoningSickness(oldMan)
        val target = driver.putCreatureOnBattlefield(opponent, "Elvish Warrior")  // 2/3

        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = oldMan,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        driver.bothPass()
        projector.project(driver.state).getController(target) shouldBe activePlayer

        // Two +1/+1 counters push the stolen creature's power 2 → 4, exceeding Old Man's 2.
        // The per-entity gate should drop the control effect for that target.
        driver.replaceState(driver.state.updateEntity(target) { c ->
            c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
        })

        projector.project(driver.state).getController(target) shouldBe opponent
    }

    test("Control reverts when stolen creature is pumped by a Layer-7 floating effect") {
        // Aggressive-Urge-style: a temporary +1/+1 floating effect (NOT a counter) on the
        // stolen creature. The post-Layer-7 fix-up must see the projected power and revert
        // control even though the pump never touches CountersComponent.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val oldMan = driver.putCreatureOnBattlefield(activePlayer, "Old Man of the Sea")
        driver.removeSummoningSickness(oldMan)
        val target = driver.putCreatureOnBattlefield(opponent, "Elvish Warrior")  // 2/3

        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = oldMan,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        driver.bothPass()
        projector.project(driver.state).getController(target) shouldBe activePlayer

        // Layer-7 +2/+0 floating effect on the stolen creature (Giant Growth-style).
        // Power goes 2 → 4, exceeding Old Man's 2. The post-Layer-7 fix-up must catch this.
        val ctx = EffectContext(
            sourceId = oldMan,
            controllerId = opponent,
        )
        driver.replaceState(
            driver.state.addFloatingEffect(
                layer = Layer.POWER_TOUGHNESS,
                modification = SerializableModification.ModifyPowerToughness(powerMod = 2, toughnessMod = 0),
                affectedEntities = setOf(target),
                duration = Duration.EndOfTurn,
                context = ctx,
            )
        )

        projector.project(driver.state).getController(target) shouldBe opponent
    }

    test("CR 611.2b — control does NOT return after a temporary pump wears off (one-way latch)") {
        // The reversible projection gate alone would re-steal the creature once its power drops
        // back to ≤ Old Man's. CR 611.2b: a "for as long as" duration ends permanently once its
        // condition fails. EndedDurationExpiryCheck physically removes the control effect the
        // moment power is exceeded, so the pump wearing off must NOT bring control back.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val oldMan = driver.putCreatureOnBattlefield(activePlayer, "Old Man of the Sea")
        driver.removeSummoningSickness(oldMan)
        val target = driver.putCreatureOnBattlefield(opponent, "Elvish Warrior")  // 2/3

        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = oldMan,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        driver.bothPass()
        projector.project(driver.state).getController(target) shouldBe activePlayer

        val sbaChecker = com.wingedsheep.engine.mechanics.StateBasedActionChecker(
            cardRegistry = driver.cardRegistry
        )

        // Temporary +2/+0 pump: power 2 → 4 > Old Man's 2. SBAs latch the steal off for good.
        val ctx = EffectContext(sourceId = oldMan, controllerId = opponent)
        driver.replaceState(
            driver.state.addFloatingEffect(
                layer = Layer.POWER_TOUGHNESS,
                modification = SerializableModification.ModifyPowerToughness(powerMod = 2, toughnessMod = 0),
                affectedEntities = setOf(target),
                duration = Duration.EndOfTurn,
                context = ctx,
            )
        )
        driver.replaceState(sbaChecker.checkAndApply(driver.state).newState)
        projector.project(driver.state).getController(target) shouldBe opponent

        // The control effect must be gone — not merely hidden by the power gate.
        driver.state.floatingEffects.none {
            it.duration is Duration.WhileSourceTappedAndAffectedPowerAtMostSource
        } shouldBe true

        // The pump wears off (remove the EndOfTurn effect); Old Man is still tapped and the
        // Warrior is back to power 2 ≤ 2 — but control must stay with its owner.
        driver.replaceState(
            driver.state.copy(
                floatingEffects = driver.state.floatingEffects.filterNot { it.duration == Duration.EndOfTurn }
            )
        )
        driver.replaceState(sbaChecker.checkAndApply(driver.state).newState)
        projector.project(driver.state).getController(target) shouldBe opponent
    }

    test("CR 506.4 — stolen attacker is removed from combat when control reverts mid-attack") {
        // Alice steals Bob's Elvish Warrior with Old Man and attacks Bob with it. Bob plays
        // an Aggressive-Urge-style pump on the Warrior, pushing its power past Old Man's.
        // The post-Layer-7 power gate reverts the controller to Bob; the CR 506.4 SBA then
        // removes the Warrior from combat so it deals zero damage during combat damage.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val oldMan = driver.putCreatureOnBattlefield(activePlayer, "Old Man of the Sea")
        driver.removeSummoningSickness(oldMan)
        val target = driver.putCreatureOnBattlefield(opponent, "Elvish Warrior")  // 2/3

        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = oldMan,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        driver.bothPass()
        projector.project(driver.state).getController(target) shouldBe activePlayer

        // The stolen Warrior is no longer summoning-sick once Alice controls it. Clear it
        // explicitly to mirror what a real "since your last turn began" check would say.
        driver.removeSummoningSickness(target)

        // Advance to declare attackers and attack with the stolen Warrior.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val opponentLifeBefore = driver.state.getEntity(opponent)
            ?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life ?: 20
        driver.declareAttackers(activePlayer, listOf(target), opponent)
        driver.state.getEntity(target)
            ?.has<com.wingedsheep.engine.state.components.combat.AttackingComponent>() shouldBe true

        // Bob pumps the (stolen) Warrior with a +2/+0 floating effect. Power becomes 4 > Old
        // Man's 2 → power-gate reverts control to Bob → CR 506.4 removes Warrior from combat.
        val ctx = EffectContext(
            sourceId = oldMan,
            controllerId = opponent,
        )
        driver.replaceState(
            driver.state.addFloatingEffect(
                layer = Layer.POWER_TOUGHNESS,
                modification = SerializableModification.ModifyPowerToughness(powerMod = 2, toughnessMod = 0),
                affectedEntities = setOf(target),
                duration = Duration.EndOfTurn,
                context = ctx,
            )
        )

        // Run state-based actions and verify the Warrior is no longer attacking.
        val sbaChecker = com.wingedsheep.engine.mechanics.StateBasedActionChecker(
            cardRegistry = driver.cardRegistry
        )
        val sbaResult = sbaChecker.checkAndApply(driver.state)
        sbaResult.isSuccess shouldBe true
        driver.replaceState(sbaResult.newState)

        driver.state.getEntity(target)
            ?.has<com.wingedsheep.engine.state.components.combat.AttackingComponent>() shouldBe false
        projector.project(driver.state).getController(target) shouldBe opponent

        // Bob's life total is unchanged once we finish out combat damage.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        val opponentLifeAfter = driver.state.getEntity(opponent)
            ?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life ?: 20
        opponentLifeAfter shouldBe opponentLifeBefore
    }

    test("Pumping Old Man of the Sea with counters expands its valid-target range") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val oldMan = driver.putCreatureOnBattlefield(activePlayer, "Old Man of the Sea")
        driver.removeSummoningSickness(oldMan)
        val target = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")  // 3/3

        // Without a pump, targeting a 3-power creature is illegal (verified by an earlier test).
        // With two +1/+1 counters, Old Man's power becomes 4 — the Centaur (3) is now in range.
        driver.replaceState(driver.state.updateEntity(oldMan) { c ->
            c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
        })

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = oldMan,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()
        projector.project(driver.state).getController(target) shouldBe activePlayer
    }
})
