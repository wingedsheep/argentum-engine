package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.ResilientRoadrunner
import com.wingedsheep.mtg.sets.definitions.otj.OutlawsOfThunderJunctionSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContainIgnoringCase

/**
 * Tests for Resilient Roadrunner's activated ability:
 *
 * "{3}: This creature can't be blocked this turn except by creatures with haste."
 *
 * Exercises the new floating, one-shot [GrantCantBeBlockedExceptBy] grant: it routes through the
 * same projected `cantBeBlockedExceptByFilters` channel the static ability uses, so the existing
 * block-evasion rule enforces it. Blocker "with haste" = Ragavan (HASTE keyword); blocker without
 * haste = Grizzly Bears.
 */
class ResilientRoadrunnerScenarioTest : FunSpec({

    val abilityId = ResilientRoadrunner.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + OutlawsOfThunderJunctionSet.cards)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20), startingLife = 20)
        return driver
    }

    /** Put Roadrunner attacking [opponent], having activated its ability this turn. */
    fun GameTestDriver.setupAttackWithAbilityActive(blockerName: String): Triple<EntityId, EntityId, EntityId> {
        val player = activePlayer!!
        val opponent = getOpponent(player)
        passPriorityUntil(Step.PRECOMBAT_MAIN)

        val roadrunner = putCreatureOnBattlefield(player, "Resilient Roadrunner")
        removeSummoningSickness(roadrunner)
        val blocker = putCreatureOnBattlefield(opponent, blockerName)
        removeSummoningSickness(blocker)

        // Activate "{3}: can't be blocked this turn except by creatures with haste."
        giveMana(player, Color.RED, 3)
        submit(ActivateAbility(playerId = player, sourceId = roadrunner, abilityId = abilityId))
            .isSuccess shouldBe true
        bothPass() // resolve the ability

        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player, listOf(roadrunner), opponent).isSuccess shouldBe true
        bothPass() // move to declare blockers
        currentStep shouldBe Step.DECLARE_BLOCKERS
        return Triple(roadrunner, blocker, opponent)
    }

    test("a creature without haste can't block the Roadrunner after the ability resolves") {
        val driver = createDriver()
        val (roadrunner, blocker, opponent) = driver.setupAttackWithAbilityActive("Grizzly Bears")

        val result = driver.submitExpectFailure(
            DeclareBlockers(opponent, mapOf(blocker to listOf(roadrunner)))
        )

        result.isSuccess shouldBe false
        result.error shouldContainIgnoringCase "haste"
        result.error shouldContainIgnoringCase "cannot block"
    }

    test("a creature with haste CAN block the Roadrunner after the ability resolves") {
        val driver = createDriver()
        val (roadrunner, blocker, opponent) =
            driver.setupAttackWithAbilityActive("Test Hasty Prospector")

        driver.declareBlockers(opponent, mapOf(blocker to listOf(roadrunner)))
            .isSuccess shouldBe true
    }

    test("without activating the ability, any creature can block (restriction is the floating grant)") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val roadrunner = driver.putCreatureOnBattlefield(player, "Resilient Roadrunner")
        driver.removeSummoningSickness(roadrunner)
        val blocker = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(roadrunner), opponent).isSuccess shouldBe true
        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        driver.declareBlockers(opponent, mapOf(blocker to listOf(roadrunner)))
            .isSuccess shouldBe true
    }
})
