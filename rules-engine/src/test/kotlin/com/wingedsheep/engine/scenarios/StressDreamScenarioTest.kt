package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Stress Dream (SOS #235).
 *
 * {3}{U}{R} Instant.
 *   "Stress Dream deals 5 damage to up to one target creature. Look at the top two cards of your
 *    library. Put one of those cards into your hand and the other on the bottom of your library."
 *
 * Composed from existing primitives: DealDamage(5) on an optional creature target, then
 * Patterns.Library.lookAtTopAndKeep (look top 2, keep 1 in hand, rest to bottom). The scenarios
 * prove (1) the happy path with a target, and (2) declining the optional target still runs the
 * library look.
 */
class StressDreamScenarioTest : ScenarioTestBase() {

    init {
        test("deals 5 damage to the target creature, keeps one of the top two, bottoms the other") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Stress Dream")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(2, "Grizzly Bears") // 2/2 — dies to 5 damage
                // Library top two cards (first added = top): Lightning Bolt, then Hill Giant.
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Plains") // a third card so "bottom" is meaningful
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val cardId = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Stress Dream"
            }

            val cast = game.execute(
                CastSpell(game.player1Id, cardId, listOf(ChosenTarget.Permanent(bears))),
            )
            withClue("Casting Stress Dream should succeed: ${cast.error}") {
                cast.error shouldBe null
            }
            game.resolveStack()

            // Keep Lightning Bolt from the looked-at top two.
            if (game.hasPendingDecision()) {
                val bolt = game.state.getLibrary(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt"
                }
                game.selectCards(listOf(bolt))
                game.resolveStack()
            }

            withClue("Grizzly Bears died to 5 damage") {
                game.findPermanent("Grizzly Bears") shouldBe null
            }
            withClue("Lightning Bolt is now in hand") {
                game.state.getZone(game.player1Id, Zone.HAND).mapNotNull {
                    game.state.getEntity(it)?.get<CardComponent>()?.name
                } shouldBe listOf("Lightning Bolt")
            }
            withClue("Hill Giant (the other looked-at card) is on the bottom of the library") {
                game.state.getLibrary(game.player1Id).mapNotNull {
                    game.state.getEntity(it)?.get<CardComponent>()?.name
                } shouldBe listOf("Plains", "Hill Giant")
            }
        }

        test("declining the optional creature target still looks at the top two and keeps one") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Stress Dream")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cardId = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Stress Dream"
            }

            // No target — decline the "up to one" creature.
            val cast = game.execute(CastSpell(game.player1Id, cardId, emptyList()))
            withClue("Declining the optional target is legal: ${cast.error}") {
                cast.error shouldBe null
            }
            game.resolveStack()
            if (game.hasPendingDecision()) {
                val bolt = game.state.getLibrary(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt"
                }
                game.selectCards(listOf(bolt))
                game.resolveStack()
            }

            withClue("Lightning Bolt is in hand even with no damage target") {
                game.state.getZone(game.player1Id, Zone.HAND).mapNotNull {
                    game.state.getEntity(it)?.get<CardComponent>()?.name
                } shouldBe listOf("Lightning Bolt")
            }
            withClue("Hill Giant is on the bottom") {
                game.state.getLibrary(game.player1Id).mapNotNull {
                    game.state.getEntity(it)?.get<CardComponent>()?.name
                } shouldBe listOf("Hill Giant")
            }
        }
    }
}
