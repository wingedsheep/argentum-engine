package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Lodestone Myr (MRD #200).
 *
 * {4} Artifact Creature — Myr 2/2
 * "Trample
 *  Tap an untapped artifact you control: This creature gets +1/+1 until end of turn."
 *
 * The cost is "tap an untapped artifact you control", not the {T} symbol, which makes two things
 * true that a naive reading would miss: Lodestone Myr is itself a legal choice for its own cost,
 * and the ability can be activated as often as you have untapped artifacts left.
 */
class LodestoneMyrScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    private val pumpAbilityId by lazy {
        cardRegistry.requireCard("Lodestone Myr").activatedAbilities[0].id
    }

    init {
        fun isTapped(game: TestGame, id: EntityId): Boolean =
            game.state.getEntity(id)?.has<TappedComponent>() == true

        context("Lodestone Myr") {

            test("tapping another artifact pumps it +1/+1, and it can be activated repeatedly") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Lodestone Myr")
                    .withCardOnBattlefield(1, "Serum Tank")
                    .withCardOnBattlefield(1, "Leonin Scimitar")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myr = game.findPermanent("Lodestone Myr")!!
                val tank = game.findPermanent("Serum Tank")!!
                val scimitar = game.findPermanent("Leonin Scimitar")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = myr,
                        abilityId = pumpAbilityId,
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(tank))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("One activation: 2/2 becomes 3/3, and the tapped artifact paid for it") {
                    val projected = stateProjector.project(game.state)
                    projected.getPower(myr) shouldBe 3
                    projected.getToughness(myr) shouldBe 3
                    isTapped(game, tank) shouldBe true
                }

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = myr,
                        abilityId = pumpAbilityId,
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(scimitar))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("The ability has no {T} and no mana, so it stacks as often as you have artifacts") {
                    val projected = stateProjector.project(game.state)
                    projected.getPower(myr) shouldBe 4
                    projected.getToughness(myr) shouldBe 4
                }
            }

            test("can tap itself to pay — it is an artifact you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Lodestone Myr")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myr = game.findPermanent("Lodestone Myr")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = myr,
                        abilityId = pumpAbilityId,
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(myr))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("Lodestone Myr taps itself and is a tapped 3/3") {
                    isTapped(game, myr) shouldBe true
                    val projected = stateProjector.project(game.state)
                    projected.getPower(myr) shouldBe 3
                    projected.getToughness(myr) shouldBe 3
                }
            }

            test("cannot pay by tapping a non-artifact") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Lodestone Myr")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myr = game.findPermanent("Lodestone Myr")!!
                val giant = game.findPermanent("Hill Giant")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = myr,
                        abilityId = pumpAbilityId,
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(giant))
                    )
                )

                withClue("Hill Giant is not an artifact, so it can't pay the cost") {
                    result.error shouldNotBe null
                    isTapped(game, giant) shouldBe false
                }
            }
        }
    }
}
