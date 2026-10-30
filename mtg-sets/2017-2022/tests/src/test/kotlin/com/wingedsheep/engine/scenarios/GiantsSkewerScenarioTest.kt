package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.GiantsSkewer
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Giant's Skewer (ELD #91) — {1}{B} Artifact — Equipment.
 *
 * "Equipped creature gets +2/+1. Whenever equipped creature deals combat damage to a creature,
 *  create a Food token. Equip {3}"
 */
class GiantsSkewerScenarioTest : FunSpec({

    fun GameTestDriver.equippedBears(): EntityId {
        val me = player1
        val bears = putCreatureOnBattlefield(me, "Grizzly Bears").also(::removeSummoningSickness)
        val skewer = putPermanentOnBattlefield(me, "Giant's Skewer")
        giveColorlessMana(me, 3)
        submit(
            ActivateAbility(
                playerId = me,
                sourceId = skewer,
                abilityId = GiantsSkewer.activatedAbilities.single { it.isEquipAbility }.id,
                targets = listOf(ChosenTarget.Permanent(bears)),
            )
        ).outcome shouldBe Outcome.Done
        while (state.stack.isNotEmpty()) bothPass()
        return bears
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GiantsSkewer) + PredefinedTokens.allTokens)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("equipped creature gets +2/+1 and makes a Food when it deals combat damage to a blocker") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.getOpponent(me)
        val blocker = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")
        val bears = driver.equippedBears()
        driver.state.projectedState.getPower(bears) shouldBe 4
        driver.state.projectedState.getToughness(bears) shouldBe 3

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, mapOf(bears to opponent)).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(blocker to listOf(bears))).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.END_COMBAT)

        driver.findPermanent(me, "Food").shouldNotBeNull()
    }

    test("combat damage to a player doesn't create a Food") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.getOpponent(me)
        val bears = driver.equippedBears()

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, mapOf(bears to opponent)).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, emptyMap()).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.END_COMBAT)

        driver.getLifeTotal(opponent) shouldBe 16
        driver.findPermanent(me, "Food") shouldBe null
    }
})
