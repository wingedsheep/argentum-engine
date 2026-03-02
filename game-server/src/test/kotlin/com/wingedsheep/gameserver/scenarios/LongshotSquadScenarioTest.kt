package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Longshot Squad.
 *
 * Card reference:
 * - Longshot Squad ({3}{G}): Creature — Dog Archer 3/3
 *   Outlast {1}{G} ({1}{G}, {T}: Put a +1/+1 counter on this creature. Outlast only as a sorcery.)
 *   Each creature you control with a +1/+1 counter on it has reach.
 */
class LongshotSquadScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Longshot Squad outlast ability") {

            test("outlast puts a +1/+1 counter on Longshot Squad") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Longshot Squad")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val squad = game.findPermanent("Longshot Squad")!!
                val cardDef = cardRegistry.getCard("Longshot Squad")!!
                val outlastAbility = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = squad,
                        abilityId = outlastAbility.id
                    )
                )

                withClue("Outlast should activate successfully") {
                    result.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                val counters = game.state.getEntity(squad)?.get<CountersComponent>()
                withClue("Longshot Squad should have 1 +1/+1 counter") {
                    counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
            }

            test("creatures you control with +1/+1 counters gain reach") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Longshot Squad")
                    .withCardOnBattlefield(1, "Glory Seeker") // no counter, should not have reach
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val squad = game.findPermanent("Longshot Squad")!!
                val glorySeeker = game.findPermanent("Glory Seeker")!!

                // Before outlast, no one has reach
                val projectedBefore = stateProjector.project(game.state)
                withClue("Longshot Squad should not have reach before outlast") {
                    projectedBefore.hasKeyword(squad, Keyword.REACH) shouldBe false
                }
                withClue("Glory Seeker should not have reach") {
                    projectedBefore.hasKeyword(glorySeeker, Keyword.REACH) shouldBe false
                }

                // Activate outlast on Longshot Squad
                val cardDef = cardRegistry.getCard("Longshot Squad")!!
                val outlastAbility = cardDef.script.activatedAbilities.first()
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = squad,
                        abilityId = outlastAbility.id
                    )
                )
                game.resolveStack()

                // After outlast, Longshot Squad should have reach (has +1/+1 counter)
                val projectedAfter = stateProjector.project(game.state)
                withClue("Longshot Squad should have reach after getting +1/+1 counter") {
                    projectedAfter.hasKeyword(squad, Keyword.REACH) shouldBe true
                }
                // Glory Seeker should still not have reach (no counter)
                withClue("Glory Seeker without counter should not have reach") {
                    projectedAfter.hasKeyword(glorySeeker, Keyword.REACH) shouldBe false
                }
            }

            test("controller-only: opponent creatures with +1/+1 counters do not gain reach") {
                // Use Dragonscale Boon to put +1/+1 counters on opponent's creature
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Longshot Squad")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withCardInHand(1, "Dragonscale Boon")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glorySeeker = game.findPermanent("Glory Seeker")!!

                // Cast Dragonscale Boon targeting opponent's Glory Seeker
                game.castSpell(1, "Dragonscale Boon", glorySeeker)
                game.resolveStack()

                // Glory Seeker should have +1/+1 counters
                val counters = game.state.getEntity(glorySeeker)?.get<CountersComponent>()
                withClue("Glory Seeker should have +1/+1 counters") {
                    (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 2
                }

                // But Glory Seeker (opponent's creature) should NOT have reach
                val projected = stateProjector.project(game.state)
                withClue("Opponent's creature with +1/+1 counter should NOT get reach from Longshot Squad") {
                    projected.hasKeyword(glorySeeker, Keyword.REACH) shouldBe false
                }
            }
        }
    }
}
