package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.clb.cards.BattleAngelsOfTyr
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Battle Angels of Tyr (CLB #9) — {2}{W}{W} 4/4 Flying, myriad.
 *
 *   Whenever this creature deals combat damage to a player, draw a card if that player has more
 *   cards in hand than each other player. Then you create a Treasure token if that player controls
 *   more lands than each other player. Then you gain 3 life if that player has more life than each
 *   other player.
 *
 * Three-player games: p[0] attacks p[1]; myriad's copy toward p[2] is declined so only the
 * original connects. Each check is independent, is made at resolution (after the 4 damage), and
 * counts every other player — the Angels' controller included.
 */
class BattleAngelsOfTyrScenarioTest : FunSpec({

    fun threePlayer(): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + BattleAngelsOfTyr + PredefinedTokens.allTokens)
        val players = d.initMultiplayer(List(3) { Deck.of("Plains" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe players[0]
        return d to players
    }

    fun GameTestDriver.setHandSize(player: EntityId, n: Int) {
        while (getHandSize(player) > n) moveToGraveyard(getHand(player).first())
        while (getHandSize(player) < n) putCardInHand(player, "Plains")
    }

    fun GameTestDriver.treasures(player: EntityId): Int =
        getPermanents(player).count { state.getEntity(it)?.get<CardComponent>()?.name == "Treasure" }

    /** Attack p[1] with the Angels, decline myriad, and run to the postcombat main phase. */
    fun GameTestDriver.attackWithAngels(p: List<EntityId>, angels: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(p[0], listOf(angels), p[1]).error shouldBe null
        var guard = 0
        while (guard++ < 100 && state.step != Step.POSTCOMBAT_MAIN) {
            when (val decision = pendingDecision) {
                is YesNoDecision -> submitYesNo(decision.playerId, false)
                null -> bothPass()
                else -> error("unexpected decision $decision")
            }
        }
    }

    test("the damaged player leads every count: draw, Treasure and 3 life") {
        val (d, p) = threePlayer()
        val angels = d.putCreatureOnBattlefield(p[0], "Battle Angels of Tyr").also { d.removeSummoningSickness(it) }
        d.setHandSize(p[0], 3)
        d.setHandSize(p[1], 6)
        d.setHandSize(p[2], 4)
        repeat(2) { d.putLandOnBattlefield(p[1], "Plains") }
        d.putLandOnBattlefield(p[2], "Plains")
        d.setLifeTotal(p[1], 30)

        d.attackWithAngels(p, angels)

        d.getLifeTotal(p[1]) shouldBe 26
        d.getHandSize(p[0]) shouldBe 4
        d.treasures(p[0]) shouldBe 1
        d.getLifeTotal(p[0]) shouldBe 23
    }

    test("ties fail, and the Angels' controller counts as one of the other players") {
        val (d, p) = threePlayer()
        val angels = d.putCreatureOnBattlefield(p[0], "Battle Angels of Tyr").also { d.removeSummoningSickness(it) }
        d.setHandSize(p[0], 2)
        d.setHandSize(p[1], 5)
        d.setHandSize(p[2], 5)
        // You control more lands than the damaged player.
        repeat(3) { d.putLandOnBattlefield(p[0], "Plains") }
        repeat(2) { d.putLandOnBattlefield(p[1], "Plains") }
        // After the 4 damage the damaged player is at 20, tied with the third player.
        d.setLifeTotal(p[1], 24)

        d.attackWithAngels(p, angels)

        d.getLifeTotal(p[1]) shouldBe 20
        withClue("hand size tied with the third player") { d.getHandSize(p[0]) shouldBe 2 }
        withClue("the controller has more lands") { d.treasures(p[0]) shouldBe 0 }
        withClue("life tied with the third player after damage") { d.getLifeTotal(p[0]) shouldBe 20 }
    }

    test("each check stands alone: only the land count leads, so only the Treasure") {
        val (d, p) = threePlayer()
        val angels = d.putCreatureOnBattlefield(p[0], "Battle Angels of Tyr").also { d.removeSummoningSickness(it) }
        d.setHandSize(p[0], 4)
        d.setHandSize(p[1], 1)
        d.setHandSize(p[2], 4)
        d.putLandOnBattlefield(p[1], "Plains")

        d.attackWithAngels(p, angels)

        d.getLifeTotal(p[1]) shouldBe 16
        d.getHandSize(p[0]) shouldBe 4
        d.treasures(p[0]) shouldBe 1
        d.getLifeTotal(p[0]) shouldBe 20
    }
})
