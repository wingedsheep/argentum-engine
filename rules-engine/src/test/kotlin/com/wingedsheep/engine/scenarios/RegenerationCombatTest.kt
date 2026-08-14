package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for regeneration interaction with combat.
 *
 * When a creature regenerates, it is tapped, all damage is removed, and it is
 * removed from combat. A creature removed from combat should not deal combat damage.
 *
 * Key scenarios:
 * - Blocker regenerates from first strike damage → does not deal normal combat damage
 * - Blocker regenerates from a damage spell during combat → does not deal normal combat damage
 * - Attacker regenerates from first strike damage → does not deal normal combat damage
 * - Blocked attacker whose blocker regenerated → still "blocked", deals no player damage (no trample)
 */
class RegenerationCombatTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    /**
     * Add a regeneration shield to a creature via floating effect.
     */
    fun GameTestDriver.addRegenerationShield(entityId: EntityId, controllerId: EntityId) {
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                modification = SerializableModification.RegenerationShield,
                affectedEntities = setOf(entityId)
            ),
            duration = Duration.EndOfTurn,
            sourceId = null,
            controllerId = controllerId,
            timestamp = System.currentTimeMillis()
        )
        replaceState(state.copy(floatingEffects = state.floatingEffects + floatingEffect))
    }

    test("blocker with regeneration shield survives first strike but does not deal normal combat damage") {
        // 3/1 first strike attacks, 3/3 with regen shield blocks
        // First strike step: 3/1 deals 3 to 3/3 (lethal) → regeneration replaces destruction
        //   → 3/3 is tapped, damage removed, removed from combat
        // Normal step: 3/3 is no longer in combat → does NOT deal damage to attacker
        // Result: 3/1 survives, 3/3 survives (regenerated), attacker takes no damage
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(activePlayer, "First Strike Knight") // 3/1 first strike
        val blocker = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3
        driver.removeSummoningSickness(attacker)

        // Give blocker a regeneration shield
        driver.addRegenerationShield(blocker, opponent)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(attacker), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(blocker to listOf(attacker))).isSuccess shouldBe true

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Blocker should survive (regenerated)
        driver.findPermanent(opponent, "Centaur Courser") shouldNotBe null

        // Blocker should be tapped (regeneration taps it)
        driver.isTapped(blocker) shouldBe true

        // Blocker's damage should be removed
        driver.state.getEntity(blocker)?.get<DamageComponent>() shouldBe null

        // Attacker should survive — blocker was removed from combat and couldn't deal damage back
        driver.findPermanent(activePlayer, "First Strike Knight") shouldNotBe null

        // Blocker should no longer be in combat
        driver.state.getEntity(blocker)?.get<BlockingComponent>() shouldBe null
    }

    test("attacker with regeneration shield survives first strike blocker but does not deal normal combat damage") {
        // 3/3 attacks, 3/1 first strike blocks. Attacker has regen shield.
        // First strike step: 3/1 deals 3 to 3/3 (lethal) → regeneration replaces destruction
        //   → 3/3 is tapped, damage removed, removed from combat
        // Normal step: 3/3 is no longer in combat → does NOT deal damage to blocker
        // Result: 3/3 survives (regenerated), 3/1 survives (attacker removed from combat, no damage dealt)
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser") // 3/3
        val blocker = driver.putCreatureOnBattlefield(opponent, "First Strike Knight") // 3/1 first strike
        driver.removeSummoningSickness(attacker)

        // Give attacker a regeneration shield
        driver.addRegenerationShield(attacker, activePlayer)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(attacker), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(blocker to listOf(attacker))).isSuccess shouldBe true

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Attacker should survive (regenerated)
        driver.findPermanent(activePlayer, "Centaur Courser") shouldNotBe null

        // Attacker should be tapped (regeneration taps it)
        driver.isTapped(attacker) shouldBe true

        // First strike blocker should survive — attacker was removed from combat before normal damage
        driver.findPermanent(opponent, "First Strike Knight") shouldNotBe null

        // Attacker should no longer be in combat
        driver.state.getEntity(attacker)?.get<AttackingComponent>() shouldBe null
    }

    test("blocker regenerates from damage spell during combat — does not deal normal combat damage") {
        // 3/3 attacks, 2/2 blocks. During combat (after blockers declared), active player bolts the blocker.
        // 2/2 takes 3 damage (lethal) → regeneration: tapped, damage removed, removed from combat
        // Normal combat damage step: 2/2 is no longer blocking → does not deal damage to 3/3
        // 3/3 is still "blocked" → deals no damage to defending player (no trample)
        // Result: 3/3 survives undamaged, 2/2 survives (regenerated), no player damage
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser") // 3/3
        val blocker = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears") // 2/2
        driver.removeSummoningSickness(attacker)

        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")

        // Give blocker a regeneration shield
        driver.addRegenerationShield(blocker, opponent)

        // Manually drive through combat step by step
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(attacker), opponent).isSuccess shouldBe true
        // Manually pass priority to advance to declare blockers (don't use bothPass which auto-declares)
        driver.passPriority(activePlayer)
        driver.passPriority(opponent)
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        driver.declareBlockers(opponent, mapOf(blocker to listOf(attacker))).isSuccess shouldBe true

        // After blockers declared, non-active player (blocker) has priority. Pass to active player.
        driver.passPriority(opponent)

        // Now active player has priority. Cast Lightning Bolt on blocker.
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.castSpell(activePlayer, bolt, listOf(blocker)).isSuccess shouldBe true

        // Opponent passes, then resolve bolt
        driver.bothPass()

        // Blocker should have regenerated (still on battlefield, tapped)
        driver.findPermanent(opponent, "Grizzly Bears") shouldNotBe null
        driver.isTapped(blocker) shouldBe true

        // Now advance through combat damage to postcombat main
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Attacker should survive — blocker was removed from combat, can't deal damage
        driver.findPermanent(activePlayer, "Centaur Courser") shouldNotBe null

        // Blocker still alive (regenerated)
        driver.findPermanent(opponent, "Grizzly Bears") shouldNotBe null

        // Defending player should not have taken damage (attacker was blocked, no trample)
        driver.assertLifeTotal(opponent, 20)
    }

    test("blocked attacker whose blocker regenerated does not deal damage to player") {
        // 2/2 attacks, 3/3 blocks. 3/3 has regen shield.
        // Active player bolts the 3/3 blocker during combat → regeneration triggers
        // 3/3 removed from combat. 2/2 is still "blocked" with no remaining blockers.
        // Without trample, blocked attacker with no blockers deals no damage.
        // Result: no player damage dealt
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears") // 2/2
        val blocker = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3
        driver.removeSummoningSickness(attacker)

        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")

        // Give blocker a regeneration shield
        driver.addRegenerationShield(blocker, opponent)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(attacker), opponent).isSuccess shouldBe true
        // Manually pass priority to advance to declare blockers
        driver.passPriority(activePlayer)
        driver.passPriority(opponent)
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        driver.declareBlockers(opponent, mapOf(blocker to listOf(attacker))).isSuccess shouldBe true

        // After blockers declared, non-active player has priority. Pass to active player.
        driver.passPriority(opponent)

        // Now active player has priority. Cast Lightning Bolt on blocker.
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.castSpell(activePlayer, bolt, listOf(blocker)).isSuccess shouldBe true

        // Resolve bolt
        driver.bothPass()

        // Blocker regenerated
        driver.findPermanent(opponent, "Centaur Courser") shouldNotBe null

        // Advance through combat damage
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Defending player takes no damage — attacker was blocked and has no trample
        driver.assertLifeTotal(opponent, 20)

        // Both creatures should survive
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldNotBe null
        driver.findPermanent(opponent, "Centaur Courser") shouldNotBe null
    }

    test("blocked trample attacker whose blocker regenerated deals full damage to player") {
        // 5/5 trample attacks, 3/3 blocks. 3/3 has regen shield.
        // Active player bolts the 3/3 blocker during combat → regeneration triggers
        // 3/3 removed from combat. 5/5 is still "blocked" but has trample.
        // Per CR 702.19d, trample creature with no remaining blockers assigns all damage to player.
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(activePlayer, "Trample Beast") // 5/5 trample
        val blocker = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3
        driver.removeSummoningSickness(attacker)

        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")

        // Give blocker a regeneration shield
        driver.addRegenerationShield(blocker, opponent)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(attacker), opponent).isSuccess shouldBe true
        driver.passPriority(activePlayer)
        driver.passPriority(opponent)
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        driver.declareBlockers(opponent, mapOf(blocker to listOf(attacker))).isSuccess shouldBe true

        // After blockers declared, non-active player has priority. Pass to active player.
        driver.passPriority(opponent)

        // Now active player has priority. Cast Lightning Bolt on blocker to trigger regen.
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.castSpell(activePlayer, bolt, listOf(blocker)).isSuccess shouldBe true

        // Resolve bolt
        driver.bothPass()

        // Blocker regenerated (removed from combat)
        driver.findPermanent(opponent, "Centaur Courser") shouldNotBe null

        // Advance through combat damage
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Trample creature deals full 5 damage to defending player
        driver.assertLifeTotal(opponent, 15)

        // Both creatures survive
        driver.findPermanent(activePlayer, "Trample Beast") shouldNotBe null
        driver.findPermanent(opponent, "Centaur Courser") shouldNotBe null
    }

    test("other combat pairs still resolve normally when one blocker regenerates") {
        // Attacker A (2/2) and Attacker B (2/1 first strike) both attack.
        // Blocker (3/3 with regen) blocks Attacker B.
        // First strike step: B deals 2 to blocker (not lethal yet - wait, 2 < 3 so not lethal)
        // Let's use: Attacker B (3/1 first strike) blocked by Blocker (3/3 with regen)
        //   First strike: B deals 3 to Blocker (lethal) → Blocker regenerates
        //   Normal: Blocker removed from combat, can't damage B. B is blocked, no damage to player.
        // Meanwhile Attacker A (2/2) is unblocked → deals 2 to defending player
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attackerA = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears") // 2/2
        val attackerB = driver.putCreatureOnBattlefield(activePlayer, "First Strike Knight") // 3/1 first strike
        val blocker = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3
        driver.removeSummoningSickness(attackerA)
        driver.removeSummoningSickness(attackerB)

        // Give blocker a regeneration shield
        driver.addRegenerationShield(blocker, opponent)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(attackerA, attackerB), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        // Only block the first strike knight, leave Grizzly Bears unblocked
        driver.declareBlockers(opponent, mapOf(blocker to listOf(attackerB))).isSuccess shouldBe true

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Blocker regenerated and survived
        driver.findPermanent(opponent, "Centaur Courser") shouldNotBe null

        // First Strike Knight survived (blocker removed from combat, no damage dealt back)
        driver.findPermanent(activePlayer, "First Strike Knight") shouldNotBe null

        // Grizzly Bears survived (unblocked)
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldNotBe null

        // Opponent took 2 damage from unblocked Grizzly Bears only
        driver.assertLifeTotal(opponent, 18)
    }
})
