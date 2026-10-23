package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.OchreJelly
import com.wingedsheep.mtg.sets.definitions.ktk.cards.HardenedScales
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Ochre Jelly (AFR #196) — {X}{G} 0/0 Creature — Ooze.
 *
 * "Trample. This creature enters with X +1/+1 counters on it.
 *  Split — When this creature dies, if it had two or more +1/+1 counters on it, create a token
 *  that's a copy of it at the beginning of the next end step. The token enters with half that many
 *  +1/+1 counters on it, rounded down."
 *
 * Covers the delayed token copy entering with half the last-known count, a dying Jelly *token*
 * splitting again (its copiable values are frozen when the delayed trigger is scheduled, since the
 * token ceases to exist), the two-counter intervening "if", and the entry counters counting as
 * counters put on the token (Hardened Scales).
 */
class OchreJellyScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(OchreJelly)
        driver.registerCard(HardenedScales)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun plusOnes(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun jellies(driver: GameTestDriver, player: EntityId): List<EntityId> =
        driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD)).filter {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Ochre Jelly"
        }

    fun castJelly(driver: GameTestDriver, player: EntityId, x: Int): EntityId {
        val jelly = driver.putCardInHand(player, "Ochre Jelly")
        driver.giveMana(player, Color.GREEN, x + 1)
        driver.castXSpell(player, jelly, x).outcome shouldBe Outcome.Done
        driver.bothPass()
        return jelly
    }

    /** Destroy [victim] and let its dies trigger resolve, scheduling the split. */
    fun destroy(driver: GameTestDriver, player: EntityId, victim: EntityId) {
        val blade = driver.putCardInHand(player, "Doom Blade")
        driver.giveMana(player, Color.BLACK, 2)
        driver.castSpell(player, blade, listOf(victim)).outcome shouldBe Outcome.Done
        driver.bothPass() // Doom Blade resolves; the dies trigger goes on the stack
        driver.bothPass() // the dies trigger resolves
    }

    fun resolveEndStepSplit(driver: GameTestDriver) {
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
    }

    test("enters with X counters; dies with five and splits into a token with two at the next end step") {
        val driver = newDriver()
        val player = driver.player1

        val jelly = castJelly(driver, player, 5)
        plusOnes(driver, jelly) shouldBe 5

        destroy(driver, player, jelly)
        jellies(driver, player).shouldBeEmpty() // the copy is delayed, not immediate

        resolveEndStepSplit(driver)

        val tokens = jellies(driver, player)
        tokens shouldHaveSize 1
        val token = tokens.single()
        driver.state.getEntity(token)?.has<TokenComponent>() shouldBe true
        plusOnes(driver, token) shouldBe 2
        driver.state.projectedState.getPower(token) shouldBe 2
        driver.state.projectedState.getToughness(token) shouldBe 2
        driver.state.projectedState.hasKeyword(token, Keyword.TRAMPLE) shouldBe true
    }

    test("a dying Jelly token splits again, with half of its own counters") {
        val driver = newDriver()
        val player = driver.player1

        val jelly = castJelly(driver, player, 5)
        destroy(driver, player, jelly)
        resolveEndStepSplit(driver)
        val firstToken = jellies(driver, player).single()
        plusOnes(driver, firstToken) shouldBe 2

        // Next turn: the token (two counters) dies and has ceased to exist by the end step.
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.state.activePlayerId!!
        destroy(driver, active, firstToken)

        resolveEndStepSplit(driver)

        val secondToken = jellies(driver, player).single()
        (secondToken == firstToken) shouldBe false
        plusOnes(driver, secondToken) shouldBe 1
    }

    test("a Jelly with fewer than two counters doesn't split") {
        val driver = newDriver()
        val player = driver.player1

        val jelly = castJelly(driver, player, 1)
        destroy(driver, player, jelly)
        resolveEndStepSplit(driver)

        jellies(driver, player).shouldBeEmpty()
    }

    test("the token's entry counters are counters put on it, so Hardened Scales adds one") {
        val driver = newDriver()
        val player = driver.player1

        val jelly = castJelly(driver, player, 4)
        driver.putPermanentOnBattlefield(player, "Hardened Scales")
        destroy(driver, player, jelly)
        resolveEndStepSplit(driver)

        // Half of four is two, plus one from Hardened Scales.
        plusOnes(driver, jellies(driver, player).single()) shouldBe 3
    }
})
