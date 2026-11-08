package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Perilous Predicament (AER #68) — {4}{B} Instant.
 *
 *   Each opponent sacrifices an artifact creature and a nonartifact creature of their choice.
 *
 * The opponent is asked once per category, the prompt says the pick is sacrificed, both picks go
 * at the same time, and the caster's own creatures are untouched.
 */
class PerilousPredicamentScenarioTest : ScenarioTestBase() {

    init {
        context("Perilous Predicament") {

            test("the opponent picks an artifact creature and a nonartifact creature, and sacrifices both") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Perilous Predicament")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withCardOnBattlefield(2, "Bronze Sable")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                fun opponents(name: String): EntityId =
                    game.state.getBattlefield().first { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == name &&
                            game.state.projectedState.getController(id) == EntityId.of("player-2")
                    }

                val sable = opponents("Bronze Sable")
                val giant = opponents("Hill Giant")

                game.castSpell(1, "Perilous Predicament").error shouldBe null
                game.resolveStack()

                withClue("the opponent chooses the artifact creature to sacrifice") {
                    val decision = game.getPendingDecision()!!
                    decision.playerId shouldBe EntityId.of("player-2")
                    decision.prompt shouldContain "artifact creature to sacrifice"
                }
                game.selectCards(listOf(sable)).error shouldBe null
                game.resolveStack()

                withClue("nothing is sacrificed until every pick is in") {
                    game.state.getBattlefield().contains(sable) shouldBe true
                }
                withClue("then the nonartifact creature to sacrifice") {
                    val decision = game.getPendingDecision()!!
                    decision.playerId shouldBe EntityId.of("player-2")
                    decision.prompt shouldContain "to sacrifice"
                }
                game.selectCards(listOf(giant)).error shouldBe null
                game.resolveStack()

                withClue("both picks are sacrificed, the others stay") {
                    game.isInGraveyard(2, "Bronze Sable") shouldBe true
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                    game.findCardsInGraveyard(2, "Ornithopter").size shouldBe 0
                    game.findCardsInGraveyard(2, "Grizzly Bears").size shouldBe 0
                }
                withClue("the caster's creatures are untouched") {
                    game.findPermanents("Ornithopter").size shouldBe 2
                    game.findPermanents("Grizzly Bears").size shouldBe 2
                    game.graveyardSize(1) shouldBe 1 // the spell itself
                }
            }

            test("an opponent with only artifact creatures sacrifices just one of them") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Perilous Predicament")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withCardOnBattlefield(2, "Bronze Sable")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ornithopter = game.findPermanent("Ornithopter")!!

                game.castSpell(1, "Perilous Predicament").error shouldBe null
                game.resolveStack()

                withClue("one artifact-creature pick, and no nonartifact question") {
                    game.getPendingDecision()!!.prompt shouldContain "artifact creature to sacrifice"
                }
                game.selectCards(listOf(ornithopter)).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.isInGraveyard(2, "Ornithopter") shouldBe true
                game.findPermanents("Bronze Sable").size shouldBe 1
            }
        }
    }
}
