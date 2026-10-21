package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.actsInStepOf
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TurnPart
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.DrawDuringOtherDrawSteps
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.SkipStepOrPhase
import com.wingedsheep.sdk.scripting.UntapDuringOtherUntapSteps
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * "Untap all permanents you control during each opponent's untap step" and "You draw a card during
 * each opponent's draw step" — [UntapDuringOtherUntapSteps] / [DrawDuringOtherDrawSteps] scoped to
 * opponents, as Teferi, Who Slows the Sunset's emblem has them. Per its ruling, the active player
 * draws first and the emblem's controller draws next, with no priority between the two draws; a
 * skipped draw step (CR 103.8a's first-turn skip included) has no draws to follow.
 */
class DuringOpponentsStepsStaticsTest : FunSpec({

    val emblemSpell = card("Opponent Steps Emblem") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.CreatePermanentEmblem(
                ownedStaticAbilities = listOf(
                    UntapDuringOtherUntapSteps(Player.EachOpponent),
                    DrawDuringOtherDrawSteps(Player.EachOpponent)
                ),
                emblemDescription = "Untap all permanents you control during each opponent's untap step. " +
                    "You draw a card during each opponent's draw step."
            )
        }
    }
    val drawingArtifact = card("Opponent Step Draw Artifact") {
        typeLine = "Artifact"
        staticAbility { ability = DrawDuringOtherDrawSteps(Player.EachOpponent) }
    }
    val skipDraw = card("Skip Own Draw Artifact") {
        typeLine = "Artifact"
        staticAbility { ability = SkipStepOrPhase(TurnPart.DRAW_STEP, Player.You) }
    }
    val miracleCard = card("Opponent Step Miracle") {
        manaCost = "{3}{U}"
        typeLine = "Sorcery"
        keywordAbility(KeywordAbility.miracle("{U}"))
        spell { effect = Effects.DrawCards(1) }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(emblemSpell, drawingArtifact, skipDraw, miracleCard))
        initMirrorMatch(Deck.of("Island" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.getEmblem(player: EntityId) {
        castSpell(player, putCardInHand(player, emblemSpell.name)).error shouldBe null
        bothPass()
    }

    test("the controller draws right after the opponent's draw-step draw, before anyone has priority") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.getEmblem(me)
        d.passPriorityUntil(Step.UPKEEP, opp)
        val myHand = d.getHandSize(me); val oppHand = d.getHandSize(opp)

        val result = d.services.turnManager.drawPhaseManager.performDrawStep(d.state)
        result.error shouldBe null
        result.events.filterIsInstance<CardsDrawnEvent>().map { it.playerId } shouldContainExactly listOf(opp, me)
        result.newState.priorityPlayerId shouldBe opp

        d.passPriorityUntil(Step.DRAW, opp)
        d.getHandSize(opp) shouldBe oppHand + 1
        d.getHandSize(me) shouldBe myHand + 1
        d.assertPriority(opp)
    }

    test("the controller's own draw step draws only the turn-based card") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.getEmblem(me)
        d.passPriorityUntil(Step.UPKEEP, me)
        val myHand = d.getHandSize(me); val oppHand = d.getHandSize(opp)
        d.passPriorityUntil(Step.DRAW, me)
        d.getHandSize(me) shouldBe myHand + 1
        d.getHandSize(opp) shouldBe oppHand
    }

    test("each instance draws a card: two emblems and a printed static draw three") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.getEmblem(me); d.getEmblem(me)
        d.putPermanentOnBattlefield(me, drawingArtifact.name)
        d.passPriorityUntil(Step.UPKEEP, opp)
        val myHand = d.getHandSize(me)
        d.passPriorityUntil(Step.DRAW, opp)
        d.getHandSize(me) shouldBe myHand + 3
    }

    test("an opponent who skips their draw step gives the static no draw step to act in") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.getEmblem(me)
        d.putPermanentOnBattlefield(opp, skipDraw.name)
        d.passPriorityUntil(Step.UPKEEP, opp)
        val myHand = d.getHandSize(me); val oppHand = d.getHandSize(opp)
        d.passPriorityUntil(Step.DRAW, opp)
        d.getHandSize(opp) shouldBe oppHand
        d.getHandSize(me) shouldBe myHand
    }

    test("an opponent's first-turn draw skip leaves nothing to follow") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + listOf(drawingArtifact))
            initMirrorMatch(Deck.of("Island" to 40))
        }
        val first = d.activePlayer!!; val second = d.getOpponent(first)
        d.putPermanentOnBattlefield(second, drawingArtifact.name)
        val secondHand = d.getHandSize(second)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.getHandSize(second) shouldBe secondHand
    }

    test("a miracle revealed on the emblem draw still hands priority back to the active player") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.getEmblem(me)
        d.passPriorityUntil(Step.UPKEEP, opp)
        d.putCardOnTopOfLibrary(me, miracleCard.name)
        d.passPriority(opp); d.passPriority(me)

        val question = d.pendingDecision as YesNoDecision
        question.playerId shouldBe me
        d.submitYesNo(me, false).error shouldBe null
        d.state.step shouldBe Step.DRAW
        d.assertPriority(opp)
    }

    test("the controller's permanents untap during the opponent's untap step") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.getEmblem(me)
        val land = d.putLandOnBattlefield(me, "Island")
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.tapPermanent(land); d.tapPermanent(bears)
        d.passPriorityUntil(Step.UPKEEP, opp)
        d.isTapped(land) shouldBe false
        d.isTapped(bears) shouldBe false
    }

    test("opponent-scoped statics skip a teammate's own turn; other-player-scoped ones don't") {
        val d = GameTestDriver().apply { registerCards(TestCards.all) }
        val (p1, p2, p3, _) = d.initMultiplayer(
            List(4) { Deck.of("Island" to 40) },
            format = Format.TeamVsTeam(),
            teams = listOf(listOf(0, 2), listOf(1, 3)),
        )
        actsInStepOf(d.state, Player.EachOpponent, controller = p3, activePlayer = p1) shouldBe false
        actsInStepOf(d.state, Player.Each, controller = p3, activePlayer = p1) shouldBe true
        actsInStepOf(d.state, Player.EachOpponent, controller = p2, activePlayer = p1) shouldBe true
        actsInStepOf(d.state, Player.EachOpponent, controller = p1, activePlayer = p1) shouldBe false
    }
})
