package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mrd.cards.SerumTank
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Serum Tank (MRD #240) — "Whenever this artifact or another artifact enters, put a charge counter
 * on this artifact." / "{3}, {T}, Remove a charge counter from this artifact: Draw a card."
 *
 * The two things worth proving are the halves of the trigger's scope, both of which a narrower
 * wiring would silently drop: `TriggerBinding.ANY` is what makes the Tank see *itself* enter (the
 * "this artifact or" clause), and the unscoped artifact filter is what makes an *opponent's*
 * artifact charge it too — the oracle text says "another artifact", not "another artifact you
 * control".
 */
class SerumTankScenarioTest : ScenarioTestBase() {

    private fun chargeCounters(state: com.wingedsheep.engine.state.GameState, id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0

    init {
        context("Serum Tank") {

            test("it sees itself enter and arrives with one charge counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Serum Tank")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Serum Tank").error shouldBe null
                game.resolveStack()

                val tank = game.findPermanent("Serum Tank")!!
                withClue("'this artifact or another artifact' includes the Tank's own entry") {
                    chargeCounters(game.state, tank) shouldBe 1
                }
            }

            test("another artifact you control entering adds a counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Serum Tank")
                    .withCardInHand(1, "Bonesplitter")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tank = game.findPermanent("Serum Tank")!!
                withClue("the Tank was placed directly, so it starts empty") {
                    chargeCounters(game.state, tank) shouldBe 0
                }

                game.castSpell(1, "Bonesplitter").error shouldBe null
                game.resolveStack()

                chargeCounters(game.state, tank) shouldBe 1
            }

            test("an opponent's artifact charges it too — the filter is not 'you control'") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Serum Tank")
                    .withCardInHand(2, "Bonesplitter")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tank = game.findPermanent("Serum Tank")!!
                game.castSpell(2, "Bonesplitter").error shouldBe null
                game.resolveStack()

                withClue("the oracle text says 'another artifact', with no controller restriction") {
                    chargeCounters(game.state, tank) shouldBe 1
                }
            }

            test("{3}, {T}, remove a charge counter: draw a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Serum Tank")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast it so its own entry trigger supplies the counter the draw needs.
                game.castSpell(1, "Serum Tank").error shouldBe null
                game.resolveStack()

                val tank = game.findPermanent("Serum Tank")!!
                chargeCounters(game.state, tank) shouldBe 1
                val handBefore = game.handSize(1)

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = tank,
                        abilityId = SerumTank.activatedAbilities[0].id,
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.handSize(1) shouldBe handBefore + 1
                withClue("removing the counter is part of the cost, so it is gone") {
                    chargeCounters(game.state, tank) shouldBe 0
                }
            }

            test("with no charge counter the ability can't be activated") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Serum Tank")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tank = game.findPermanent("Serum Tank")!!
                chargeCounters(game.state, tank) shouldBe 0

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = tank,
                        abilityId = SerumTank.activatedAbilities[0].id,
                    )
                )
                withClue("the counter-removal cost is unpayable with zero counters") {
                    (result.error != null) shouldBe true
                }
            }
        }
    }
}
