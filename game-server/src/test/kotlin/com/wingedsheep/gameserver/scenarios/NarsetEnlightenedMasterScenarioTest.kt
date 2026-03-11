package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Narset, Enlightened Master.
 *
 * Whenever Narset attacks, exile the top four cards of your library.
 * Until end of turn, you may cast noncreature, nonland cards from among
 * those cards without paying their mana costs.
 */
class NarsetEnlightenedMasterScenarioTest : ScenarioTestBase() {

    init {
        context("Narset, Enlightened Master - attack trigger") {

            test("exiles top 4 cards and grants cast permission for noncreature nonland cards") {
                val game = scenario()
                    .withPlayers("Narset Player", "Opponent")
                    .withCardOnBattlefield(1, "Narset, Enlightened Master")
                    // Library: top 4 will be exiled
                    .withCardInLibrary(1, "Feed the Clan")     // instant - should get permissions
                    .withCardInLibrary(1, "Trumpet Blast")     // instant - should get permissions
                    .withCardInLibrary(1, "Glory Seeker")      // creature - should NOT get permissions
                    .withCardInLibrary(1, "Forest")            // land - should NOT get permissions
                    .withCardInLibrary(1, "Mountain")          // padding
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Move to combat and declare Narset as attacker
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Narset, Enlightened Master" to 2))

                // Resolve the triggered ability
                game.resolveStack()

                // Should have exiled 4 cards
                val exile = game.state.getExile(game.player1Id)
                withClue("Should have 4 cards in exile") {
                    exile shouldHaveSize 4
                }

                // Check permissions on exiled cards
                val exiledFeed = exile.firstOrNull { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Feed the Clan"
                }
                val exiledTrumpet = exile.firstOrNull { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Trumpet Blast"
                }
                val exiledCreature = exile.firstOrNull { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Glory Seeker"
                }
                val exiledLand = exile.firstOrNull { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Forest"
                }

                withClue("Feed the Clan should have MayPlayFromExileComponent") {
                    exiledFeed shouldNotBe null
                    game.state.getEntity(exiledFeed!!)?.get<MayPlayFromExileComponent>() shouldNotBe null
                }
                withClue("Feed the Clan should have PlayWithoutPayingCostComponent") {
                    game.state.getEntity(exiledFeed!!)?.get<PlayWithoutPayingCostComponent>() shouldNotBe null
                }

                withClue("Trumpet Blast should have MayPlayFromExileComponent") {
                    exiledTrumpet shouldNotBe null
                    game.state.getEntity(exiledTrumpet!!)?.get<MayPlayFromExileComponent>() shouldNotBe null
                }

                withClue("Glory Seeker (creature) should NOT have MayPlayFromExileComponent") {
                    exiledCreature shouldNotBe null
                    game.state.getEntity(exiledCreature!!)?.get<MayPlayFromExileComponent>() shouldBe null
                }

                withClue("Forest (land) should NOT have MayPlayFromExileComponent") {
                    exiledLand shouldNotBe null
                    game.state.getEntity(exiledLand!!)?.get<MayPlayFromExileComponent>() shouldBe null
                }
            }

            test("can cast exiled noncreature nonland cards without paying mana cost") {
                val game = scenario()
                    .withPlayers("Narset Player", "Opponent")
                    .withCardOnBattlefield(1, "Narset, Enlightened Master")
                    // Library: top 4 will be exiled
                    .withCardInLibrary(1, "Feed the Clan")
                    .withCardInLibrary(1, "Trumpet Blast")
                    .withCardInLibrary(1, "Glory Seeker")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Move to combat and declare Narset as attacker
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Narset, Enlightened Master" to 2))

                // Resolve the triggered ability
                game.resolveStack()

                // Find Feed the Clan in exile
                val exile = game.state.getExile(game.player1Id)
                val feedId = exile.first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Feed the Clan"
                }

                // Cast Feed the Clan from exile for free (no lands needed)
                val castResult = game.execute(CastSpell(game.player1Id, feedId))
                withClue("Cast from exile should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
            }
        }
    }
}
