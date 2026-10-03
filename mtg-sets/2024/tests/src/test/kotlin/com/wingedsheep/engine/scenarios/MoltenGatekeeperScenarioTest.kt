package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.MoltenGatekeeper
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Molten Gatekeeper (MH3 #128) — {2}{R} 2/3 Artifact Creature — Golem.
 *
 * "Whenever another creature you control enters, this creature deals 1 damage to each opponent.
 *  Unearth {R}"
 */
class MoltenGatekeeperScenarioTest : FunSpec({

    val unearthId = MoltenGatekeeper.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + MoltenGatekeeper)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("another creature entering under your control deals 1 damage to each opponent") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Molten Gatekeeper")

        val bears = driver.putCardInHand(me, "Grizzly Bears")
        driver.giveMana(me, Color.GREEN, 2)
        driver.castSpell(me, bears).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve Bears
        driver.bothPass() // resolve trigger

        driver.getLifeTotal(opp) shouldBe 19
        driver.getLifeTotal(me) shouldBe 20
    }

    test("unearth returns it with haste and exiles it at the next end step") {
        val driver = newDriver()
        val me = driver.player1
        val gatekeeper = driver.putCardInGraveyard(me, "Molten Gatekeeper")
        driver.giveMana(me, Color.RED, 1)

        driver.submit(ActivateAbility(playerId = me, sourceId = gatekeeper, abilityId = unearthId))
            .outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getPermanents(me) shouldContain gatekeeper
        driver.state.projectedState.hasKeyword(gatekeeper, Keyword.HASTE) shouldBe true

        driver.passPriorityUntil(Step.END)
        driver.bothPass() // resolve the delayed exile trigger

        driver.getPermanents(me) shouldNotContain gatekeeper
        driver.getExile(me) shouldContain gatekeeper
        driver.getGraveyard(me) shouldNotContain gatekeeper
    }

    test("an unearthed Gatekeeper that would die is exiled instead") {
        val driver = newDriver()
        val me = driver.player1
        val gatekeeper = driver.putCardInGraveyard(me, "Molten Gatekeeper")
        driver.giveMana(me, Color.RED, 1)
        driver.submit(ActivateAbility(playerId = me, sourceId = gatekeeper, abilityId = unearthId))
            .outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.getPermanents(me) shouldContain gatekeeper

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(gatekeeper)).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getPermanents(me) shouldNotContain gatekeeper
        driver.getGraveyardCardNames(me) shouldNotContain "Molten Gatekeeper"
        driver.getExile(me) shouldContain gatekeeper
    }

    test("unearth can't be activated at instant speed") {
        val driver = newDriver()
        val me = driver.player1
        val gatekeeper = driver.putCardInGraveyard(me, "Molten Gatekeeper")
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 2)
        driver.castSpell(me, bolt, listOf(driver.getOpponent(me))).outcome shouldBe Outcome.Done

        // Stack is non-empty: sorcery-speed activation is illegal.
        driver.submitExpectFailure(ActivateAbility(playerId = me, sourceId = gatekeeper, abilityId = unearthId))
        driver.getGraveyard(me) shouldContain gatekeeper
    }
})
