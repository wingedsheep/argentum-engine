package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Battlefield Thaumaturge (JOU #31): "Each instant and sorcery spell you cast costs {1} less to
 * cast for each creature it targets. Heroic — Whenever you cast a spell that targets this
 * creature, this creature gains hexproof until end of turn."
 */
class BattlefieldThaumaturgeScenarioTest : ScenarioTestBase() {

    private fun TestGame.strike(targets: List<ChosenTarget>) = execute(
        CastSpell(player1Id, state.getHand(player1Id).first { cardName(it) == "Lightning Strike" }, targets)
    )

    private fun TestGame.cardName(id: com.wingedsheep.sdk.model.EntityId) =
        state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name

    init {
        test("an instant aimed at a creature costs {1} less") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Battlefield Thaumaturge")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Lightning Strike")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // {1}{R} less {1}: one Mountain pays it.
            game.strike(listOf(ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!))).error shouldBe null
            game.resolveStack()
            game.findPermanent("Grizzly Bears") shouldBe null
        }

        test("aimed at a player, it costs full price") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Battlefield Thaumaturge")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Lightning Strike")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.strike(listOf(ChosenTarget.Player(game.player2Id))).error shouldNotBe null
        }

        test("heroic: targeting it with a spell gives it hexproof until end of turn, and the spell is discounted") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Battlefield Thaumaturge")
                .withCardInHand(1, "Titanic Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val thaumaturge = game.findPermanent("Battlefield Thaumaturge")!!

            // Titanic Growth's {1}{G} less {1} is {G}: one Forest.
            game.castSpell(1, "Titanic Growth", thaumaturge).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.hasKeyword(thaumaturge, Keyword.HEXPROOF) shouldBe true
            projected.getPower(thaumaturge) shouldBe 6
        }
    }
}
