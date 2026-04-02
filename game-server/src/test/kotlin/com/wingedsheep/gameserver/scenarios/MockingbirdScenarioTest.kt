package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Mockingbird.
 *
 * Mockingbird {X}{U}
 * Creature — Bird Bard
 * 1/1
 * Flying
 * You may have this creature enter as a copy of any creature on the battlefield
 * with mana value less than or equal to the amount of mana spent to cast this creature,
 * except it's a Bird in addition to its other types and it has flying.
 */
class MockingbirdScenarioTest : ScenarioTestBase() {

    init {
        context("Mockingbird — clone with mana value restriction") {

            test("copies a creature with mana value <= total mana spent") {
                // Cast Mockingbird with X=2, total mana spent = 3 (X=2 + U=1)
                // Glory Seeker has MV 2, which is <= 3, so it should be copyable
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Mockingbird")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardOnBattlefield(2, "Glory Seeker") // MV 2
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castXSpell(1, "Mockingbird", xValue = 2)
                withClue("Should cast Mockingbird: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Should be presented with clone selection decision
                withClue("Should have pending clone selection decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select Glory Seeker to copy
                val glorySeeker = game.findPermanent("Glory Seeker")
                withClue("Glory Seeker should be on battlefield") {
                    glorySeeker shouldNotBe null
                }
                game.selectCards(listOf(glorySeeker!!))

                // The copy should be on the battlefield as "Glory Seeker" with Bird subtype and flying
                val copies = game.state.getBattlefield(game.player1Id).filter { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Glory Seeker"
                }
                withClue("Player should control a copy of Glory Seeker") {
                    copies.size shouldBe 1
                }

                val copyCard = game.state.getEntity(copies.first())?.get<CardComponent>()!!
                withClue("Copy should have Bird subtype") {
                    copyCard.typeLine.hasSubtype(Subtype.BIRD) shouldBe true
                }
                withClue("Copy should have flying") {
                    copyCard.baseKeywords.contains(Keyword.FLYING) shouldBe true
                }
            }

            test("cannot copy creature with mana value > total mana spent") {
                // Cast Mockingbird with X=0, total mana spent = 1 (X=0 + U=1)
                // Glory Seeker has MV 2, which is > 1, so it should NOT be copyable
                // No valid targets means Mockingbird enters as a 1/1
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Mockingbird")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardOnBattlefield(2, "Glory Seeker") // MV 2
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castXSpell(1, "Mockingbird", xValue = 0)
                withClue("Should cast Mockingbird: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // No valid copy targets, so Mockingbird should enter as itself (1/1)
                val mockingbird = game.findPermanent("Mockingbird")
                withClue("Mockingbird should be on battlefield as itself") {
                    mockingbird shouldNotBe null
                }
                val card = game.state.getEntity(mockingbird!!)?.get<CardComponent>()!!
                withClue("Should be a 1/1") {
                    card.baseStats?.basePower shouldBe 1
                    card.baseStats?.baseToughness shouldBe 1
                }
            }

            test("optional — player can decline to copy") {
                // Cast Mockingbird with X=2, total mana spent = 3
                // Glory Seeker has MV 2, which is <= 3, but player declines
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Mockingbird")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardOnBattlefield(2, "Glory Seeker") // MV 2
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castXSpell(1, "Mockingbird", xValue = 2)
                withClue("Should cast Mockingbird: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Skip selection (decline to copy)
                game.skipSelection()

                // Mockingbird should be on battlefield as itself (1/1 Bird Bard)
                val mockingbird = game.findPermanent("Mockingbird")
                withClue("Mockingbird should be on battlefield as itself") {
                    mockingbird shouldNotBe null
                }
            }
        }
    }
}
