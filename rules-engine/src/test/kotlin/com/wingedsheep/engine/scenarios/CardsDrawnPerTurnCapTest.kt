package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.state.components.player.AttemptedDrawFromEmptyLibraryComponent
import com.wingedsheep.engine.state.components.player.CardsDrawnThisTurnComponent
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.RestrictCardsDrawnPerTurn
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [RestrictCardsDrawnPerTurn] — "can't draw more than N cards each turn" (CR 121.2b) and its
 * N = 0 form "can't draw cards". Pins the rulings of Narset, Parter of Veils: individual draws are
 * capped, earlier draws this turn count, a disallowed draw can't be replaced, a replaced draw
 * doesn't use up the allowance, and an optional "up to" draw can't be chosen past the cap.
 */
class CardsDrawnPerTurnCapTest : FunSpec({
    val opponentsCap = card("Opponents Draw Cap") { typeLine = "Artifact"
        staticAbility { ability = RestrictCardsDrawnPerTurn(Player.EachOpponent, maxPerTurn = 1) } }
    val selfCap = card("Self Draw Cap") { typeLine = "Artifact"
        staticAbility { ability = RestrictCardsDrawnPerTurn(Player.You, maxPerTurn = 1) } }
    val selfCapTwo = card("Self Draw Cap Two") { typeLine = "Artifact"
        staticAbility { ability = RestrictCardsDrawnPerTurn(Player.You, maxPerTurn = 2) } }
    val drawLock = card("Draw Lock") { typeLine = "Artifact"
        staticAbility { ability = RestrictCardsDrawnPerTurn(Player.Each, maxPerTurn = 0) } }
    val untappedCap = card("Untapped Draw Cap") { typeLine = "Artifact"
        staticAbility { condition = Conditions.SourceIsUntapped
            ability = RestrictCardsDrawnPerTurn(Player.You, maxPerTurn = 1) } }
    val everyoneDrawsThree = card("Everyone Draws Three") { manaCost = "{0}"; typeLine = "Sorcery"
        spell { effect = Effects.DrawCards(3, EffectTarget.PlayerRef(Player.Each)) } }
    val drawThree = card("Draw Three Now") { manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.DrawCards(3) } }
    val drawUpToThree = card("Draw Up To Three") { manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.DrawUpTo(3) } }
    val lifeForDraw = card("Life For Next Draw") { manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.ReplaceNextDraw(Effects.GainLife(3)) } }
    val mute = card("Mute Draw Cap") { manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.RemoveAllAbilities(t) } }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(opponentsCap, selfCap, selfCapTwo, drawLock, untappedCap,
            everyoneDrawsThree, drawThree, drawUpToThree, lifeForDraw, mute))
        initMirrorMatch(Deck.of("Forest" to 40)); passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.drawn(p: EntityId) = state.getEntity(p)?.get<CardsDrawnThisTurnComponent>()?.count ?: 0
    fun GameTestDriver.cast(p: EntityId, name: String, targets: List<EntityId> = emptyList()) {
        castSpell(p, putCardInHand(p, name), targets).error shouldBe null
        bothPass()
    }

    test("an instruction to draw several is carried out up to the cap; the controller is unaffected") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, opponentsCap.name)
        val (myHand, oppHand) = d.getHandSize(me) to d.getHandSize(opp)
        d.cast(me, everyoneDrawsThree.name)
        d.getHandSize(me) shouldBe myHand + 3
        d.getHandSize(opp) shouldBe oppHand + 1
        d.drawn(opp) shouldBe 1
    }

    test("cards drawn earlier in the turn, before the cap existed, count against it") {
        val d = driver(); val me = d.activePlayer!!
        d.cast(me, drawThree.name)
        val hand = d.getHandSize(me)
        d.putPermanentOnBattlefield(me, selfCap.name)
        d.cast(me, drawThree.name)
        d.getHandSize(me) shouldBe hand
    }

    test("the smallest applicable cap wins") {
        val d = driver(); val me = d.activePlayer!!
        val hand0 = d.getHandSize(me)
        d.putPermanentOnBattlefield(me, selfCapTwo.name)
        d.putPermanentOnBattlefield(me, selfCap.name)
        d.cast(me, drawThree.name)
        d.getHandSize(me) shouldBe hand0 + 1
    }

    test("the allowance resets each turn and the draw step uses it") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, opponentsCap.name)
        val oppHand = d.getHandSize(opp)
        d.passPriorityUntil(Step.UPKEEP); d.passPriorityUntil(Step.PRECOMBAT_MAIN) // their draw step draws one
        d.activePlayer shouldBe opp
        d.getHandSize(opp) shouldBe oppHand + 1
        d.drawn(opp) shouldBe 1
    }

    test("a player who can't draw doesn't attempt a draw from an empty library") {
        val d = driver(); val me = d.activePlayer!!
        d.replaceState(d.state.copy(zones = d.state.zones + (ZoneKey(me, Zone.LIBRARY) to emptyList())))
        d.putPermanentOnBattlefield(me, drawLock.name)
        d.cast(me, drawThree.name)
        d.state.getEntity(me)!!.has<AttemptedDrawFromEmptyLibraryComponent>() shouldBe false
    }

    test("can't draw cards also stops the draw step") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, drawLock.name)
        val oppHand = d.getHandSize(opp)
        d.passPriorityUntil(Step.UPKEEP); d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe opp
        d.getHandSize(opp) shouldBe oppHand
        d.drawn(opp) shouldBe 0
    }

    test("a disallowed draw can't be replaced, so a draw shield waits") {
        val d = driver(); val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, selfCap.name)
        d.cast(me, drawThree.name) // uses the one allowed draw
        d.cast(me, lifeForDraw.name)
        val (life, hand) = d.getLifeTotal(me) to d.getHandSize(me)
        d.cast(me, drawThree.name)
        d.getLifeTotal(me) shouldBe life
        d.getHandSize(me) shouldBe hand
    }

    test("a replaced draw didn't happen, so the next draw is still allowed") {
        val d = driver(); val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, selfCap.name)
        d.cast(me, lifeForDraw.name)
        val (life, hand) = d.getLifeTotal(me) to d.getHandSize(me)
        d.cast(me, drawThree.name)
        d.getLifeTotal(me) shouldBe life + 3
        d.getHandSize(me) shouldBe hand + 1
    }

    test("draw up to N offers no more than the remaining allowance") {
        val d = driver(); val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, selfCap.name)
        d.castSpell(me, d.putCardInHand(me, drawUpToThree.name)).error shouldBe null
        d.bothPass()
        val decision = d.pendingDecision as ChooseNumberDecision
        decision.maxValue shouldBe 1
        val hand = d.getHandSize(me)
        d.submitDecision(me, NumberChosenResponse(decision.id, 1)).error shouldBe null
        d.getHandSize(me) shouldBe hand + 1
    }

    test("a conditional cap applies only while its condition holds") {
        val d = driver(); val me = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(me, untappedCap.name)
        d.tapPermanent(source)
        val hand = d.getHandSize(me)
        d.cast(me, drawThree.name)
        d.getHandSize(me) shouldBe hand + 3
        d.untapPermanent(source)
        d.cast(me, drawThree.name)
        d.getHandSize(me) shouldBe hand + 3
    }

    test("a source that lost its abilities caps nothing") {
        val d = driver(); val me = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(me, selfCap.name)
        d.cast(me, mute.name, listOf(source))
        val hand = d.getHandSize(me)
        d.cast(me, drawThree.name)
        d.getHandSize(me) shouldBe hand + 3
    }
})
