package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Devastating Onslaught (Edge of Eternities).
 *
 * {X}{X}{R} Sorcery
 * Create X tokens that are copies of target artifact or creature you control.
 * Those tokens gain haste until end of turn. Sacrifice them at the beginning of
 * the next end step.
 */
class DevastatingOnslaughtScenarioTest : ScenarioTestBase() {

    init {
        context("Cast targeting a creature") {
            test("X=2 creates two token copies with haste") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Devastating Onslaught")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!
                val castResult = game.castXSpell(1, "Devastating Onslaught", xValue = 2, targetId = hillGiantId)
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // Original + two token copies = three Hill Giants total
                val hillGiants = game.findAllPermanents("Hill Giant")
                withClue("X=2 should create two token copies on top of the original") {
                    hillGiants shouldHaveSize 3
                }

                val tokens = hillGiants.filter { it != hillGiantId }
                tokens shouldHaveSize 2
                for (tokenId in tokens) {
                    val container = game.state.getEntity(tokenId)!!
                    withClue("Token $tokenId should carry TokenComponent") {
                        container.get<TokenComponent>() shouldBe TokenComponent
                    }
                    withClue("Token $tokenId should have haste in its base keywords") {
                        container.get<CardComponent>()!!.baseKeywords shouldContain Keyword.HASTE
                    }
                    withClue("Token $tokenId should be controlled by the caster") {
                        container.get<ControllerComponent>()?.playerId shouldBe game.player1Id
                    }
                }
            }

            test("tokens are sacrificed at the beginning of the next end step") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Devastating Onslaught")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!
                game.castXSpell(1, "Devastating Onslaught", xValue = 2, targetId = hillGiantId)
                game.resolveStack()

                game.findAllPermanents("Hill Giant") shouldHaveSize 3

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                val survivors = game.findAllPermanents("Hill Giant")
                withClue("All token copies should be sacrificed at end step, original remains") {
                    survivors shouldHaveSize 1
                    survivors.first() shouldBe hillGiantId
                }
            }

            test("X=0 casts legally and creates no tokens") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Devastating Onslaught")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!
                val castResult = game.castXSpell(1, "Devastating Onslaught", xValue = 0, targetId = hillGiantId)
                withClue("X=0 cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("X=0 should leave only the original Hill Giant on the battlefield") {
                    game.findAllPermanents("Hill Giant") shouldHaveSize 1
                }
            }
        }

        context("Cast targeting an artifact") {
            test("X=1 creates a token copy of a non-creature artifact") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Devastating Onslaught")
                    .withCardOnBattlefield(1, "Sol Ring")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val solRingId = game.findPermanent("Sol Ring")!!
                val castResult = game.castXSpell(1, "Devastating Onslaught", xValue = 1, targetId = solRingId)
                withClue("Cast targeting an artifact should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                val solRings = game.findAllPermanents("Sol Ring")
                withClue("X=1 should produce one token copy alongside the original") {
                    solRings shouldHaveSize 2
                }

                val tokenId = solRings.first { it != solRingId }
                game.state.getEntity(tokenId)!!.get<TokenComponent>() shouldBe TokenComponent
            }
        }
    }
}
