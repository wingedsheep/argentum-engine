package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.TheFiligreeSylex
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.DistributedCounterRemoval
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The Filigree Sylex (ONE #227) — {2} Legendary Artifact.
 *
 *   {T}: Put an oil counter on The Filigree Sylex.
 *   {T}, Sacrifice The Filigree Sylex: Destroy each nonland permanent with mana value equal to the
 *   number of oil counters on The Filigree Sylex.
 *   {T}, Remove ten oil counters from among permanents you control and sacrifice The Filigree
 *   Sylex: It deals 10 damage to any target.
 */
class TheFiligreeSylexScenarioTest : ScenarioTestBase() {

    private val addOil = TheFiligreeSylex.activatedAbilities[0].id
    private val wipe = TheFiligreeSylex.activatedAbilities[1].id
    private val blast = TheFiligreeSylex.activatedAbilities[2].id

    private fun oil(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    private fun setOil(game: TestGame, id: EntityId, count: Int) {
        game.state = game.state.updateEntity(id) { it.with(CountersComponent(mapOf(CounterType.OIL to count))) }
    }

    init {
        context("The Filigree Sylex") {

            test("tap: puts an oil counter on itself") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "The Filigree Sylex")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val sylex = game.findPermanent("The Filigree Sylex")!!

                game.execute(ActivateAbility(game.player1Id, sylex, addOil)).error shouldBe null
                game.state.getEntity(sylex)?.has<TappedComponent>() shouldBe true
                game.resolveStack()

                oil(game, sylex) shouldBe 1
            }

            test("sacrifice: destroys each nonland permanent whose mana value equals the sacrificed Sylex's oil count") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "The Filigree Sylex")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val sylex = game.findPermanent("The Filigree Sylex")!!
                setOil(game, sylex, 2)

                game.execute(ActivateAbility(game.player1Id, sylex, wipe)).error shouldBe null
                withClue("The sacrifice is a cost, paid on activation") {
                    game.isInGraveyard(1, "The Filigree Sylex") shouldBe true
                }
                game.resolveStack()

                withClue("Both mana value 2 creatures are destroyed, read from last-known oil count") {
                    game.findAllPermanents("Grizzly Bears").size shouldBe 0
                }
                withClue("Mana value 1 artifact and lands survive") {
                    game.isOnBattlefield("Bonesplitter") shouldBe true
                    game.findAllPermanents("Forest").size shouldBe 2
                }
            }

            test("sacrifice with no oil counters destroys mana value 0 permanents, spares mana value 2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "The Filigree Sylex")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val sylex = game.findPermanent("The Filigree Sylex")!!

                game.execute(ActivateAbility(game.player1Id, sylex, wipe)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Ornithopter") shouldBe false
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("remove ten oil counters from among permanents and sacrifice: 10 damage to any target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "The Filigree Sylex")
                    .withCardOnBattlefield(1, "Font of Progress")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val sylex = game.findPermanent("The Filigree Sylex")!!
                val font = game.findPermanent("Font of Progress")!!
                setOil(game, sylex, 6)
                setOil(game, font, 5)

                val act = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sylex,
                        abilityId = blast,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        costPayment = AdditionalCostPayment(
                            distributedCounterRemovals = listOf(
                                DistributedCounterRemoval(sylex, CounterType.OIL.printed, 6),
                                DistributedCounterRemoval(font, CounterType.OIL.printed, 4),
                            )
                        )
                    )
                )
                withClue("Activating should succeed: ${act.error}") { act.error shouldBe null }
                game.isInGraveyard(1, "The Filigree Sylex") shouldBe true
                oil(game, font) shouldBe 1

                game.resolveStack()

                game.getLifeTotal(2) shouldBe 10
            }

            test("can't activate the damage ability with fewer than ten oil counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "The Filigree Sylex")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val sylex = game.findPermanent("The Filigree Sylex")!!
                setOil(game, sylex, 9)

                val act = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sylex,
                        abilityId = blast,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        costPayment = AdditionalCostPayment(
                            distributedCounterRemovals = listOf(
                                DistributedCounterRemoval(sylex, CounterType.OIL.printed, 9),
                            )
                        )
                    )
                )
                act.outcome shouldNotBe Outcome.Done
                game.isOnBattlefield("The Filigree Sylex") shouldBe true
                oil(game, sylex) shouldBe 9
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
