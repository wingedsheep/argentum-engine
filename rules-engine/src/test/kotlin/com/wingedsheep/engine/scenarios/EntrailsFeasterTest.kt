package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Entrails Feaster:
 * {B}
 * Creature — Zombie Cat
 * 1/1
 * At the beginning of your upkeep, you may exile a creature card from a graveyard.
 * If you do, put a +1/+1 counter on Entrails Feaster.
 * If you don't, tap Entrails Feaster.
 */
class EntrailsFeasterTest : FunSpec({

    val EntrailsFeaster = CardDefinition.creature(
        name = "Entrails Feaster",
        manaCost = ManaCost.parse("{B}"),
        subtypes = setOf(Subtype("Zombie"), Subtype("Cat")),
        power = 1,
        toughness = 1,
        oracleText = "At the beginning of your upkeep, you may exile a creature card from a graveyard. If you do, put a +1/+1 counter on Entrails Feaster. If you don't, tap Entrails Feaster.",
        script = CardScript.permanent(
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = GameEvent.StepEvent(Step.UPKEEP, Player.You),
                    binding = TriggerBinding.ANY,
                    optional = true,
                    targetRequirement = TargetObject(id = "target",
                        filter = TargetFilter.CreatureInGraveyard
                    ),
                    effect = CompositeEffect(
                        listOf(
                            MoveToZoneEffect(EffectTarget.BoundVariable("target"), Zone.EXILE),
                            AddCountersEffect("+1/+1", 1, EffectTarget.Self)
                        )
                    ),
                    elseEffect = TapUntapEffect(EffectTarget.Self, tap = true)
                )
            )
        )
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EntrailsFeaster))
        return driver
    }

    fun advanceToPlayerUpkeep(driver: GameTestDriver, targetPlayer: EntityId) {
        driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        if (driver.activePlayer == targetPlayer) {
            driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        }
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        driver.currentStep shouldBe Step.UPKEEP
        driver.activePlayer shouldBe targetPlayer
    }

    test("exile creature from graveyard - gets +1/+1 counter") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Entrails Feaster on the battlefield
        val feaster = driver.putCreatureOnBattlefield(activePlayer, "Entrails Feaster")

        // Put a creature card in graveyard
        val creature = driver.putCardInGraveyard(activePlayer, "Grizzly Bears")

        // Advance to the controller's upkeep
        advanceToPlayerUpkeep(driver, activePlayer)

        // Trigger should fire - target selection decision
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        // Choose the creature in graveyard as target
        driver.submitTargetSelection(activePlayer, listOf(creature))

        // Resolve the trigger
        driver.bothPass()

        // Creature should be exiled
        driver.state.getGraveyard(activePlayer).contains(creature) shouldBe false
        driver.state.getExile(activePlayer).contains(creature) shouldBe true

        // Feaster should be 2/2 (1/1 base + 1 +1/+1 counter)
        projector.getProjectedPower(driver.state, feaster) shouldBe 2
        projector.getProjectedToughness(driver.state, feaster) shouldBe 2

        // Feaster should NOT be tapped
        driver.isTapped(feaster) shouldBe false
    }

    test("decline to exile - gets tapped") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val feaster = driver.putCreatureOnBattlefield(activePlayer, "Entrails Feaster")
        driver.putCardInGraveyard(activePlayer, "Grizzly Bears")

        advanceToPlayerUpkeep(driver, activePlayer)

        // Trigger fires - target selection
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        // Decline by selecting 0 targets
        driver.submitTargetSelection(activePlayer, emptyList())

        // Resolve the else effect (tap)
        driver.bothPass()

        // Feaster should be tapped
        driver.isTapped(feaster) shouldBe true

        // Feaster should still be 1/1
        projector.getProjectedPower(driver.state, feaster) shouldBe 1
        projector.getProjectedToughness(driver.state, feaster) shouldBe 1
    }

    test("no creature cards in any graveyard - gets tapped automatically") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val feaster = driver.putCreatureOnBattlefield(activePlayer, "Entrails Feaster")

        // No creatures in any graveyard
        advanceToPlayerUpkeep(driver, activePlayer)

        // The else effect should be put on stack automatically (no legal targets)
        // Resolve it
        driver.bothPass()

        // Feaster should be tapped
        driver.isTapped(feaster) shouldBe true

        // Feaster should still be 1/1
        projector.getProjectedPower(driver.state, feaster) shouldBe 1
        projector.getProjectedToughness(driver.state, feaster) shouldBe 1
    }

    test("can exile creature from opponent's graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val feaster = driver.putCreatureOnBattlefield(activePlayer, "Entrails Feaster")

        // Put creature in opponent's graveyard
        val oppCreature = driver.putCardInGraveyard(opponent, "Grizzly Bears")

        advanceToPlayerUpkeep(driver, activePlayer)

        // Trigger fires
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        val legalTargets = targetDecision.legalTargets[0] ?: emptyList()

        // Opponent's creature should be a legal target (any graveyard, not just yours)
        legalTargets.contains(oppCreature) shouldBe true

        // Exile it
        driver.submitTargetSelection(activePlayer, listOf(oppCreature))
        driver.bothPass()

        // Creature should be exiled from opponent's graveyard
        driver.state.getGraveyard(opponent).contains(oppCreature) shouldBe false

        // Feaster should be 2/2
        projector.getProjectedPower(driver.state, feaster) shouldBe 2
        projector.getProjectedToughness(driver.state, feaster) shouldBe 2
    }
})
