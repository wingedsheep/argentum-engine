package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GiveControlToTargetPlayerEffect
import com.wingedsheep.sdk.scripting.OnEnchantedCreatureControllerUpkeep
import com.wingedsheep.sdk.scripting.PayCost
import com.wingedsheep.sdk.scripting.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.targeting.TargetCreature
import com.wingedsheep.sdk.targeting.TargetOpponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Custody Battle.
 *
 * Custody Battle: {1}{R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has "At the beginning of your upkeep, target opponent gains
 * control of this creature unless you sacrifice a land."
 *
 * In a 2-player game, TargetOpponent auto-selects the single opponent,
 * so the trigger goes directly on the stack without a targeting prompt.
 */
class CustodyBattleTest : FunSpec({

    val CustodyBattle = CardDefinition.aura(
        name = "Custody Battle",
        manaCost = ManaCost.parse("{1}{R}"),
        oracleText = "Enchant creature\nEnchanted creature has \"At the beginning of your upkeep, target opponent gains control of this creature unless you sacrifice a land.\"",
        script = CardScript(
            auraTarget = TargetCreature(),
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = OnEnchantedCreatureControllerUpkeep,
                    effect = PayOrSufferEffect(
                        cost = PayCost.Sacrifice(GameObjectFilter.Land),
                        suffer = GiveControlToTargetPlayerEffect(
                            permanent = EffectTarget.EnchantedCreature,
                            newController = EffectTarget.ContextTarget(0)
                        )
                    ),
                    targetRequirement = TargetOpponent()
                )
            )
        )
    )

    // Simple creature to enchant
    val TestCreature = CardDefinition.creature(
        name = "Test Creature",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Warrior")),
        power = 3,
        toughness = 3
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CustodyBattle, TestCreature))
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

    test("sacrifice a land to keep creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on the battlefield
        val creature = driver.putCreatureOnBattlefield(activePlayer, "Test Creature")
        driver.removeSummoningSickness(creature)

        // Put a land on the battlefield to sacrifice later
        val mountain = driver.putLandOnBattlefield(activePlayer, "Mountain")

        // Cast Custody Battle on the creature
        val custodyBattle = driver.putCardInHand(activePlayer, "Custody Battle")
        driver.giveMana(activePlayer, Color.RED, 2)
        driver.castSpell(activePlayer, custodyBattle, listOf(creature))
        driver.bothPass() // Resolve aura

        // Advance to active player's next upkeep
        advanceToPlayerUpkeep(driver, activePlayer)

        // In a 2-player game, TargetOpponent auto-selects; trigger goes on stack
        driver.stackSize shouldBe 1

        // Resolve the trigger
        driver.bothPass()

        // Should have a pending decision to select a land to sacrifice
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()

        // Sacrifice the mountain to keep the creature
        driver.submitCardSelection(activePlayer, listOf(mountain))

        // Creature should still be controlled by active player
        val projected = projector.project(driver.state)
        projected.getController(creature) shouldBe activePlayer

        // Mountain should be gone
        driver.findPermanent(activePlayer, "Mountain") shouldBe null
    }

    test("opponent gains control when player declines to sacrifice a land") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on the battlefield
        val creature = driver.putCreatureOnBattlefield(activePlayer, "Test Creature")
        driver.removeSummoningSickness(creature)

        // Put a land on the battlefield
        driver.putLandOnBattlefield(activePlayer, "Mountain")

        // Cast Custody Battle on the creature
        val custodyBattle = driver.putCardInHand(activePlayer, "Custody Battle")
        driver.giveMana(activePlayer, Color.RED, 2)
        driver.castSpell(activePlayer, custodyBattle, listOf(creature))
        driver.bothPass() // Resolve aura

        // Advance to active player's next upkeep
        advanceToPlayerUpkeep(driver, activePlayer)

        // Trigger auto-targeted opponent and went on stack
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Decline to sacrifice (select empty)
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(activePlayer, emptyList())

        // Opponent should now control the creature
        val projected = projector.project(driver.state)
        projected.getController(creature) shouldBe opponent
    }

    test("opponent gains control automatically when player has no lands") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on the battlefield - no lands!
        val creature = driver.putCreatureOnBattlefield(activePlayer, "Test Creature")
        driver.removeSummoningSickness(creature)

        // Cast Custody Battle on the creature
        val custodyBattle = driver.putCardInHand(activePlayer, "Custody Battle")
        driver.giveMana(activePlayer, Color.RED, 2)
        driver.castSpell(activePlayer, custodyBattle, listOf(creature))
        driver.bothPass() // Resolve aura

        // Advance to active player's next upkeep
        advanceToPlayerUpkeep(driver, activePlayer)

        // Trigger auto-targeted opponent and went on stack
        driver.stackSize shouldBe 1
        driver.bothPass()

        // With no lands, the suffer effect fires automatically (no decision)
        // Opponent should now control the creature
        val projected = projector.project(driver.state)
        projected.getController(creature) shouldBe opponent
    }

    test("hot potato - trigger fires on new controller's upkeep after control change") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on active player's battlefield - no lands
        val creature = driver.putCreatureOnBattlefield(activePlayer, "Test Creature")
        driver.removeSummoningSickness(creature)

        // Opponent has a land they might sacrifice later
        val opponentMountain = driver.putLandOnBattlefield(opponent, "Mountain")

        // Cast Custody Battle on the creature
        val custodyBattle = driver.putCardInHand(activePlayer, "Custody Battle")
        driver.giveMana(activePlayer, Color.RED, 2)
        driver.castSpell(activePlayer, custodyBattle, listOf(creature))
        driver.bothPass() // Resolve aura

        // Advance to active player's upkeep - trigger fires
        advanceToPlayerUpkeep(driver, activePlayer)

        // Trigger auto-targeted opponent and went on stack
        driver.stackSize shouldBe 1
        driver.bothPass()

        // No lands → opponent gains control automatically
        val projected1 = projector.project(driver.state)
        projected1.getController(creature) shouldBe opponent

        // Now advance to opponent's upkeep - the trigger should fire again
        // because the enchanted creature is now controlled by opponent
        advanceToPlayerUpkeep(driver, opponent)

        // Trigger auto-targeted activePlayer and went on stack
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Opponent has a land - sacrifice it to keep
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(opponent, listOf(opponentMountain))

        // Opponent keeps control (they paid the cost)
        val projected2 = projector.project(driver.state)
        projected2.getController(creature) shouldBe opponent

        // Mountain should be gone
        driver.findPermanent(opponent, "Mountain") shouldBe null
    }
})
