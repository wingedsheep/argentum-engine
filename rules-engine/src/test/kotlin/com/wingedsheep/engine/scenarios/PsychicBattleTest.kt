package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.inv.cards.PsychicBattle
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Psychic Battle (INV #68) — Invasion engine gap #19: reveal-and-compare target swap.
 *
 * "Whenever a player chooses one or more targets, each player reveals the top card of their
 * library. The player who reveals the card with the greatest mana value may change the target or
 * targets. If two or more cards are tied for greatest, the target or targets remain unchanged."
 *
 * In each scenario "you" control Psychic Battle and a Savannah Lions; "opp" (the active player)
 * controls a Centaur Courser and casts Lightning Bolt (3 damage, any target) at your Lions. The
 * library tops decide whether "you" win the reveal and may redirect the Bolt.
 */
class PsychicBattleTest : FunSpec({

    /** Empty a player's library (CR ruling: a player with no card to reveal doesn't compare). */
    fun GameTestDriver.emptyLibrary(playerId: EntityId) {
        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        var newState = state
        for (cardId in state.getZone(libraryZone)) {
            newState = newState.removeFromZone(libraryZone, cardId).withoutEntity(cardId)
        }
        replaceState(newState)
    }

    fun setup(configureLibraries: GameTestDriver.(you: EntityId, opp: EntityId) -> Unit):
        Triple<GameTestDriver, List<EntityId>, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(PsychicBattle))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val opp = driver.activePlayer!!
        val you = driver.getOpponent(opp)

        driver.putPermanentOnBattlefield(you, "Psychic Battle")
        val courser = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        val lions = driver.putCreatureOnBattlefield(you, "Savannah Lions")

        driver.configureLibraries(you, opp)

        // Opp casts Lightning Bolt at your Lions. This emits the "chooses targets" trigger, so
        // Psychic Battle's ability goes on the stack above the Bolt.
        val bolt = driver.putCardInHand(opp, "Lightning Bolt")
        driver.giveMana(opp, Color.RED, 1)
        driver.castSpellWithTargets(opp, bolt, listOf(ChosenTarget.Permanent(lions))).isSuccess shouldBe true
        driver.state.stack.contains(bolt) shouldBe true

        return Triple(driver, listOf(you, opp, courser, lions), bolt)
    }

    test("reveal winner redirects the spell to a new legal target") {
        // You reveal Force of Nature (mana value 6); opp reveals Llanowar Elves (mana value 1).
        val (driver, ids, bolt) = setup { you, opp ->
            putCardOnTopOfLibrary(you, "Force of Nature")
            putCardOnTopOfLibrary(opp, "Llanowar Elves")
        }
        val (you, _, courser, lions) = ids

        // Resolve Psychic Battle's trigger: it reveals both tops and (you won) prompts you to
        // change the Bolt's target.
        driver.bothPass()
        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe you
        // Both creatures are legal targets for Lightning Bolt (any target), including the current.
        decision.options.contains(courser) shouldBe true
        decision.options.contains(lions) shouldBe true

        // You redirect the Bolt away from your Lions onto opp's Courser.
        driver.submitCardSelection(you, listOf(courser))
        driver.state.getEntity(bolt)!!.get<TargetsComponent>()!!.targets shouldBe
            listOf(ChosenTarget.Permanent(courser))

        // The Bolt now resolves on the Courser: it dies; the Lions is spared.
        driver.bothPass()
        driver.state.getBattlefield().contains(courser) shouldBe false
        driver.state.getBattlefield().contains(lions) shouldBe true
    }

    test("a tie for greatest mana value leaves the targets unchanged") {
        // Both players reveal a mana value 1 card — a tie, so no one may change the targets.
        val (driver, ids, bolt) = setup { you, opp ->
            putCardOnTopOfLibrary(you, "Llanowar Elves")
            putCardOnTopOfLibrary(opp, "Birds of Paradise")
        }
        val (_, _, courser, lions) = ids

        // Resolve the trigger — the tie means no retarget decision is offered.
        driver.bothPass()
        driver.pendingDecision.shouldBeNull()
        driver.state.getEntity(bolt)!!.get<TargetsComponent>()!!.targets shouldBe
            listOf(ChosenTarget.Permanent(lions))

        // The Bolt resolves on its original target: the Lions dies, the Courser survives.
        driver.bothPass()
        driver.state.getBattlefield().contains(lions) shouldBe false
        driver.state.getBattlefield().contains(courser) shouldBe true
    }

    test("a player with an empty library is skipped — the only revealer wins by default") {
        // Opp's library is empty, so only you reveal a card; you are the sole revealer and win
        // regardless of mana value (CR: the effect looks at the cards actually revealed).
        val (driver, ids, bolt) = setup { you, opp ->
            putCardOnTopOfLibrary(you, "Llanowar Elves") // mana value 1
            emptyLibrary(opp)
        }
        val (you, _, courser, lions) = ids

        // Resolve the trigger — you are the only revealer, so you may change the Bolt's target.
        driver.bothPass()
        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe you

        // Redirect the Bolt onto opp's Courser.
        driver.submitCardSelection(you, listOf(courser))
        driver.bothPass()
        driver.state.getBattlefield().contains(courser) shouldBe false
        driver.state.getBattlefield().contains(lions) shouldBe true
    }
})
