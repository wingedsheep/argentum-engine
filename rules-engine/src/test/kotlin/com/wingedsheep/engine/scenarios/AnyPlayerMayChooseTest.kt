package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The free-choice form of `AnyPlayerMayPayEffect` (null cost, `Effects.AnyPlayerMayChoose`) and its
 * `payerAs` binding: each eligible player in turn order answers a yes/no, the first yes runs the
 * consequence with that player bound as `Player.BoundVariable`, nobody after them is asked, and if
 * everyone declines the otherwise branch runs (Sin Prodder's "any opponent may have you … If a
 * player does, this deals damage to that player … Otherwise, …").
 */
class AnyPlayerMayChooseTest : FunSpec({

    /** "When this enters, any opponent may have it deal 3 damage to them. Otherwise, you gain 2 life." */
    val Offer = card("Test Opponent Offer") {
        manaCost = "{0}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.AnyPlayerMayChoose(
                prompt = "Take 3 damage?",
                consequence = Effects.DealDamage(
                    amount = 3,
                    target = EffectTarget.PlayerRef(Player.BoundVariable("taker")),
                    damageSource = EffectTarget.Self,
                ),
                otherwise = Effects.GainLife(2),
                eligiblePlayers = Player.EachOpponent,
                chooserAs = "taker",
            )
        }
    }

    /** "When this enters, any player may pay 2 life. If a player does, that player draws a card." */
    val PaidOffer = card("Test Paid Offer") {
        manaCost = "{0}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = com.wingedsheep.sdk.scripting.effects.AnyPlayerMayPayEffect(
                cost = Costs.pay.PayLife(2),
                consequence = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.BoundVariable("payer"))),
                payerAs = "payer",
            )
        }
    }

    fun driver(players: Int): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCards(listOf(Offer, PaidOffer))
        d.initMultiplayer(List(players) { Deck.of("Forest" to 30) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d to d.state.turnOrder
    }

    /** Cast [name] for the active player and pass until the trigger asks something (or the stack empties). */
    fun GameTestDriver.castAndResolve(playerId: EntityId, name: String) {
        castSpell(playerId, putCardInHand(playerId, name)).error shouldBe null
        repeat(12) {
            if (state.pendingDecision == null && state.stack.isNotEmpty()) passPriority(state.priorityPlayerId!!)
        }
    }

    test("the opponent who accepts is the one bound as 'that player'; the controller is never asked") {
        val (d, order) = driver(2)
        val (me, opp) = order
        d.castAndResolve(me, "Test Opponent Offer")

        val ask = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        ask.playerId shouldBe opp
        ask.prompt shouldBe "Take 3 damage?"
        d.submitYesNo(opp, true)

        d.pendingDecision shouldBe null
        d.getLifeTotal(opp) shouldBe 17
        d.getLifeTotal(me) shouldBe 20
    }

    test("when every opponent declines, the otherwise branch runs and nobody is dealt damage") {
        val (d, order) = driver(2)
        val (me, opp) = order
        d.castAndResolve(me, "Test Opponent Offer")

        d.submitYesNo(opp, false)

        d.pendingDecision shouldBe null
        d.getLifeTotal(opp) shouldBe 20
        d.getLifeTotal(me) shouldBe 22
    }

    test("opponents are asked in turn order; a decline passes the offer on and the later acceptor is bound") {
        val (d, order) = driver(3)
        val (me, second, third) = order
        d.castAndResolve(me, "Test Opponent Offer")

        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe second
        d.submitYesNo(second, false)
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe third
        d.submitYesNo(third, true)

        d.pendingDecision shouldBe null
        d.getLifeTotal(second) shouldBe 20
        d.getLifeTotal(third) shouldBe 17
        d.getLifeTotal(me) shouldBe 20
    }

    test("the first yes ends the offer: later opponents are not asked") {
        val (d, order) = driver(3)
        val (me, second, third) = order
        d.castAndResolve(me, "Test Opponent Offer")

        d.submitYesNo(second, true)

        d.pendingDecision shouldBe null
        d.getLifeTotal(second) shouldBe 17
        d.getLifeTotal(third) shouldBe 20
        d.getLifeTotal(me) shouldBe 20
    }

    test("payerAs binds the paying player on a paid cost too") {
        val (d, order) = driver(2)
        val (me, opp) = order
        val oppHand = d.getHandSize(opp)
        val myHand = d.getHandSize(me)
        d.castAndResolve(me, "Test Paid Offer")

        // APNAP: the controller is asked first and declines; the opponent pays.
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe me
        d.submitYesNo(me, false)
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe opp
        d.submitYesNo(opp, true)

        d.getLifeTotal(opp) shouldBe 18
        d.getHandSize(opp) shouldBe oppHand + 1
        d.getHandSize(me) shouldBe myHand // the artifact put in hand was cast; no draw for the decliner
    }
})
