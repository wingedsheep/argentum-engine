package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import io.kotest.matchers.shouldBe

/**
 * Bat Colony's enters-the-battlefield ability makes "a 1/1 black Bat with flying for each mana from
 * a Cave spent to cast it" — the [com.wingedsheep.sdk.scripting.values.DynamicAmount.ManaSpentFromSubtype]
 * count backed by the engine's mana-source provenance. The pool is seeded with Cave-tagged mana so the
 * cast spends it; the resolved permanent's `CastRecordComponent.manaSpentBySubtype` carries the count
 * to the ETB trigger.
 */
class BatColonyScenarioTest : ScenarioTestBase() {
    init {
        context("Bat Colony — a Bat for each mana from a Cave spent") {
            test("three Cave mana spent makes three Bats") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Bat Colony")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Seed the pool with the {2}{W} cost entirely as Cave-tagged mana.
                game.state = game.state.updateEntity(game.player1Id) { container ->
                    container.with(
                        ManaPoolComponent(
                            white = 3,
                            manaBySubtype = mapOf(Subtype.CAVE to 3),
                        )
                    )
                }

                game.castSpell(1, "Bat Colony").error shouldBe null
                game.resolveStack()

                game.findPermanents("Bat Token").size shouldBe 3
            }

            test("no Cave mana spent makes no Bats") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Bat Colony")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Pay the same cost with plain (untagged) white mana — zero Cave provenance.
                game.state = game.state.updateEntity(game.player1Id) { container ->
                    container.with(ManaPoolComponent(white = 3))
                }

                game.castSpell(1, "Bat Colony").error shouldBe null
                game.resolveStack()

                game.findPermanents("Bat Token").size shouldBe 0
            }

            test("only part of the payment is Cave mana — Bats scale to the Cave portion") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Bat Colony")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Two of the three mana spent carry the Cave tag.
                game.state = game.state.updateEntity(game.player1Id) { container ->
                    container.with(
                        ManaPoolComponent(
                            white = 3,
                            manaBySubtype = mapOf(Subtype.CAVE to 2),
                        )
                    )
                }

                game.castSpell(1, "Bat Colony").error shouldBe null
                game.resolveStack()

                game.findPermanents("Bat Token").size shouldBe 2
            }
        }
    }
}
