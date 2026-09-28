package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.EleshNorn
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Elesh Norn // The Argent Etchings (MOM #12).
 *
 * The front's damage punisher is a `Recipient.You` trigger plus a "permanent you control" trigger on
 * one permanent — the shape the damage-observer bucket routing exists for. Each damaged recipient
 * must punish exactly once (2023-04-14 rulings).
 */
class EleshNornScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + PredefinedTokens.allTokens + listOf(EleshNorn))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun nameOf(driver: GameTestDriver, id: EntityId): String =
        driver.state.getEntity(id)!!.get<CardComponent>()!!.name

    /** Resolve the stack, answering the punisher's "pay {1}?" prompt with [pay]. */
    fun resolveAll(driver: GameTestDriver, pay: Boolean) {
        var guard = 0
        while (guard++ < 30 && (driver.state.stack.isNotEmpty() || driver.isPaused)) {
            when (val d = driver.pendingDecision) {
                is YesNoDecision -> driver.submitYesNo(d.playerId, pay)
                is SelectManaSourcesDecision ->
                    driver.submitDecision(d.playerId, ManaSourcesSelectedResponse(d.id, d.autoPaySuggestion))
                null -> driver.bothPass()
                else -> driver.autoResolveDecision()
            }
        }
    }

    fun bolt(driver: GameTestDriver, caster: EntityId, target: EntityId) {
        val card = driver.putCardInHand(caster, "Lightning Bolt")
        driver.giveMana(caster, Color.RED, 1)
        driver.castSpell(caster, card, listOf(target)).error shouldBe null
    }

    test("damage to you makes the source's controller lose 2 life unless they pay {1}, once") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val elesh = driver.getOpponent(caster)
        driver.putCreatureOnBattlefield(elesh, "Elesh Norn")

        val before = driver.getLifeTotal(caster)
        bolt(driver, caster, elesh)
        resolveAll(driver, pay = false)

        driver.getLifeTotal(elesh) shouldBe 20 - 3
        driver.getLifeTotal(caster) shouldBe before - 2
    }

    test("paying {1} avoids the life loss") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val elesh = driver.getOpponent(caster)
        driver.putCreatureOnBattlefield(elesh, "Elesh Norn")
        driver.putPermanentOnBattlefield(caster, "Forest")

        val before = driver.getLifeTotal(caster)
        bolt(driver, caster, elesh)
        resolveAll(driver, pay = true)

        driver.getLifeTotal(caster) shouldBe before
    }

    test("damage to a permanent you control punishes once, and the creature's controller is the payer") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val elesh = driver.getOpponent(caster)
        driver.putCreatureOnBattlefield(elesh, "Elesh Norn")
        val bear = driver.putCreatureOnBattlefield(elesh, "Grizzly Bears")

        val before = driver.getLifeTotal(caster)
        bolt(driver, caster, bear)
        resolveAll(driver, pay = false)

        driver.getLifeTotal(caster) shouldBe before - 2
        driver.getLifeTotal(elesh) shouldBe 20
    }

    test("damage to your own permanents or to the opponent's does not trigger it") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val elesh = driver.getOpponent(caster)
        driver.putCreatureOnBattlefield(elesh, "Elesh Norn")
        val theirs = driver.putCreatureOnBattlefield(caster, "Grizzly Bears")

        val before = driver.getLifeTotal(caster)
        bolt(driver, caster, theirs) // damage to the caster's own creature: not Elesh's controller's
        resolveAll(driver, pay = false)
        driver.getLifeTotal(caster) shouldBe before
    }

    fun transform(driver: GameTestDriver, you: EntityId): EntityId {
        val elesh = driver.putCreatureOnBattlefield(you, "Elesh Norn")
        val fodder = List(3) { driver.putCreatureOnBattlefield(you, "Grizzly Bears") }
        driver.giveMana(you, Color.WHITE, 1)
        driver.giveColorlessMana(you, 2)
        driver.submit(
            ActivateAbility(playerId = you, sourceId = elesh, abilityId = EleshNorn.activatedAbilities.first().id)
        ).error shouldBe null
        if (driver.isPaused) driver.submitCardSelection(you, fodder)
        resolveAll(driver, pay = false)
        return elesh
    }

    test("transforming needs three other creatures; chapter I makes five Phyrexian 2/2s") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val saga = transform(driver, you)

        nameOf(driver, saga) shouldBe "The Argent Etchings"
        driver.state.getEntity(saga)!!.get<CountersComponent>()!!.getCount(CounterType.LORE) shouldBe 1
        driver.state.getBattlefield(you).count { nameOf(driver, it) == "Grizzly Bears" } shouldBe 0
        driver.state.getBattlefield(you).count { nameOf(driver, it) == "Phyrexian" } shouldBe 5
    }

    fun advanceToNextTurnMain(driver: GameTestDriver) {
        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
        resolveAll(driver, pay = false)
    }

    test("chapter II pumps and grants double strike; chapter III wipes non-artifact, non-land, non-Phyrexian permanents") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        val saga = transform(driver, you)

        advanceToNextTurnMain(driver) // opponent's turn
        val oppBear = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val oppForest = driver.putPermanentOnBattlefield(opp, "Forest")
        advanceToNextTurnMain(driver) // your turn: lore 2
        driver.state.getEntity(saga)!!.get<CountersComponent>()!!.getCount(CounterType.LORE) shouldBe 2
        val token = driver.state.getBattlefield(you).first { nameOf(driver, it) == "Phyrexian" }
        driver.state.projectedState.getPower(token) shouldBe 3
        driver.state.projectedState.hasKeyword(token, Keyword.DOUBLE_STRIKE) shouldBe true

        val yourBear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        advanceToNextTurnMain(driver) // opponent's turn
        advanceToNextTurnMain(driver) // your turn: lore 3

        nameOf(driver, saga) shouldBe "Elesh Norn"
        driver.state.getBattlefield().contains(oppBear) shouldBe false
        driver.state.getBattlefield().contains(yourBear) shouldBe false
        driver.state.getBattlefield().contains(oppForest) shouldBe true
        driver.state.getBattlefield(you).count { nameOf(driver, it) == "Phyrexian" } shouldBe 5
    }
})
