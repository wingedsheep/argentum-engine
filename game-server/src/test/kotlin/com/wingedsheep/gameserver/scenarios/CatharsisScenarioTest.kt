package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import io.kotest.matchers.shouldBe
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo

/**
 * Scenario tests for Catharsis (Lorwyn Eclipsed)
 *
 * Tests mana-spent-gated ETB triggers, evoke alternative cost,
 * and the interaction between the two mechanics.
 */
class CatharsisScenarioTest : ScenarioTestBase() {

    init {
        test("Catharsis cast with {W}{W} creates two Kithkin tokens") {
            // Cast with 4 colorless + 2 white = white gate fires, red gate does not
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Catharsis")
                // 4 Plains + 2 more for the hybrid symbols (paid as white)
                .withLandsOnBattlefield(1, "Plains", 6)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .build()

            game.castSpell(1, "Catharsis")
            game.resolveStack()

            // White gate: 2 Kithkin tokens created
            val p1Battlefield = game.state.getBattlefield(game.player1Id)
            val tokens = p1Battlefield.filter { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Kithkin Token"
            }
            tokens.size shouldBe 2

            // Catharsis should be on the battlefield
            val catharsis = p1Battlefield.filter { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Catharsis"
            }
            catharsis.size shouldBe 1
        }

        test("Catharsis cast with {R}{R} gives creatures +1/+1 and haste") {
            // Cast with 4 colorless + 2 red = red gate fires, white gate does not
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Catharsis")
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withCardOnBattlefield(1, "Goldmeadow Nomad") // a creature to check buff
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .build()

            game.castSpell(1, "Catharsis")
            game.resolveStack()

            // Red gate: creatures get +1/+1 and haste until end of turn
            // Check Catharsis itself has the buff (3+1=4 power, 4+1=5 toughness)
            val projected = game.state.projectedState
            val catharsisId = game.state.getBattlefield(game.player1Id).find { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Catharsis"
            }!!
            projected.getPower(catharsisId) shouldBe 4
            projected.getToughness(catharsisId) shouldBe 5
        }

        test("Catharsis evoked for {W}{W} creates tokens then is sacrificed") {
            // Evoke with 2 white mana — white gate fires, creature is sacrificed
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Catharsis")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .build()

            game.castSpellWithAlternativeCost(1, "Catharsis")
            game.resolveStack()

            // White gate should have fired — 2 Kithkin tokens created
            val p1Battlefield = game.state.getBattlefield(game.player1Id)
            val tokens = p1Battlefield.filter { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Kithkin Token"
            }
            tokens.size shouldBe 2

            // Catharsis should be sacrificed (in graveyard)
            val graveyard = game.state.getZone(ZoneKey(game.player1Id, Zone.GRAVEYARD))
            val catharsisInGraveyard = graveyard.filter { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Catharsis"
            }
            catharsisInGraveyard.size shouldBe 1

            // Not on battlefield
            val catharsisOnBattlefield = p1Battlefield.filter { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Catharsis"
            }
            catharsisOnBattlefield.size shouldBe 0
        }

        test("Catharsis evoked for {R}{R} is sacrificed on ETB") {
            // Evoke with 2 red mana — red gate fires, creature is sacrificed
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Catharsis")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(1, "Goldmeadow Nomad") // 2/2
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .build()

            game.castSpellWithAlternativeCost(1, "Catharsis")
            game.resolveStack()

            // Catharsis should be sacrificed (in graveyard)
            val graveyard = game.state.getZone(ZoneKey(game.player1Id, Zone.GRAVEYARD))
            val catharsisInGraveyard = graveyard.filter { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Catharsis"
            }
            catharsisInGraveyard.size shouldBe 1
        }

        test("Catharsis cast with both {W}{W} and {R}{R} fires both gates") {
            // Use Steam Vents (UR dual) + Plains to have both colors available
            // 4 generic + {R}{W} hybrid = need to pay with at least 2W and 2R
            // Hmm, with {4}{R/W}{R/W} cost, to trigger both gates we need to ensure
            // at least {W}{W} and {R}{R} are among the total mana spent.
            // That means at least 4 colored mana + 4 generic = 8 total, but cost is only 6.
            // Actually, looking more carefully: the gates check ALL mana spent, not just the hybrid.
            // So paying {W}{W}{R}{R} + {2} generic = 6 total could trigger both gates
            // if 2W and 2R are among the 6 mana spent.
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Catharsis")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .build()

            game.castSpell(1, "Catharsis")
            game.resolveStack()

            // Both gates should fire
            val p1Battlefield = game.state.getBattlefield(game.player1Id)

            // White gate: 2 Kithkin tokens
            val tokens = p1Battlefield.filter { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Kithkin Token"
            }
            tokens.size shouldBe 2

            // Catharsis on battlefield (not evoked)
            val catharsis = p1Battlefield.filter { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Catharsis"
            }
            catharsis.size shouldBe 1

            // Red gate: Catharsis buffed to 4/5
            val projected = game.state.projectedState
            val catharsisId = catharsis.first()
            projected.getPower(catharsisId) shouldBe 4
            projected.getToughness(catharsisId) shouldBe 5
        }

    }
}
