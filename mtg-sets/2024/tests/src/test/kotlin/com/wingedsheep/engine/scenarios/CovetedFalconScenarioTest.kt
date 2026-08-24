package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.handlers.effects.FaceDownTurnUp
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.CovetedFalcon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Coveted Falcon — {1}{U}{U} Artifact Creature — Bird 1/4.
 *
 * Two control-change abilities pointing in opposite directions. The one that needs proving is the
 * turned-face-up trigger: "target opponent gains control of any number of target permanents you
 * control. Draw a card for each one **they gained control of this way**" — the draw count is
 * control changes that actually happened, not targets that were chosen.
 */
class CovetedFalconScenarioTest : FunSpec({

    val allCards = TestCards.all + listOf(CovetedFalcon)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        return driver
    }

    /** Put [cardName] onto [playerId]'s battlefield face down under disguise, as a real cast would. */
    fun GameTestDriver.disguise(playerId: EntityId, cardName: String): EntityId {
        val id = putCreatureOnBattlefield(playerId, cardName)
        val cardDef = cardRegistry.requireCard(cardName)
        replaceState(
            state.updateEntity(id) { container ->
                var c = container.with(FaceDownComponent)
                    .with(FaceDownModeComponent(FaceDownMode.DISGUISE))
                FaceDownTurnUp.dataFor(cardDef, cardName, FaceDownMode.DISGUISE)?.let { c = c.with(it) }
                c
            }
        )
        removeSummoningSickness(id)
        return id
    }

    /**
     * Re-own [permanent] to [newOwner] while leaving it on its current controller's battlefield —
     * the "you own but don't control" board state the attack trigger looks for. Ownership is the
     * card's immutable owner, so this is the only way to build it without a second control-change
     * effect in the fixture.
     */
    fun GameTestDriver.reown(permanent: EntityId, newOwner: EntityId) {
        replaceState(
            state.updateEntity(permanent) { container ->
                val card = container.get<CardComponent>()!!
                container.with(OwnerComponent(newOwner)).with(card.copy(ownerId = newOwner))
            }
        )
    }

    context("turned face up — gifting permanents") {

        test("the opponent gains control of both chosen permanents and you draw two cards") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Island" to 40))
            val me = driver.activePlayer!!
            val opp = driver.getOpponent(me)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val falcon = driver.disguise(me, "Coveted Falcon")
            val gift1 = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
            val gift2 = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

            val handBefore = driver.getHand(me).size

            driver.giveMana(me, Color.BLUE, 2) // disguise {1}{U}
            driver.submit(
                TurnFaceUp(playerId = me, sourceId = falcon, paymentStrategy = PaymentStrategy.FromPool)
            ).error shouldBe null

            // The trigger goes on the stack and asks for its two target requirements at once.
            (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe true
            driver.submitMultiTargetSelection(
                me,
                mapOf(0 to listOf(opp), 1 to listOf(gift1, gift2)),
            ).isSuccess shouldBe true

            var guard = 0
            while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

            driver.state.projectedState.getController(gift1) shouldBe opp
            driver.state.projectedState.getController(gift2) shouldBe opp
            driver.getHand(me).size shouldBe handBefore + 2
        }

        test("choosing zero permanents is legal and draws nothing") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Island" to 40))
            val me = driver.activePlayer!!
            val opp = driver.getOpponent(me)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val falcon = driver.disguise(me, "Coveted Falcon")
            val keeper = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

            val handBefore = driver.getHand(me).size

            driver.giveMana(me, Color.BLUE, 2)
            driver.submit(
                TurnFaceUp(playerId = me, sourceId = falcon, paymentStrategy = PaymentStrategy.FromPool)
            ).error shouldBe null

            // "any number" has a minimum of zero (`unlimited = true`), so an empty list is a legal
            // choice for the permanent requirement even though the opponent target is mandatory.
            driver.submitMultiTargetSelection(
                me,
                mapOf(0 to listOf(opp), 1 to emptyList()),
            ).isSuccess shouldBe true

            var guard = 0
            while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

            driver.state.projectedState.getController(keeper) shouldBe me
            driver.getHand(me).size shouldBe handBefore
        }

        test("a chosen permanent that left the battlefield draws no card") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Island" to 40))
            val me = driver.activePlayer!!
            val opp = driver.getOpponent(me)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val falcon = driver.disguise(me, "Coveted Falcon")
            val survivor = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
            val doomed = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

            val handBefore = driver.getHand(me).size

            driver.giveMana(me, Color.BLUE, 2)
            driver.submit(
                TurnFaceUp(playerId = me, sourceId = falcon, paymentStrategy = PaymentStrategy.FromPool)
            ).error shouldBe null

            driver.submitMultiTargetSelection(
                me,
                mapOf(0 to listOf(opp), 1 to listOf(survivor, doomed)),
            ).isSuccess shouldBe true

            // With the trigger on the stack and its targets locked in (CR 603.3d), one of them dies.
            driver.moveToGraveyard(doomed)

            var guard = 0
            while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

            driver.state.projectedState.getController(survivor) shouldBe opp
            // One control change actually happened, so exactly one card is drawn.
            driver.getHand(me).size shouldBe handBefore + 1
        }
    }

    context("attacks — taking a permanent back") {

        test("gains control of a permanent you own but don't control") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Island" to 40))
            val me = driver.activePlayer!!
            val opp = driver.getOpponent(me)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val falcon = driver.putCreatureOnBattlefield(me, "Coveted Falcon")
            driver.removeSummoningSickness(falcon)

            // A creature on the opponent's battlefield that I own — e.g. one the Falcon gave away.
            val mine = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
            driver.reown(mine, me)

            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
            driver.declareAttackers(me, listOf(falcon), opp)

            var guard = 0
            while (driver.state.pendingDecision !is ChooseTargetsDecision && guard++ < 20) {
                driver.bothPass()
            }
            (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe true
            driver.submitTargetSelection(me, listOf(mine)).isSuccess shouldBe true

            guard = 0
            while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

            driver.state.projectedState.getController(mine) shouldBe me
        }

        test("a permanent the opponent both owns and controls is not a legal target") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Island" to 40))
            val me = driver.activePlayer!!
            val opp = driver.getOpponent(me)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val falcon = driver.putCreatureOnBattlefield(me, "Coveted Falcon")
            driver.removeSummoningSickness(falcon)

            // Owned *and* controlled by the opponent — fails the `OwnedByYou` half of the predicate.
            val theirs = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")

            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
            driver.declareAttackers(me, listOf(falcon), opp)

            var guard = 0
            while (driver.state.pendingDecision !is ChooseTargetsDecision && guard++ < 20) {
                driver.bothPass()
            }
            val decision = driver.state.pendingDecision
            if (decision is ChooseTargetsDecision) {
                decision.legalTargets.values.flatten().contains(theirs) shouldBe false
            }
        }
    }
})
