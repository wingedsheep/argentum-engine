package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Wall of Limbs (Magic 2015 #121) — {2}{B} Creature — Zombie Wall, 0/3, Defender.
 *
 * "Whenever you gain life, put a +1/+1 counter on this creature.
 *  {5}{B}{B}, Sacrifice this creature: Target player loses X life, where X is this creature's power."
 *
 * The wall's printed power is 0, so X comes entirely from the +1/+1 counters it had when it was
 * sacrificed as a cost — last-known information, read after the wall is already in the graveyard.
 */
class WallOfLimbsScenarioTest : ScenarioTestBase() {

    init {
        context("Wall of Limbs") {

            test("each life gain adds a counter; sacrificing it drains its last-known power") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wall of Limbs")
                    .withCardOnBattlefield(1, "Soulmender", summoningSickness = false)
                    .withCardOnBattlefield(1, "Soulmender", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 7)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wall = game.findPermanent("Wall of Limbs")!!
                val soulmenderAbility = cardRegistry.getCard("Soulmender")!!.activatedAbilities.first().id

                // Two separate life-gain events: one counter each.
                for (soulmender in game.findPermanents("Soulmender")) {
                    game.execute(
                        ActivateAbility(
                            playerId = game.player1Id,
                            sourceId = soulmender,
                            abilityId = soulmenderAbility
                        )
                    ).error shouldBe null
                    game.resolveStack()
                }

                withClue("Wall of Limbs got a +1/+1 counter per life-gain event") {
                    game.state.getEntity(wall)!!.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                    game.state.projectedState.getPower(wall) shouldBe 2
                }
                game.getLifeTotal(1) shouldBe 22

                val wallAbility = cardRegistry.getCard("Wall of Limbs")!!.activatedAbilities.first().id
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wall,
                        abilityId = wallAbility,
                        targets = listOf(ChosenTarget.Player(game.player2Id))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the wall was sacrificed to pay the cost") {
                    game.isInGraveyard(1, "Wall of Limbs") shouldBe true
                }
                withClue("target player lost X = 2, the wall's power as it last existed on the battlefield") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }
        }
    }
}
