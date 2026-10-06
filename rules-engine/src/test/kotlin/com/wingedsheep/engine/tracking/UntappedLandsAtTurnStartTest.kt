package com.wingedsheep.engine.tracking

import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.player.UntappedLandsAtTurnStartComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine coverage for `TurnTracker.UNTAPPED_LANDS_AT_TURN_START` (Power Surge). The snapshot is
 * taken in the untap step before anything phases in or untaps, for every player, and counts only
 * lands that player controls.
 */
class UntappedLandsAtTurnStartTest : FunSpec({

    fun GameTestDriver.snapshot(player: EntityId) =
        state.getEntity(player)?.get<UntappedLandsAtTurnStartComponent>()?.count

    test("counts each player's untapped lands before the untap step untaps or phases anything in") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mine = List(4) { d.putLandOnBattlefield(d.player1, "Forest") }
        d.tapPermanent(mine[0])
        d.addComponent(mine[1], PhasedOutComponent(d.player1))
        d.putPermanentOnBattlefield(d.player1, "Artifact Creature")
        repeat(3) { d.putLandOnBattlefield(d.player2, "Forest") }

        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe d.player1
        withClue("the tapped land untapped and the phased-out one phased in since the snapshot") {
            d.isTapped(mine[0]) shouldBe false
            d.state.getEntity(mine[1])?.has<PhasedOutComponent>() shouldBe false
        }
        withClue("two untapped lands; tapped, phased-out and nonland permanents don't count") {
            d.snapshot(d.player1) shouldBe 2
        }
        withClue("the non-active player is recorded too") {
            d.snapshot(d.player2) shouldBe 3
        }
    }
})
