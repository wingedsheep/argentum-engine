package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Lord of the Void (GTC #71) — {4}{B}{B}{B} Creature — Demon 7/7.
 *
 * "Flying. Whenever this creature deals combat damage to a player, exile the top seven cards of
 * that player's library, then put a creature card from among them onto the battlefield under your
 * control."
 *
 * The cards come from the *damaged* player's library and stay in that player's exile; the chosen
 * creature enters under the attacker's control (the 2013-01-24 ruling makes the pick mandatory).
 */
class LordOfTheVoidScenarioTest : ScenarioTestBase() {

    private fun ScenarioBuilder.withLibrary(player: Int, vararg names: String): ScenarioBuilder {
        var b = this
        names.forEach { b = b.withCardInLibrary(player, it) }
        return b
    }

    private fun TestGame.hitWithLord() {
        declareAttackers(mapOf("Lord of the Void" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        if (state.pendingDecision == null) resolveStack()
    }

    init {
        test("exiles the damaged player's top seven and steals a creature card from among them") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Lord of the Void", summoningSickness = false)
                .withCardInLibrary(1, "Swamp")
                .withLibrary(2, "Hill Giant", "Grizzly Bears", "Mountain", "Mountain", "Mountain", "Mountain", "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.hitWithLord()
            game.getLifeTotal(2) shouldBe 20 - 7

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.playerId shouldBe game.player1Id
            val giant = decision.options.first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant"
            }
            game.selectCards(listOf(giant))
            game.resolveStack()

            withClue("the chosen creature card is on the battlefield under Alice's control") {
                game.state.getBattlefield().contains(giant) shouldBe true
                game.state.projectedState.getController(giant) shouldBe game.player1Id
            }
            withClue("the other six stay in Bob's exile") {
                game.librarySize(2) shouldBe 0
                game.state.getZone(ZoneKey(game.player2Id, Zone.EXILE)).size shouldBe 6
                game.isInExile(2, "Grizzly Bears") shouldBe true
            }
        }

        test("with no creature card among the seven, all seven stay exiled") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Lord of the Void", summoningSickness = false)
                .withCardInLibrary(1, "Swamp")
                .withLibrary(2, "Mountain", "Mountain", "Mountain", "Mountain", "Mountain", "Mountain", "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.hitWithLord()
            if (game.state.stack.isNotEmpty()) game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.librarySize(2) shouldBe 0
            game.state.getZone(ZoneKey(game.player2Id, Zone.EXILE)).size shouldBe 7
            game.findAllPermanents("Mountain").size shouldBe 0
        }
    }
}
