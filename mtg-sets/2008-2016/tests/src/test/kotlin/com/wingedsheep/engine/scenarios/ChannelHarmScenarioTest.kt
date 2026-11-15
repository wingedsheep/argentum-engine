package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.ChannelHarm
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Channel Harm (FRF) — {5}{W} Instant. "Prevent all damage that would be dealt to you and
 * permanents you control this turn by sources you don't control. If damage is prevented this way,
 * you may have Channel Harm deal that much damage to target creature."
 *
 * Pins the rulings: the target is chosen as the spell is cast, the payoff is optional and part of
 * the prevention (no stack), damage is prevented even when the target is gone, and Channel Harm's
 * own damage to a creature you control isn't prevented by its shield.
 */
class ChannelHarmScenarioTest : FunSpec({

    class Setup(val d: GameTestDriver, val me: EntityId, val opp: EntityId)

    fun resolveAll(d: GameTestDriver) {
        var guard = 0
        while (d.stackSize > 0 && d.pendingDecision == null && guard++ < 10) d.bothPass()
    }

    fun setUp(): Setup {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + ChannelHarm)
            initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        }
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        return Setup(d, me, d.getOpponent(me))
    }

    fun Setup.castChannelHarm(target: EntityId) {
        val harm = d.putCardInHand(me, "Channel Harm")
        d.giveMana(me, Color.WHITE, 6)
        d.castSpell(me, harm, listOf(target)).error shouldBe null
        resolveAll(d)
    }

    /** The opponent bolts [target] with the stack empty. */
    fun Setup.opponentBolts(target: EntityId) {
        val bolt = d.putCardInHand(opp, "Lightning Bolt")
        d.giveMana(opp, Color.RED, 1)
        d.passPriority(me)
        d.castSpell(opp, bolt, listOf(target)).error shouldBe null
        resolveAll(d)
    }

    fun Setup.damageOn(id: EntityId) = d.state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    test("damage an opponent's source would deal to you is prevented and may be dealt to the target creature") {
        val s = setUp()
        val force = s.d.putCreatureOnBattlefield(s.opp, "Force of Nature")
        s.castChannelHarm(force)

        s.opponentBolts(s.me)
        withClue("the payoff is part of the prevention: asked while the Bolt resolves, no trigger on the stack") {
            s.d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            s.d.stackSize shouldBe 1
        }
        s.d.submitYesNo(s.me, true).error shouldBe null

        s.d.getLifeTotal(s.me) shouldBe 20
        withClue("Channel Harm deals that much — 3 — to the creature targeted at cast") { s.damageOn(force) shouldBe 3 }
    }

    test("declining still prevents the damage and deals none") {
        val s = setUp()
        val force = s.d.putCreatureOnBattlefield(s.opp, "Force of Nature")
        val courser = s.d.putCreatureOnBattlefield(s.me, "Centaur Courser")
        s.castChannelHarm(force)

        s.opponentBolts(courser)
        s.d.submitYesNo(s.me, false).error shouldBe null

        withClue("permanents you control are covered too") { s.damageOn(courser) shouldBe 0 }
        s.damageOn(force) shouldBe 0
    }

    test("a source you control isn't covered") {
        val s = setUp()
        val force = s.d.putCreatureOnBattlefield(s.opp, "Force of Nature")
        val courser = s.d.putCreatureOnBattlefield(s.me, "Centaur Courser")
        s.castChannelHarm(force)

        val bolt = s.d.putCardInHand(s.me, "Lightning Bolt")
        s.d.giveMana(s.me, Color.RED, 1)
        s.d.castSpell(s.me, bolt, listOf(courser)).error shouldBe null
        resolveAll(s.d)

        s.d.pendingDecision shouldBe null
        s.d.getGraveyard(s.me).contains(courser) shouldBe true
    }

    test("targeting a creature you control: Channel Harm's own damage to it isn't prevented") {
        val s = setUp()
        val force = s.d.putCreatureOnBattlefield(s.me, "Force of Nature")
        s.castChannelHarm(force)

        s.opponentBolts(s.me)
        s.d.submitYesNo(s.me, true).error shouldBe null

        s.d.getLifeTotal(s.me) shouldBe 20
        s.damageOn(force) shouldBe 3
    }

    test("damage is still prevented when the target has left the battlefield since Channel Harm resolved") {
        val s = setUp()
        val force = s.d.putCreatureOnBattlefield(s.opp, "Force of Nature")
        s.castChannelHarm(force)
        s.d.moveToGraveyard(force)

        s.opponentBolts(s.me)
        withClue("the choice is still offered, but there is nothing left for it to hit") {
            s.d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        }
        s.d.submitYesNo(s.me, true).error shouldBe null

        s.d.getLifeTotal(s.me) shouldBe 20
        s.d.getGraveyard(s.opp).contains(force) shouldBe true
        s.d.pendingDecision shouldBe null
    }

    test("combat damage from two attackers: each prevented instance offers its own payoff, and combat goes on") {
        val s = setUp()
        val force = s.d.putCreatureOnBattlefield(s.opp, "Force of Nature")
        val courser = s.d.putCreatureOnBattlefield(s.opp, "Centaur Courser")
        val lions = s.d.putCreatureOnBattlefield(s.opp, "Savannah Lions")
        s.d.removeSummoningSickness(courser)
        s.d.removeSummoningSickness(lions)

        s.d.passPriorityUntil(Step.DECLARE_ATTACKERS, activePlayer = s.opp)
        s.d.declareAttackers(s.opp, listOf(courser, lions), s.me).error shouldBe null
        s.d.passPriority(s.opp)
        val harm = s.d.putCardInHand(s.me, "Channel Harm")
        s.d.giveMana(s.me, Color.WHITE, 6)
        s.d.castSpell(s.me, harm, listOf(force)).error shouldBe null
        resolveAll(s.d)

        s.d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        s.d.declareNoBlockers(s.me).error shouldBe null
        var prompts = 0
        var guard = 0
        while (s.d.pendingDecision !is YesNoDecision && guard++ < 10) s.d.bothPass()
        while (s.d.pendingDecision is YesNoDecision) {
            prompts++
            s.d.submitYesNo(s.me, true).error shouldBe null
        }

        withClue("one payoff per prevented attacker") { prompts shouldBe 2 }
        s.d.getLifeTotal(s.me) shouldBe 20
        withClue("3 + 1 prevented (a 3/3 and a 1/1), all of it dealt to the Force") { s.damageOn(force) shouldBe 4 }
        s.d.pendingDecision shouldBe null
    }
})
