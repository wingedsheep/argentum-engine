package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Withering Hex.
 *
 * Card reference:
 * - Withering Hex ({B}): Enchantment — Aura
 *   Enchant creature
 *   Whenever a player cycles a card, put a plague counter on Withering Hex.
 *   Enchanted creature gets -1/-1 for each plague counter on Withering Hex.
 */
class WitheringHexScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    private fun TestGame.getPlagueCounters(entityId: EntityId): Int {
        return state.getEntity(entityId)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.PLAGUE) ?: 0
    }

    init {
        context("Withering Hex - cycling puts plague counters and debuffs creature") {

            test("cycling a card puts a plague counter on Withering Hex and debuffs enchanted creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Withering Hex")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2
                    .withCardInHand(1, "Disciple of Grace") // Has cycling {2}
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glorySeeker = game.findPermanent("Glory Seeker")!!

                // Cast Withering Hex targeting Glory Seeker
                game.castSpell(1, "Withering Hex", glorySeeker)
                game.resolveStack()

                val witheringHex = game.findPermanent("Withering Hex")!!

                // Initially: no plague counters, creature is 2/2
                withClue("Should start with 0 plague counters") {
                    game.getPlagueCounters(witheringHex) shouldBe 0
                }

                val projected0 = stateProjector.project(game.state)
                withClue("Glory Seeker should be 2/2 before cycling") {
                    projected0.getPower(glorySeeker) shouldBe 2
                    projected0.getToughness(glorySeeker) shouldBe 2
                }

                // Cycle the card - triggers Withering Hex
                game.cycleCard(1, "Disciple of Grace")
                // Resolve the triggered ability (puts plague counter on Withering Hex)
                game.resolveStack()

                withClue("Withering Hex should have 1 plague counter after cycling") {
                    game.getPlagueCounters(witheringHex) shouldBe 1
                }

                // Projected stats: Glory Seeker should be 1/1
                val projected1 = stateProjector.project(game.state)
                withClue("Glory Seeker should be 1/1 with 1 plague counter") {
                    projected1.getPower(glorySeeker) shouldBe 1
                    projected1.getToughness(glorySeeker) shouldBe 1
                }
            }

            test("multiple cycles accumulate plague counters and can kill creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Withering Hex")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2
                    .withCardInHand(1, "Disciple of Grace") // cycling {2}
                    .withCardInHand(1, "Disciple of Malice") // cycling {2}
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glorySeeker = game.findPermanent("Glory Seeker")!!

                // Cast Withering Hex targeting Glory Seeker
                game.castSpell(1, "Withering Hex", glorySeeker)
                game.resolveStack()

                val witheringHex = game.findPermanent("Withering Hex")!!

                // Cycle first card (Player 1)
                game.cycleCard(1, "Disciple of Grace")
                game.resolveStack()

                withClue("1 plague counter after first cycle") {
                    game.getPlagueCounters(witheringHex) shouldBe 1
                }

                // Cycle second card (Player 1)
                game.cycleCard(1, "Disciple of Malice")
                game.resolveStack()

                // Glory Seeker (2/2) with -2/-2 = 0/0, should be dead
                // (Withering Hex also goes to graveyard when creature dies)
                withClue("Glory Seeker should be dead (0/0 toughness)") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }

                withClue("Withering Hex should be in graveyard (enchanted creature died)") {
                    game.isOnBattlefield("Withering Hex") shouldBe false
                }
            }

            test("opponent cycling also triggers Withering Hex") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Withering Hex")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2
                    .withCardInHand(2, "Disciple of Malice") // cycling {2}
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glorySeeker = game.findPermanent("Glory Seeker")!!

                // Cast Withering Hex targeting Glory Seeker
                game.castSpell(1, "Withering Hex", glorySeeker)
                game.resolveStack()

                val witheringHex = game.findPermanent("Withering Hex")!!

                // Pass priority to P2 so they can cycle
                game.passPriority()

                // Opponent cycles
                game.cycleCard(2, "Disciple of Malice")
                game.resolveStack()

                withClue("1 plague counter from opponent cycling") {
                    game.getPlagueCounters(witheringHex) shouldBe 1
                }

                val projected = stateProjector.project(game.state)
                withClue("Glory Seeker should be 1/1") {
                    projected.getPower(glorySeeker) shouldBe 1
                    projected.getToughness(glorySeeker) shouldBe 1
                }
            }
        }
    }
}
