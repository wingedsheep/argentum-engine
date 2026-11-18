package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Launch the Fleet (JOU #15) — {W} Sorcery.
 *
 * "Strive — This spell costs {1} more to cast for each target beyond the first. Until end of turn,
 *  any number of target creatures each gain 'Whenever this creature attacks, create a 1/1 white
 *  Soldier creature token that's tapped and attacking.'"
 *
 * Pins the strive tax from both sides (two targets rejected on one Plains, accepted on two), and
 * that the granted trigger lives on each targeted creature alone: an untargeted attacker makes no
 * token, and every token enters tapped and attacking.
 */
class LaunchTheFleetScenarioTest : ScenarioTestBase() {

    private fun TestGame.launch(targets: List<EntityId>) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = state.getHand(player1Id).first { id ->
                state.getEntity(id)?.get<CardComponent>()?.name == "Launch the Fleet"
            },
            targets = targets.map { ChosenTarget.Permanent(it) },
        )
    )

    private fun TestGame.soldierTokens() = state.getBattlefield(player1Id).filter {
        state.getEntity(it)?.has<TokenComponent>() == true
    }

    private fun TestGame.attackAndResolve(attackers: Map<String, Int>) {
        advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(attackers).error shouldBe null
        resolveStack()
    }

    private fun TestGame.assertAttackingSoldiers(count: Int) {
        val tokens = soldierTokens()
        tokens shouldHaveSize count
        val projected = state.projectedState
        tokens.forEach { token ->
            projected.getPower(token) shouldBe 1
            projected.getToughness(token) shouldBe 1
            projected.getColors(token) shouldBe setOf(Color.WHITE.name)
            projected.getSubtypes(token).contains("Soldier") shouldBe true
            withClue("the token enters tapped") {
                state.getEntity(token)?.has<TappedComponent>() shouldBe true
            }
            withClue("the token enters attacking") {
                state.getEntity(token)?.get<AttackingComponent>() shouldNotBe null
            }
        }
    }

    init {
        test("one target costs {W}; only the targeted attacker makes a tapped and attacking Soldier") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardInHand(1, "Launch the Fleet")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.launch(listOf(game.findPermanent("Grizzly Bears")!!))
            withClue("a single target owes nothing extra: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.attackAndResolve(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2))

            withClue("Hill Giant wasn't targeted, so only Grizzly Bears' trigger makes a token") {
                game.assertAttackingSoldiers(1)
            }
        }

        test("two targets are rejected on one Plains") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardInHand(1, "Launch the Fleet")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.launch(
                listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Hill Giant")!!)
            )
            withClue("the second target owes {1} more, which one Plains can't pay") {
                cast.error shouldNotBe null
            }
        }

        test("two targets cost {1}{W} and each attacker makes its own Soldier") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardInHand(1, "Launch the Fleet")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.launch(
                listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Hill Giant")!!)
            )
            withClue("two Plains pay the {W} plus the strive {1}: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.attackAndResolve(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2))

            game.assertAttackingSoldiers(2)
        }
    }
}
