package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.ConfusionInTheRanks
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Confusion in the Ranks (MRD #87) — "Whenever an artifact, creature, or enchantment enters, its
 * controller chooses target permanent another player controls that shares a card type with it.
 * Exchange control of those permanents."
 *
 * Three things here are easy to get quietly wrong, and each has a test:
 *  1. **Who chooses.** "Its controller" is the controller of the *entering permanent*, not of
 *     Confusion — `TargetChooser.ControllerOfTriggeringEntity`. Every test below therefore puts
 *     Confusion on the **opposite** side from the permanent that enters, because with both on one
 *     side the two readings produce the same deciding player and the test proves nothing.
 *  2. **"Another player"** is measured against the entering permanent's controller too, so the
 *     eligible set excludes *that* player's own permanents rather than "the opponents of whoever
 *     plays Confusion".
 *  3. **"Shares a card type."** Card types only — a creature that enters can't be swapped for a
 *     land, however many supertypes the two have in common.
 *
 * The permanents that must *trigger* Confusion are cast from hand rather than dropped onto the
 * battlefield: the driver's direct placement bypasses the real zone change, so no enters trigger
 * fires and the test would pass vacuously.
 */
class ConfusionInTheRanksScenarioTest : FunSpec({

    // Built once, during spec construction: `TestCards.all` forces a ClassGraph scan of the
    // whole card corpus, and paying that inside the first test body puts it under the per-test
    // timeout — which is what makes a single-spec run flake on a loaded machine.
    val cards = TestCards.all + ConfusionInTheRanks

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(cards)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Cast [cardName] from [player]'s hand and let it resolve onto the battlefield. */
    fun GameTestDriver.castPermanent(
        player: EntityId,
        cardName: String,
        color: Color = Color.GREEN,
        amount: Int = 2
    ) {
        val inHand = putCardInHand(player, cardName)
        giveMana(player, color, amount)
        castSpell(player, inHand).error shouldBe null
        // Resolve the spell; stop as soon as its enters trigger raises a decision.
        repeat(6) {
            if (state.pendingDecision != null) return
            bothPass()
        }
    }

    fun GameTestDriver.controllerOf(permanent: EntityId): EntityId? =
        state.projectedState.getController(permanent)

    test("the entering permanent's controller chooses, and gets the swap") {
        val d = driver()
        // Confusion belongs to player 2; player 1 (the active player) is the one who plays into it.
        d.putPermanentOnBattlefield(d.player2, "Confusion in the Ranks")
        val bait = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")

        d.castPermanent(d.player1, "Grizzly Bears")
        val entering = d.findPermanent(d.player1, "Grizzly Bears")
        entering shouldNotBe null

        val decision = d.state.pendingDecision
        decision shouldNotBe null
        withClue("\"its controller chooses\" — the player who cast the creature decides, not Confusion's controller") {
            decision!!.playerId shouldBe d.player1
        }
        withClue("\"another player controls\" is relative to the entering permanent's controller") {
            (decision as ChooseTargetsDecision).legalTargets.values.flatten() shouldBe listOf(bait)
        }

        d.submitTargetSelection(d.player1, listOf(bait)).error shouldBe null
        repeat(4) { if (d.state.pendingDecision == null) d.bothPass() }

        withClue("control of the two creatures was exchanged") {
            d.controllerOf(entering!!) shouldBe d.player2
            d.controllerOf(bait) shouldBe d.player1
        }
    }

    test("a permanent that shares no card type with the entering one is not a legal target") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, "Confusion in the Ranks")
        // Player 2's only other permanent is a land — no card type in common with a creature.
        val theirLand = d.putLandOnBattlefield(d.player2, "Forest")

        d.castPermanent(d.player1, "Grizzly Bears")

        // With no legal target the mandatory trigger is removed from the stack (CR 603.3d) rather
        // than resolving as a no-op, so no decision is ever raised.
        withClue("a land shares no card type with a creature, so nothing was asked and nothing swapped") {
            d.state.pendingDecision shouldBe null
            d.controllerOf(theirLand) shouldBe d.player2
            d.controllerOf(d.findPermanent(d.player1, "Grizzly Bears")!!) shouldBe d.player1
        }
    }

    test("Confusion triggers on its own arrival") {
        // It is an enchantment, so it meets its own trigger — the printed card, and the reason it
        // reads as a symmetrical mess rather than an engine. Here Confusion is the permanent that
        // enters, so its own controller is the one who chooses, and it must swap *itself* away.
        val d = driver()
        val theirEnchantment = d.putPermanentOnBattlefield(d.player2, "Sphere of Purity")

        d.castPermanent(d.player1, "Confusion in the Ranks", color = Color.RED, amount = 5)
        val confusion = d.findPermanent(d.player1, "Confusion in the Ranks")
        confusion shouldNotBe null

        val decision = d.state.pendingDecision
        decision shouldNotBe null
        withClue("Confusion is the permanent that entered, so its own controller chooses") {
            decision!!.playerId shouldBe d.player1
        }
        withClue("the only enchantment another player controls is the legal target") {
            (decision as ChooseTargetsDecision).legalTargets.values.flatten() shouldBe
                listOf(theirEnchantment)
        }

        d.submitTargetSelection(d.player1, listOf(theirEnchantment)).error shouldBe null
        repeat(4) { if (d.state.pendingDecision == null) d.bothPass() }

        withClue("Confusion swaps itself away") {
            d.controllerOf(confusion!!) shouldBe d.player2
            d.controllerOf(theirEnchantment) shouldBe d.player1
        }
    }
})
