package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.thb.cards.AlseidOfLifesBounty
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Alseid of Life's Bounty (THB #1) — {W} Enchantment Creature — Nymph, 1/1.
 *
 * "Lifelink. {1}, Sacrifice this creature: Target creature or enchantment you control gains
 *  protection from the color of your choice until end of turn."
 */
class AlseidOfLifesBountyScenarioTest : FunSpec({

    val abilityId = AlseidOfLifesBounty.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AlseidOfLifesBounty))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("sacrifice: target creature you control gains protection from the chosen color") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val alseid = driver.putCreatureOnBattlefield(me, "Alseid of Life's Bounty")
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        driver.giveColorlessMana(me, 1)
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = alseid,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(bears)),
            )
        ).outcome shouldBe Outcome.Done

        // Sacrificed as a cost.
        driver.findPermanent(me, "Alseid of Life's Bounty") shouldBe null
        driver.getGraveyardCardNames(me).contains("Alseid of Life's Bounty") shouldBe true

        driver.bothPass()
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseColorDecision>()
        driver.submitDecision(me, ColorChosenResponse(decision.id, Color.RED))

        driver.state.projectedState.hasKeyword(bears, "PROTECTION_FROM_RED") shouldBe true
        driver.state.projectedState.hasKeyword(bears, "PROTECTION_FROM_BLACK") shouldBe false
    }

    test("can't target a creature an opponent controls") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val alseid = driver.putCreatureOnBattlefield(me, "Alseid of Life's Bounty")
        val theirBears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.giveColorlessMana(me, 1)
        driver.submitExpectFailure(
            ActivateAbility(
                playerId = me,
                sourceId = alseid,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(theirBears)),
            )
        )
        driver.findPermanent(me, "Alseid of Life's Bounty") shouldBe alseid
    }

    test("can target a noncreature enchantment you control") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val alseid = driver.putCreatureOnBattlefield(me, "Alseid of Life's Bounty")
        val enchantment = driver.putPermanentOnBattlefield(me, "Test Enchantment")

        driver.giveColorlessMana(me, 1)
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = alseid,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(enchantment)),
            )
        ).outcome shouldBe Outcome.Done

        driver.bothPass()
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseColorDecision>()
        driver.submitDecision(me, ColorChosenResponse(decision.id, Color.BLUE))

        driver.state.projectedState.hasKeyword(enchantment, "PROTECTION_FROM_BLUE") shouldBe true
    }
})
