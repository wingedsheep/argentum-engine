package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class LongRiverLurkerWardTest : ScenarioTestBase() {

    init {
        context("Long River Lurker — ward granted to other Frogs") {
            test("ward counters ability when opponent can't pay ward cost") {
                // P1: Long River Lurker (grants ward {1} to other Frogs) + Stickytongue Sentinel (Frog)
                // P2: Mouse Trapper (Valiant: tap target creature) + High Stride (combat trick)
                // P2 casts High Stride on Mouse Trapper → Valiant triggers → P2 targets Stickytongue
                // Stickytongue should have ward {1} from Long River Lurker
                // P2 spent their only mana on High Stride, so ward auto-counters the ability
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Long River Lurker", summoningSickness = false)
                    .withCardOnBattlefield(1, "Stickytongue Sentinel", summoningSickness = false)
                    .withCardOnBattlefield(2, "Mouse Trapper", summoningSickness = false)
                    .withCardInHand(2, "High Stride")
                    .withLandsOnBattlefield(2, "Forest", 1) // Exactly enough for High Stride, nothing for ward
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Verify Stickytongue Sentinel has ward from Long River Lurker
                val sentinelId = game.findPermanent("Stickytongue Sentinel")!!
                val projected = game.state.projectedState
                withClue("Stickytongue Sentinel should have ward granted by Long River Lurker") {
                    projected.hasKeyword(sentinelId, Keyword.WARD) shouldBe true
                }

                // P2 casts High Stride targeting Mouse Trapper
                val mouseTrapperId = game.findPermanent("Mouse Trapper")!!
                val castResult = game.castSpell(2, "High Stride", mouseTrapperId)
                withClue("Should cast High Stride: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Valiant triggers — target selection decision appears immediately
                var iterations = 0
                while (iterations < 20) {
                    val decision = game.state.pendingDecision
                    if (decision != null) break
                    game.passPriority()
                    iterations++
                }

                // P2 targets Stickytongue Sentinel with the Valiant tap ability
                withClue("Should have a target selection for Valiant") {
                    game.state.pendingDecision shouldNotBe null
                }
                game.selectTargets(listOf(sentinelId))

                // Ward trigger fires because Stickytongue was targeted by opponent's ability
                // Resolve everything — ward auto-counters since P2 can't pay {1}
                game.resolveStack()

                // The Valiant ability was countered by ward — Stickytongue should NOT be tapped
                withClue("Stickytongue Sentinel should not be tapped (Valiant was countered by ward)") {
                    game.state.getEntity(sentinelId)?.has<TappedComponent>() shouldBe false
                }
            }
        }
    }
}
