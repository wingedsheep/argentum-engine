package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GlobalEffect
import com.wingedsheep.sdk.scripting.GlobalEffectType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Tests for Grand Melee.
 *
 * Grand Melee ({3}{R}, Enchantment):
 * All creatures attack each combat if able.
 * All creatures block each combat if able.
 */
class GrandMeleeTest : FunSpec({

    val GrandMelee = CardDefinition(
        name = "Grand Melee",
        manaCost = ManaCost.parse("{3}{R}"),
        typeLine = TypeLine.parse("Enchantment"),
        oracleText = "All creatures attack each combat if able.\nAll creatures block each combat if able.",
        script = CardScript.permanent(
            staticAbilities = listOf(
                GlobalEffect(GlobalEffectType.ALL_CREATURES_MUST_ATTACK),
                GlobalEffect(GlobalEffectType.ALL_CREATURES_MUST_BLOCK)
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GrandMelee)
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Grizzly Bears" to 20),
            skipMulligans = true
        )
        return driver
    }

    test("Grand Melee forces creatures to attack if able") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Grand Melee on the battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Grand Melee")

        // Put a creature on the active player's side
        val bear = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.removeSummoningSickness(bear)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Try to declare no attackers - should fail
        val noAttackResult = driver.submit(
            DeclareAttackers(
                playerId = activePlayer,
                attackers = emptyMap()
            )
        )
        noAttackResult.isSuccess shouldBe false
        noAttackResult.error shouldContain "must attack"

        // Declare the bear as attacker - should succeed
        val attackResult = driver.submit(
            DeclareAttackers(
                playerId = activePlayer,
                attackers = mapOf(bear to opponent)
            )
        )
        attackResult.isSuccess shouldBe true
    }

    test("Grand Melee forces creatures to block if able") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Grand Melee on the battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Grand Melee")

        // Put a creature on the active player's side and attack with it
        val attacker = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.removeSummoningSickness(attacker)

        // Put a creature on the opponent's side (to block)
        val blocker = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.removeSummoningSickness(blocker)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Declare attacker (must attack due to Grand Melee)
        driver.submit(
            DeclareAttackers(
                playerId = activePlayer,
                attackers = mapOf(attacker to opponent)
            )
        )

        // Advance to declare blockers
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        // Try to declare no blockers - should fail
        val noBlockResult = driver.submit(
            DeclareBlockers(
                playerId = opponent,
                blockers = emptyMap()
            )
        )
        noBlockResult.isSuccess shouldBe false
        noBlockResult.error shouldContain "must block"

        // Declare the blocker - should succeed
        val blockResult = driver.submit(
            DeclareBlockers(
                playerId = opponent,
                blockers = mapOf(blocker to listOf(attacker))
            )
        )
        blockResult.isSuccess shouldBe true
    }

    test("Grand Melee does not force creatures with summoning sickness to attack") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Grand Melee on the battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Grand Melee")

        // Put a creature without summoning sickness (must attack)
        val readyBear = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.removeSummoningSickness(readyBear)

        // Put a creature with summoning sickness (should not be forced to attack)
        driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        // Don't remove summoning sickness

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Declaring only the ready bear as attacker should succeed
        // (the summoning-sick bear is not forced to attack)
        val attackResult = driver.submit(
            DeclareAttackers(
                playerId = activePlayer,
                attackers = mapOf(readyBear to opponent)
            )
        )
        attackResult.isSuccess shouldBe true
    }

    test("Grand Melee does not force tapped creatures to block") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Grand Melee on the battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Grand Melee")

        // Put a creature on the active player's side and attack with it
        val attacker = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.removeSummoningSickness(attacker)

        // Put a tapped creature on the opponent's side
        val tappedCreature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.removeSummoningSickness(tappedCreature)
        driver.tapPermanent(tappedCreature)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Declare attacker
        driver.submit(
            DeclareAttackers(
                playerId = activePlayer,
                attackers = mapOf(attacker to opponent)
            )
        )

        // Advance to declare blockers
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        // Declaring no blockers should succeed since the creature is tapped
        val result = driver.submit(
            DeclareBlockers(
                playerId = opponent,
                blockers = emptyMap()
            )
        )
        result.isSuccess shouldBe true
    }

    test("Grand Melee effect applies to all players' creatures") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Grand Melee on the battlefield (controlled by active player, but affects all)
        driver.putPermanentOnBattlefield(activePlayer, "Grand Melee")

        // Put creatures on both sides
        val myCreature = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.removeSummoningSickness(myCreature)
        val theirCreature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.removeSummoningSickness(theirCreature)

        // Advance to declare attackers - active player must attack
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val noAttackResult = driver.submit(
            DeclareAttackers(
                playerId = activePlayer,
                attackers = emptyMap()
            )
        )
        noAttackResult.isSuccess shouldBe false

        // Attack with our creature
        driver.submit(
            DeclareAttackers(
                playerId = activePlayer,
                attackers = mapOf(myCreature to opponent)
            )
        )

        // Opponent's creature must block
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val noBlockResult = driver.submit(
            DeclareBlockers(
                playerId = opponent,
                blockers = emptyMap()
            )
        )
        noBlockResult.isSuccess shouldBe false
    }
})
