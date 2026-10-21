package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.decayed
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for the Decayed keyword (CR 702.147, Innistrad: Midnight Hunt).
 *
 * "This creature can't block, and when it attacks, sacrifice it at end of combat."
 *
 * The engine realizes both halves off the projected `DECAYED` keyword, whatever granted it: a
 * printed `decayed()`, a token created "with decayed" (`CreateTokenEffect.keywords`), or a decayed
 * counter. `StateProjector` sets `cantBlock`; `TriggerDetector` schedules the end-of-combat
 * self-sacrifice (`CreateDelayedTriggerEffect(step = END_COMBAT, effect = SacrificeTarget(Self))`).
 */
class DecayedTest : FunSpec({

    val decayedZombie = card("Decayed Zombie") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Zombie"
        power = 3
        toughness = 3
        decayed()
    }

    val vanillaAttacker = card("Test Marauder") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Human Warrior"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(decayedZombie, vanillaAttacker))
        return driver
    }

    /** Resolve "create a 2/2 black Zombie creature token with decayed" for [player]; returns the token. */
    fun GameTestDriver.createDecayedZombieToken(player: EntityId): EntityId {
        val before = state.getBattlefield().toSet()
        val result = services.effectExecutorRegistry.execute(
            state,
            Effects.CreateToken(
                power = 2, toughness = 2, colors = setOf(Color.BLACK),
                creatureTypes = setOf("Zombie"), keywords = setOf(Keyword.DECAYED)
            ),
            EffectContext(sourceId = null, controllerId = player)
        )
        result.error shouldBe null
        replaceState(result.state)
        return state.getBattlefield().single { it !in before }
    }

    fun GameTestDriver.removeAllAbilities(entity: EntityId, controller: EntityId) {
        val result = services.effectExecutorRegistry.execute(
            state,
            Effects.RemoveAllAbilities(EffectTarget.ContextTarget(0)),
            EffectContext(sourceId = entity, controllerId = controller, targets = listOf(ChosenTarget.Permanent(entity)))
        )
        result.error shouldBe null
        replaceState(result.state)
    }

    test("a creature with decayed can't be declared as a blocker") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        // Attacker swings with a vanilla creature; the defender holds the decayed creature.
        val marauder = driver.putCreatureOnBattlefield(attacker, "Test Marauder")
        driver.removeSummoningSickness(marauder)
        val zombie = driver.putCreatureOnBattlefield(defender, "Decayed Zombie")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(marauder), defender)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        // Declaring the decayed creature as a blocker is illegal.
        driver.submitExpectFailure(
            DeclareBlockers(defender, mapOf(zombie to listOf(marauder)))
        )
    }

    test("when a creature with decayed attacks, a sacrifice trigger is scheduled for end of combat") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val zombie = driver.putCreatureOnBattlefield(attacker, "Decayed Zombie")
        driver.removeSummoningSickness(zombie)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(zombie), defender)
        driver.bothPass() // resolve the attack trigger that creates the delayed sacrifice

        driver.state.delayedTriggers.size shouldBe 1
        driver.state.delayedTriggers.first().fireAtStep shouldBe Step.END_COMBAT
    }

    test("a decayed creature deals combat damage, then is sacrificed at end of combat") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val zombie = driver.putCreatureOnBattlefield(attacker, "Decayed Zombie")
        driver.removeSummoningSickness(zombie)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(zombie), defender)
        driver.bothPass()

        // Combat damage is dealt before the creature is sacrificed (3/3 hits for 3).
        driver.passPriorityUntil(Step.END_COMBAT)
        driver.assertLifeTotal(defender, 17)

        // At end of combat the delayed sacrifice trigger goes on the stack — resolve it.
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        // The creature has been sacrificed and is no longer on the battlefield.
        driver.findPermanent(attacker, "Decayed Zombie") shouldBe null
        driver.state.delayedTriggers.size shouldBe 0
    }

    test("a creature with decayed has no requirement to attack") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val zombie = driver.putCreatureOnBattlefield(attacker, "Decayed Zombie")
        driver.removeSummoningSickness(zombie)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        // Declaring no attackers is legal; decayed adds no attack requirement.
        driver.declareAttackers(attacker, emptyList(), defender)

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        // No attack happened, so no sacrifice trigger and the creature survives.
        driver.findPermanent(attacker, "Decayed Zombie") shouldNotBe null
        driver.state.delayedTriggers.size shouldBe 0
    }
    test("a token created with decayed can't be declared as a blocker") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val marauder = driver.putCreatureOnBattlefield(attacker, "Test Marauder")
        driver.removeSummoningSickness(marauder)
        val token = driver.createDecayedZombieToken(defender)
        driver.state.projectedState.hasKeyword(token, Keyword.DECAYED) shouldBe true

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(marauder), defender)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        driver.submitExpectFailure(DeclareBlockers(defender, mapOf(token to listOf(marauder))))
    }

    test("a token created with decayed is sacrificed at end of combat after it attacks") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val token = driver.createDecayedZombieToken(attacker)
        driver.removeSummoningSickness(token)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(token), defender)
        driver.bothPass()
        driver.state.delayedTriggers.size shouldBe 1

        driver.passPriorityUntil(Step.END_COMBAT)
        driver.assertLifeTotal(defender, 18)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        (token in driver.state.getBattlefield()) shouldBe false
    }

    test("a decayed creature that loses all abilities can block") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val marauder = driver.putCreatureOnBattlefield(attacker, "Test Marauder")
        driver.removeSummoningSickness(marauder)
        val zombie = driver.putCreatureOnBattlefield(defender, "Decayed Zombie")
        driver.removeAllAbilities(zombie, attacker)
        driver.state.projectedState.hasKeyword(zombie, Keyword.DECAYED) shouldBe false

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(marauder), defender)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        driver.submitSuccess(DeclareBlockers(defender, mapOf(zombie to listOf(marauder))))
    }

    test("a decayed creature that loses all abilities is not sacrificed when it attacks") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val zombie = driver.putCreatureOnBattlefield(attacker, "Decayed Zombie")
        driver.removeSummoningSickness(zombie)
        driver.removeAllAbilities(zombie, attacker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(zombie), defender)
        driver.state.stack.size shouldBe 0
        driver.state.delayedTriggers.size shouldBe 0

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.findPermanent(attacker, "Decayed Zombie") shouldNotBe null
    }
})
