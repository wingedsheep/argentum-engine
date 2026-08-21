package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.LoxodonPeacekeeper
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Loxodon Peacekeeper (MRD #13) — "At the beginning of your upkeep, the player with the lowest life
 * total gains control of this creature. If two or more players are tied for lowest life total, you
 * choose one of them, and that player gains control of this creature."
 *
 * Ghazbán Ogre's mirror, and the card that turned "most" and "ties do nothing" from hardcoded
 * behaviour into two axes on the shared rank effect. The tie case is not an edge case here: two
 * players start on the same life total, so the very first upkeep is a tie, and a tie-break that
 * silently did nothing would look correct in a race and be wrong in every opening.
 *
 * Setup runs on player 2's turn so that player 1's upkeep — where the trigger lives — is one turn
 * transition away.
 */
class LoxodonPeacekeeperScenarioTest : FunSpec({

    /** Player 2's precombat main, with the Peacekeeper already on player 1's battlefield. */
    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + LoxodonPeacekeeper)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /**
     * Index of the tie-break option that hands the creature to [id]. Each option names the player
     * it favours, so the label carries the player's name — a bare list of names would not tell the
     * player which way the choice runs.
     */
    fun ChooseOptionDecision.optionFor(d: GameTestDriver, id: EntityId): Int {
        val name = d.state.getEntity(id)?.get<PlayerComponent>()?.name ?: error("player has no name")
        return options.indexOfFirst { it.startsWith(name) }
    }

    /**
     * The *projected* controller. A control change is a Layer 2 floating effect and never touches
     * the base `ControllerComponent`, so `GameTestDriver.getController` — which reads that
     * component directly — would report the original controller no matter what happened.
     */
    fun GameTestDriver.controllerOf(id: EntityId): EntityId? =
        state.projectedState.getController(id)

    fun resolveStack(d: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && d.state.stack.isNotEmpty() && !d.isPaused) d.bothPass()
    }

    /** Put the Peacekeeper down, set both life totals, then run into player 1's upkeep. */
    fun GameTestDriver.setUp(player1Life: Int, player2Life: Int): EntityId {
        val peacekeeper = putCreatureOnBattlefield(player1, "Loxodon Peacekeeper")
        setLifeTotal(player1, player1Life)
        setLifeTotal(player2, player2Life)
        passPriorityUntil(Step.UPKEEP)
        activePlayer shouldBe player1
        resolveStack(this)
        return peacekeeper
    }

    test("the player with the strictly lowest life total takes it, with no choice offered") {
        val d = driver()
        val peacekeeper = d.setUp(player1Life = 20, player2Life = 12)

        withClue("an unambiguous rank asks nobody anything") {
            d.isPaused shouldBe false
        }
        d.controllerOf(peacekeeper) shouldBe d.player2
    }

    test("the controller keeps it when they are the one lowest on life") {
        // The same ability, the other way round: the direction is read off the life totals, not
        // off who controls the creature.
        val d = driver()
        val peacekeeper = d.setUp(player1Life = 5, player2Life = 20)

        d.isPaused shouldBe false
        d.controllerOf(peacekeeper) shouldBe d.player1
    }

    test("a tie asks the controller, who may keep it") {
        // Both players on 20 — the opening position of every game, and the case a NONE tie-break
        // would silently no-op.
        val d = driver()
        val peacekeeper = d.setUp(player1Life = 20, player2Life = 20)

        val decision = d.pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        withClue("both tied players are offered, and only they are") {
            decision.options.size shouldBe 2
            decision.playerId shouldBe d.player1
        }

        val keepIndex = decision.optionFor(d, d.player1)
        d.submitDecision(d.player1, OptionChosenResponse(decision.id, keepIndex))
        resolveStack(d)

        d.controllerOf(peacekeeper) shouldBe d.player1
    }

    test("a tie can also be resolved in the opponent's favour") {
        val d = driver()
        val peacekeeper = d.setUp(player1Life = 20, player2Life = 20)

        val decision = d.pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val giveIndex = decision.optionFor(d, d.player2)
        d.submitDecision(d.player1, OptionChosenResponse(decision.id, giveIndex))
        resolveStack(d)

        withClue("the chosen tied player gains control, not merely the ability's controller") {
            d.controllerOf(peacekeeper) shouldBe d.player2
        }
    }
})
