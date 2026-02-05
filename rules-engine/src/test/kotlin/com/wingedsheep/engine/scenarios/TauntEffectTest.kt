package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.combat.MustAttackPlayerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.Player
import com.wingedsheep.sdk.scripting.TauntEffect
import com.wingedsheep.sdk.targeting.TargetPlayer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for TauntEffect.
 *
 * Taunt ({U} Sorcery): "During target player's next turn, creatures that player
 * controls attack you if able."
 *
 * Key mechanics:
 * - Target: An opponent (the player whose creatures must attack)
 * - Attacker requirement: Creatures must attack the caster of Taunt
 * - "If able": Only creatures that can legally attack must do so (excludes tapped,
 *   summoning sickness, defender)
 * - Duration: Only during that player's next turn
 */
class TauntEffectTest : FunSpec({

    // Test card that mimics Taunt
    val Taunt = CardDefinition.sorcery(
        name = "Taunt",
        manaCost = ManaCost.parse("{U}"),
        oracleText = "During target player's next turn, creatures that player controls attack you if able.",
        script = CardScript.spell(
            effect = TauntEffect(EffectTarget.PlayerRef(Player.TargetPlayer)),
            TargetPlayer()
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Taunt)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 20,
                "Grizzly Bears" to 20
            ),
            skipMulligans = true
        )
        return driver
    }

    test("Taunt adds MustAttackPlayerComponent to target opponent") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Taunt in hand and give mana
        val taunt = driver.putCardInHand(caster, "Taunt")
        driver.giveMana(caster, Color.BLUE, 1)

        // Cast Taunt targeting opponent (no need to pass explicit target, TargetOpponent auto-selects)
        val castResult = driver.castSpell(caster, taunt, listOf(opponent))
        castResult.isSuccess shouldBe true

        // Resolve the spell
        driver.bothPass()

        // Verify opponent has MustAttackPlayerComponent
        val opponentEntity = driver.state.getEntity(opponent)
        opponentEntity?.has<MustAttackPlayerComponent>() shouldBe true

        // Verify the defender is the caster
        val component = opponentEntity?.get<MustAttackPlayerComponent>()
        component?.defenderId shouldBe caster
        component?.activeThisTurn shouldBe false // Not active until their turn
    }

    test("MustAttackPlayerComponent activates on target player's turn") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Taunt in hand and give mana
        val taunt = driver.putCardInHand(caster, "Taunt")
        driver.giveMana(caster, Color.BLUE, 1)

        // Cast Taunt targeting opponent
        driver.castSpell(caster, taunt, listOf(opponent))
        driver.bothPass() // Resolve spell

        // Advance to opponent's turn
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass() // End current player's turn

        // Now it's opponent's turn
        driver.activePlayer shouldBe opponent

        // Component should now be active
        val opponentEntity = driver.state.getEntity(opponent)
        val component = opponentEntity?.get<MustAttackPlayerComponent>()
        component?.activeThisTurn shouldBe true
    }

    test("creatures must attack when Taunt is active") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Taunt in hand and give mana
        val taunt = driver.putCardInHand(caster, "Taunt")
        driver.giveMana(caster, Color.BLUE, 1)

        // Cast Taunt targeting opponent
        driver.castSpell(caster, taunt, listOf(opponent))
        driver.bothPass() // Resolve spell

        // Advance to opponent's turn
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass()

        // Put a creature on battlefield for opponent
        val creature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.removeSummoningSickness(creature)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS

        // Try to declare no attackers - should fail
        val noAttackResult = driver.declareAttackers(opponent, emptyMap())
        noAttackResult.isSuccess shouldBe false
    }

    test("creatures must attack the Taunt caster specifically") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Taunt in hand and give mana
        val taunt = driver.putCardInHand(caster, "Taunt")
        driver.giveMana(caster, Color.BLUE, 1)

        // Cast Taunt targeting opponent
        driver.castSpell(caster, taunt, listOf(opponent))
        driver.bothPass() // Resolve spell

        // Advance to opponent's turn
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass()

        // Put a creature on battlefield for opponent
        val creature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.removeSummoningSickness(creature)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Declare the creature attacking the caster - should succeed
        val attackCasterResult = driver.declareAttackers(opponent, listOf(creature), caster)
        attackCasterResult.isSuccess shouldBe true
    }

    test("MustAttackPlayerComponent is removed after combat") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Taunt in hand and give mana
        val taunt = driver.putCardInHand(caster, "Taunt")
        driver.giveMana(caster, Color.BLUE, 1)

        // Cast Taunt targeting opponent
        driver.castSpell(caster, taunt, listOf(opponent))
        driver.bothPass() // Resolve spell

        // Advance to opponent's turn
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass()

        // Put a creature on battlefield for opponent
        val creature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.removeSummoningSickness(creature)

        // Advance to declare attackers and declare attack
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(opponent, listOf(creature), caster)
        driver.bothPass()

        // Declare no blockers
        driver.declareNoBlockers(caster)
        driver.bothPass()

        // Pass through combat damage steps
        driver.passPriorityUntil(Step.END_COMBAT)

        // After combat ends, component should be removed
        val opponentEntity = driver.state.getEntity(opponent)
        opponentEntity?.has<MustAttackPlayerComponent>() shouldBe false
    }

    test("tapped creatures do not need to attack") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Taunt in hand and give mana
        val taunt = driver.putCardInHand(caster, "Taunt")
        driver.giveMana(caster, Color.BLUE, 1)

        // Cast Taunt targeting opponent
        driver.castSpell(caster, taunt, listOf(opponent))
        driver.bothPass() // Resolve spell

        // Advance to opponent's turn
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass()

        // Put a creature on battlefield and tap it
        val creature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.removeSummoningSickness(creature)
        driver.tapPermanent(creature)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Should be able to declare no attackers (creature is tapped)
        val noAttackResult = driver.declareAttackers(opponent, emptyMap())
        noAttackResult.isSuccess shouldBe true
    }

    test("creatures with summoning sickness do not need to attack") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Taunt in hand and give mana
        val taunt = driver.putCardInHand(caster, "Taunt")
        driver.giveMana(caster, Color.BLUE, 1)

        // Cast Taunt targeting opponent
        driver.castSpell(caster, taunt, listOf(opponent))
        driver.bothPass() // Resolve spell

        // Advance to opponent's turn
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass()

        // Put a creature on battlefield (has summoning sickness by default)
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        // Do NOT remove summoning sickness

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Should be able to declare no attackers (creature has summoning sickness)
        val noAttackResult = driver.declareAttackers(opponent, emptyMap())
        noAttackResult.isSuccess shouldBe true
    }

    test("multiple creatures must all attack") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Taunt in hand and give mana
        val taunt = driver.putCardInHand(caster, "Taunt")
        driver.giveMana(caster, Color.BLUE, 1)

        // Cast Taunt targeting opponent
        driver.castSpell(caster, taunt, listOf(opponent))
        driver.bothPass() // Resolve spell

        // Advance to opponent's turn
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass()

        // Put two creatures on battlefield for opponent
        val creature1 = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.removeSummoningSickness(creature1)
        val creature2 = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.removeSummoningSickness(creature2)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Try to declare only one attacker - should fail
        val oneAttackResult = driver.declareAttackers(opponent, listOf(creature1), caster)
        oneAttackResult.isSuccess shouldBe false

        // Declare both attackers - should succeed
        val bothAttackResult = driver.declareAttackers(opponent, listOf(creature1, creature2), caster)
        bothAttackResult.isSuccess shouldBe true
    }

    test("cannot target yourself with Taunt") {
        val driver = createDriver()
        val caster = driver.activePlayer!!

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Taunt in hand and give mana
        val taunt = driver.putCardInHand(caster, "Taunt")
        driver.giveMana(caster, Color.BLUE, 1)

        // Cast Taunt targeting self
        val castResult = driver.castSpell(caster, taunt, listOf(caster))
        castResult.isSuccess shouldBe true // Casting succeeds

        // Resolve the spell - effect should fail (cannot target self)
        driver.bothPass()

        // Caster should NOT have MustAttackPlayerComponent (effect failed)
        val casterEntity = driver.state.getEntity(caster)
        casterEntity?.has<MustAttackPlayerComponent>() shouldBe false
    }
})
