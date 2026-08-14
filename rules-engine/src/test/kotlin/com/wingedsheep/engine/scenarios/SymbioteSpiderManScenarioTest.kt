package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.SymbioteSpiderMan
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Symbiote Spider-Man — {2}{U/B} Legendary Creature — Symbiote Spider Hero (2/4).
 *
 * "Whenever this creature deals combat damage to a player, look at that many cards from the top of
 *  your library. Put one of them into your hand and the rest into your graveyard.
 *  Find New Host — {2}{U/B}, Exile this card from your graveyard: Put a +1/+1 counter on target
 *  creature you control. It gains this card's other abilities. Activate only as a sorcery."
 */
class SymbioteSpiderManScenarioTest : FunSpec({

    val projector = StateProjector()
    val findNewHost = SymbioteSpiderMan.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(SymbioteSpiderMan)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("combat damage to a player digs N=damage and keeps one, rest to graveyard") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // Two known cards on top so the look/keep among 2 is meaningful (N = 2 power).
        val keep = driver.putCardOnTopOfLibrary(me, "Centaur Courser")
        val discard = driver.putCardOnTopOfLibrary(me, "Savannah Lions")

        val attacker = driver.putCreatureOnBattlefield(me, "Symbiote Spider-Man") // 2/4
        driver.removeSummoningSickness(attacker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(attacker), defendingPlayer = opp).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(opp).error shouldBe null
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        if (driver.pendingDecision != null && driver.pendingDecision !is SelectCardsDecision) {
            driver.confirmCombatDamage()
        }

        // The combat-damage trigger digs 2 (that many) and pauses to keep one.
        var safety = 0
        while (!driver.isPaused && safety++ < 30) driver.bothPass()
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()

        val decision = driver.pendingDecision as SelectCardsDecision
        driver.submitDecision(
            me,
            CardsSelectedResponse(decisionId = decision.id, selectedCards = listOf(keep))
        )
        safety = 0
        while (driver.isPaused && safety++ < 10) driver.bothPass()

        // One kept card went to hand, the rest to the graveyard.
        driver.getHand(me).contains(keep) shouldBe true
        driver.getGraveyard(me).contains(discard) shouldBe true
    }

    test("Find New Host exiles from graveyard, adds a +1/+1 counter, and grants the dig trigger") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val symbiote = driver.putCardInGraveyard(me, "Symbiote Spider-Man")
        val host = driver.putCreatureOnBattlefield(me, "Centaur Courser") // 3/3
        driver.removeSummoningSickness(host)

        projector.getProjectedPower(driver.state, host) shouldBe 3

        // {2}{U/B}: two generic + one blue for the hybrid pip.
        driver.giveColorlessMana(me, 2)
        driver.giveMana(me, Color.BLUE, 1)
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = symbiote,
                abilityId = findNewHost,
                targets = listOf(ChosenTarget.Permanent(host))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // Symbiote exiled from the graveyard; host got a +1/+1 counter (now 4/4).
        driver.getExile(me).contains(symbiote) shouldBe true
        driver.getGraveyard(me).contains(symbiote) shouldBe false
        val counters = driver.state.getEntity(host)?.get<CountersComponent>()?.counters ?: emptyMap()
        counters[CounterType.PLUS_ONE_PLUS_ONE] shouldBe 1
        projector.getProjectedPower(driver.state, host) shouldBe 4

        // The host gained Symbiote's combat-damage dig ability: swing and it fires.
        driver.putCardOnTopOfLibrary(me, "Centaur Courser")
        driver.putCardOnTopOfLibrary(me, "Savannah Lions")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(host), defendingPlayer = opp).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(opp).error shouldBe null
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        if (driver.pendingDecision != null && driver.pendingDecision !is SelectCardsDecision) {
            driver.confirmCombatDamage()
        }

        var safety = 0
        while (!driver.isPaused && safety++ < 30) driver.bothPass()
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
    }
})
