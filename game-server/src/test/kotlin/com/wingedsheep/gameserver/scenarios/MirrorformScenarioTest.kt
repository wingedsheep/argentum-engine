package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Mirrorform.
 *
 * Card reference:
 * - Mirrorform ({4}{U}{U}): Instant
 *   "Each nonland permanent you control becomes a copy of target non-Aura permanent."
 */
class MirrorformScenarioTest : ScenarioTestBase() {

    init {
        context("Mirrorform — group copy effect") {

            test("each nonland permanent you control becomes a copy of the target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Mirrorform")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withCardOnBattlefield(2, "Rorix Bladewing")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rorix = game.findPermanent("Rorix Bladewing")!!

                val castResult = game.castSpell(1, "Mirrorform", targetId = rorix)
                withClue("Mirrorform should be cast: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                val p1Creatures = game.state.getBattlefield().filter { id ->
                    game.state.projectedState.getController(id) == game.player1Id &&
                        game.state.getEntity(id)?.get<CardComponent>()?.isCreature == true
                }

                withClue("Player 1 should still control exactly two creature permanents") {
                    p1Creatures.size shouldBe 2
                }

                for (creatureId in p1Creatures) {
                    val card = game.state.getEntity(creatureId)?.get<CardComponent>()
                    withClue("Creature $creatureId should have its CardComponent replaced with Rorix Bladewing") {
                        card?.name shouldBe "Rorix Bladewing"
                    }
                    withClue("Creature $creatureId should have Rorix Bladewing's printed 6/5 stats") {
                        game.state.projectedState.getPower(creatureId) shouldBe 6
                        game.state.projectedState.getToughness(creatureId) shouldBe 5
                    }

                    val copyOf = game.state.getEntity(creatureId)?.get<CopyOfComponent>()
                    withClue("Creature $creatureId should be tagged as a copy of Rorix Bladewing") {
                        copyOf shouldNotBe null
                        copyOf?.originalCardDefinitionId shouldBe "Grizzly Bears"
                        copyOf?.copiedCardDefinitionId shouldBe "Rorix Bladewing"
                    }
                }

                withClue("Lands should be unaffected — all six Islands still on the battlefield") {
                    val islands = game.state.getBattlefield().filter { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Island"
                    }
                    islands.size shouldBe 6
                }

                withClue("The target (opponent's Rorix) should retain its original identity") {
                    val targetCard = game.state.getEntity(rorix)?.get<CardComponent>()
                    targetCard?.name shouldBe "Rorix Bladewing"
                    game.state.getEntity(rorix)?.get<CopyOfComponent>() shouldBe null
                }
            }
        }
    }
}
