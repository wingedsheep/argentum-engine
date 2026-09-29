package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Polukranos Reborn // Polukranos, Engine of Ruin (MOM #200).
 *
 *   Front (4/5) — Reach. "{6}{W/P}: Transform Polukranos Reborn. Activate only as a sorcery."
 *   Back  (6/6) — Reach, lifelink. "Whenever Polukranos or another nontoken Hydra you control dies,
 *   create a 3/3 green and white Phyrexian Hydra creature token with reach and a 3/3 green and white
 *   Phyrexian Hydra creature token with lifelink."
 */
class PolukranosRebornScenarioTest : ScenarioTestBase() {

    private fun hydraTokens(game: TestGame, playerId: EntityId): List<EntityId> =
        game.state.getBattlefield(playerId).filter { id ->
            val e = game.state.getEntity(id) ?: return@filter false
            e.get<TokenComponent>() != null &&
                game.state.projectedState.hasSubtype(id, "Hydra")
        }

    private fun transform(game: TestGame): EntityId {
        val poly = game.findPermanent("Polukranos Reborn")!!
        val abilityId = cardRegistry.getCard("Polukranos Reborn")!!.activatedAbilities.first().id
        game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = poly, abilityId = abilityId)
        ).error shouldBe null
        if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
        game.resolveStack()
        return poly
    }

    private fun assertTokenPair(game: TestGame) {
        val tokens = hydraTokens(game, game.player1Id)
        tokens.size shouldBe 2
        tokens.forEach { t ->
            game.state.projectedState.getPower(t) shouldBe 3
            game.state.projectedState.getToughness(t) shouldBe 3
        }
        tokens.count { game.state.projectedState.hasKeyword(it, Keyword.REACH) } shouldBe 1
        tokens.count { game.state.projectedState.hasKeyword(it, Keyword.LIFELINK) } shouldBe 1
    }

    init {
        context("Polukranos Reborn") {

            test("front is a 4/5 reach; paying 2 life for {W/P} transforms it into a 6/6 reach lifelink") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Polukranos Reborn")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val front = game.findPermanent("Polukranos Reborn")!!
                game.state.projectedState.getPower(front) shouldBe 4
                game.state.projectedState.getToughness(front) shouldBe 5
                game.state.projectedState.hasKeyword(front, Keyword.REACH) shouldBe true

                val poly = transform(game)
                withClue("{W/P} was paid with 2 life") { game.getLifeTotal(1) shouldBe 18 }
                game.state.getEntity(poly)!!.get<CardComponent>()!!.name shouldBe "Polukranos, Engine of Ruin"
                game.state.projectedState.getPower(poly) shouldBe 6
                game.state.projectedState.getToughness(poly) shouldBe 6
                game.state.projectedState.hasKeyword(poly, Keyword.REACH) shouldBe true
                game.state.projectedState.hasKeyword(poly, Keyword.LIFELINK) shouldBe true
            }

            test("when the transformed Polukranos dies it leaves a reach Hydra and a lifelink Hydra") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Polukranos Reborn")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(1, "Murder")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val poly = transform(game)
                withClue("the Plains paid the {W/P} pip") { game.getLifeTotal(1) shouldBe 20 }

                game.castSpell(1, "Murder", poly).error shouldBe null
                game.resolveStack()
                game.resolveStack()

                game.isInGraveyard(1, "Polukranos Reborn") shouldBe true
                assertTokenPair(game)
            }

            test("another nontoken Hydra dying under the back face also makes the pair") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Polukranos Reborn")
                    .withCardOnBattlefield(1, "Managorger Hydra")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(1, "Murder")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                transform(game)
                val other = game.findPermanent("Managorger Hydra")!!
                game.castSpell(1, "Murder", other).error shouldBe null
                game.resolveStack()
                game.resolveStack()

                game.isOnBattlefield("Managorger Hydra") shouldBe false
                game.isOnBattlefield("Polukranos, Engine of Ruin") shouldBe true
                assertTokenPair(game)
            }

            test("the front face has no dies trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Polukranos Reborn")
                    .withCardOnBattlefield(1, "Managorger Hydra")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(1, "Murder")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val other = game.findPermanent("Managorger Hydra")!!
                game.castSpell(1, "Murder", other).error shouldBe null
                game.resolveStack()
                game.resolveStack()

                hydraTokens(game, game.player1Id).size shouldBe 0
            }
        }
    }
}
