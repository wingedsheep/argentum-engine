package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.conditions.CreatureCardsInGraveyardAtLeast
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Oversold Cemetery:
 * {1}{B} Enchantment
 * At the beginning of your upkeep, if you have four or more creature cards
 * in your graveyard, you may return target creature card from your graveyard to your hand.
 */
class OversoldCemeteryTest : FunSpec({

    val OversoldCemetery = CardDefinition.enchantment(
        name = "Oversold Cemetery",
        manaCost = ManaCost.parse("{1}{B}"),
        oracleText = "At the beginning of your upkeep, if you have four or more creature cards in your graveyard, you may return target creature card from your graveyard to your hand.",
        script = CardScript.permanent(
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = GameEvent.StepEvent(Step.UPKEEP, Player.You),
                    binding = TriggerBinding.ANY,
                    optional = true,
                    triggerCondition = CreatureCardsInGraveyardAtLeast(4),
                    targetRequirement = TargetObject(
                        filter = TargetFilter(
                            GameObjectFilter.Creature.ownedByYou(),
                            zone = Zone.GRAVEYARD
                        )
                    ),
                    effect = ConditionalEffect(
                        condition = CreatureCardsInGraveyardAtLeast(4),
                        effect = MoveToZoneEffect(
                            target = EffectTarget.ContextTarget(0),
                            destination = Zone.HAND
                        )
                    )
                )
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(OversoldCemetery))
        return driver
    }

    /**
     * Advance to targetPlayer's next upkeep.
     */
    fun advanceToPlayerUpkeep(driver: GameTestDriver, targetPlayer: EntityId) {
        driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        if (driver.activePlayer == targetPlayer) {
            driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        }
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        driver.currentStep shouldBe Step.UPKEEP
        driver.activePlayer shouldBe targetPlayer
    }

    test("with 4 creature cards in graveyard - trigger fires and returns creature to hand") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Oversold Cemetery on the battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Oversold Cemetery")

        // Put 4 creature cards in the graveyard
        val creature1 = driver.putCardInGraveyard(activePlayer, "Grizzly Bears")
        val creature2 = driver.putCardInGraveyard(activePlayer, "Grizzly Bears")
        val creature3 = driver.putCardInGraveyard(activePlayer, "Grizzly Bears")
        val creature4 = driver.putCardInGraveyard(activePlayer, "Grizzly Bears")

        // Advance to the controller's upkeep
        advanceToPlayerUpkeep(driver, activePlayer)

        // Trigger should fire - target selection decision
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        val legalTargets = targetDecision.legalTargets[0] ?: emptyList()

        // All 4 creatures should be valid targets
        legalTargets shouldContain creature1
        legalTargets shouldContain creature4

        // Choose creature1 as target
        driver.submitTargetSelection(activePlayer, listOf(creature1))

        // Resolve the trigger
        driver.bothPass()

        // creature1 should be in hand now, not graveyard
        driver.getGraveyardCardNames(activePlayer).count { it == "Grizzly Bears" } shouldBe 3
        driver.getHand(activePlayer).any { driver.getCardName(it) == "Grizzly Bears" } shouldBe true
    }

    test("with fewer than 4 creature cards in graveyard - trigger does not fire") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Oversold Cemetery on the battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Oversold Cemetery")

        // Put only 3 creature cards in the graveyard
        driver.putCardInGraveyard(activePlayer, "Grizzly Bears")
        driver.putCardInGraveyard(activePlayer, "Grizzly Bears")
        driver.putCardInGraveyard(activePlayer, "Grizzly Bears")

        // Advance to upkeep
        advanceToPlayerUpkeep(driver, activePlayer)

        // The trigger should NOT fire at all (intervening-if condition not met)
        // No target selection decision should appear
        driver.stackSize shouldBe 0

        // All 3 creatures should still be in the graveyard
        driver.getGraveyardCardNames(activePlayer).count { it == "Grizzly Bears" } shouldBe 3
    }

    test("does not trigger on opponent's upkeep") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Oversold Cemetery on the battlefield under activePlayer's control
        driver.putPermanentOnBattlefield(activePlayer, "Oversold Cemetery")

        // From activePlayer's PRECOMBAT_MAIN, the next UPKEEP is the opponent's.
        // No creatures in anyone's graveyard so the trigger has no valid targets
        // and can't fire even if it tried.
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        driver.currentStep shouldBe Step.UPKEEP
        driver.activePlayer shouldBe opponent

        // No trigger should have fired - stack should be empty
        // (Cemetery only triggers on controller's upkeep, not opponent's)
        driver.stackSize shouldBe 0
    }

    test("with exactly 4 creatures - returns one, leaving 3 in graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Oversold Cemetery")

        // Put exactly 4 creature cards in the graveyard
        val target = driver.putCardInGraveyard(activePlayer, "Grizzly Bears")
        driver.putCardInGraveyard(activePlayer, "Grizzly Bears")
        driver.putCardInGraveyard(activePlayer, "Grizzly Bears")
        driver.putCardInGraveyard(activePlayer, "Grizzly Bears")

        val graveyardSizeBefore = driver.getGraveyard(activePlayer).size

        // Advance to upkeep
        advanceToPlayerUpkeep(driver, activePlayer)

        // Select target and resolve
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(activePlayer, listOf(target))
        driver.bothPass()

        // One creature should have moved from graveyard to hand
        driver.getGraveyard(activePlayer).size shouldBe graveyardSizeBefore - 1
        driver.getHand(activePlayer).any { driver.getCardName(it) == "Grizzly Bears" } shouldBe true
    }

    test("does not allow targeting opponent's graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Oversold Cemetery")

        // Put 4 creature cards in activePlayer's graveyard
        driver.putCardInGraveyard(activePlayer, "Grizzly Bears")
        driver.putCardInGraveyard(activePlayer, "Grizzly Bears")
        driver.putCardInGraveyard(activePlayer, "Grizzly Bears")
        driver.putCardInGraveyard(activePlayer, "Grizzly Bears")

        // Put a creature in opponent's graveyard
        val opponentCreature = driver.putCardInGraveyard(opponent, "Grizzly Bears")

        // Advance to upkeep
        advanceToPlayerUpkeep(driver, activePlayer)

        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        val legalTargets = targetDecision.legalTargets[0] ?: emptyList()

        // Opponent's creature should NOT be a legal target
        legalTargets shouldNotContain opponentCreature
    }
})
