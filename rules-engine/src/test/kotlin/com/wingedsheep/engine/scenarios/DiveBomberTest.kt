package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.util.UUID

/**
 * Tests for Dive Bomber.
 *
 * Dive Bomber: {3}{W}
 * Creature — Bird Soldier
 * 2/2
 * Flying
 * {T}, Sacrifice Dive Bomber: It deals 2 damage to target attacking or blocking creature.
 */
class DiveBomberTest : FunSpec({

    val diveBomberAbilityId = AbilityId(UUID.randomUUID().toString())

    val DiveBomber = CardDefinition.creature(
        name = "Dive Bomber",
        manaCost = ManaCost.parse("{3}{W}"),
        subtypes = setOf(Subtype("Bird"), Subtype("Soldier")),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying\n{T}, Sacrifice Dive Bomber: It deals 2 damage to target attacking or blocking creature.",
        script = CardScript.permanent(
            ActivatedAbility(
                id = diveBomberAbilityId,
                cost = AbilityCost.Composite(listOf(AbilityCost.Tap, AbilityCost.SacrificeSelf)),
                effect = DealDamageEffect(2, EffectTarget.ContextTarget(0)),
                targetRequirement = TargetPermanent(filter = TargetFilter.AttackingOrBlockingCreature)
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DiveBomber))
        return driver
    }

    test("deals 2 damage to blocking creature and kills it") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Active player has an attacker and Dive Bomber; opponent has a blocker
        val ourAttacker = driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser") // 3/3
        driver.removeSummoningSickness(ourAttacker)

        val diveBomber = driver.putCreatureOnBattlefield(activePlayer, "Dive Bomber")
        driver.removeSummoningSickness(diveBomber)

        val blocker = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears") // 2/2
        driver.removeSummoningSickness(blocker)

        // Attack with Centaur Courser
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(ourAttacker), opponent)
        driver.bothPass()

        // Opponent blocks with Grizzly Bears
        driver.declareBlockers(opponent, mapOf(blocker to listOf(ourAttacker)))

        // After declaring blockers, defending player (opponent) still has priority — pass it
        driver.passPriority(opponent)

        // Now active player has priority — activate Dive Bomber on the blocker
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = diveBomber,
                abilityId = diveBomberAbilityId,
                targets = listOf(ChosenTarget.Permanent(blocker))
            )
        )
        result.isSuccess shouldBe true

        // Let the ability resolve
        driver.bothPass()

        // Blocker (2/2) took 2 damage = lethal, should be dead
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Grizzly Bears"

        // Dive Bomber was sacrificed
        driver.findPermanent(activePlayer, "Dive Bomber") shouldBe null
        driver.getGraveyardCardNames(activePlayer) shouldContain "Dive Bomber"
    }

    test("deals 2 damage to attacking creature — defender activates") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Active player has an attacker; opponent (defender) has Dive Bomber
        val attacker = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears") // 2/2
        driver.removeSummoningSickness(attacker)

        val diveBomber = driver.putCreatureOnBattlefield(opponent, "Dive Bomber")
        driver.removeSummoningSickness(diveBomber)

        // Attack with Grizzly Bears
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(attacker), opponent)
        driver.bothPass()

        // No blockers
        driver.declareNoBlockers(opponent)

        // Active player passes priority, then opponent can activate Dive Bomber
        driver.passPriority(activePlayer)

        val result = driver.submit(
            ActivateAbility(
                playerId = opponent,
                sourceId = diveBomber,
                abilityId = diveBomberAbilityId,
                targets = listOf(ChosenTarget.Permanent(attacker))
            )
        )
        result.isSuccess shouldBe true

        // Let the ability resolve
        driver.bothPass()

        // Attacker (2/2) took 2 damage = lethal, should be dead
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null
        driver.getGraveyardCardNames(activePlayer) shouldContain "Grizzly Bears"

        // Dive Bomber was sacrificed
        driver.findPermanent(opponent, "Dive Bomber") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Dive Bomber"
    }
})
