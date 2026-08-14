package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Spire Mechcycle (DFT #147) — {4}{R} Artifact — Vehicle 5/4.
 *
 * "Haste
 *  Exhaust — Tap another untapped Mount or Vehicle you control: This Vehicle becomes an artifact
 *  creature. Put a +1/+1 counter on it for each Mount and/or Vehicle you control other than this
 *  Vehicle.
 *  Crew 2"
 *
 * Three claims worth pinning:
 *
 * - the cost's "**another**" — the Mechcycle cannot tap itself to pay, and with nothing else to tap
 *   the ability is unactivatable;
 * - the counter count excludes the Mechcycle but *includes* the permanent tapped to pay, since that
 *   one is still on the battlefield when the ability resolves;
 * - the animate has no "until end of turn" clause, so it survives cleanup (as on Marshals'
 *   Pathcruiser).
 */
class SpireMechcycleScenarioTest : ScenarioTestBase() {

    private val exhaustAbilityId
        get() = cardRegistry.getCard("Spire Mechcycle")!!.script.activatedAbilities[0].id

    init {
        context("Spire Mechcycle's exhaust ability") {

            test("animates permanently with one counter per other Mount/Vehicle") {
                // Three other Vehicles: one gets tapped to pay, and all three still count.
                val game = mechcycleGame(otherVehicles = 3)
                val mechcycle = game.findPermanent("Spire Mechcycle")!!
                val ferries = game.findPermanents("Skybox Ferry")

                withClue("a Vehicle is not a creature until something animates it") {
                    game.state.projectedState.isCreature(mechcycle) shouldBe false
                }

                val result = game.activateTapping(mechcycle, ferries.first())
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                withClue("the chosen Vehicle was tapped to pay") {
                    game.state.getEntity(ferries.first())!!.has<TappedComponent>() shouldBe true
                }
                game.resolveStack()

                withClue("the tapped-as-cost Vehicle is still on the battlefield, so all 3 count") {
                    game.state.getEntity(mechcycle)!!.get<CountersComponent>()!!
                        .getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
                }
                val projected = game.state.projectedState
                withClue("5/4 base plus three +1/+1 counters, still an artifact") {
                    projected.isCreature(mechcycle) shouldBe true
                    projected.hasType(mechcycle, "ARTIFACT") shouldBe true
                    projected.getPower(mechcycle) shouldBe 8
                    projected.getToughness(mechcycle) shouldBe 7
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("no 'until end of turn' clause — cleanup must not undo it") {
                    game.state.projectedState.isCreature(mechcycle) shouldBe true
                }
            }

            test("the Mechcycle cannot tap itself to pay") {
                val game = mechcycleGame(otherVehicles = 1)
                val mechcycle = game.findPermanent("Spire Mechcycle")!!

                val result = game.activateTapping(mechcycle, mechcycle)
                withClue("'another' excludes the source even when it is untapped") {
                    result.error shouldNotBe null
                }
                game.state.projectedState.isCreature(mechcycle) shouldBe false
            }

            test("with no other Mount or Vehicle the ability cannot be activated") {
                val game = mechcycleGame(otherVehicles = 0)
                val mechcycle = game.findPermanent("Spire Mechcycle")!!

                val result = game.execute(
                    ActivateAbility(game.player1Id, mechcycle, exhaustAbilityId)
                )
                withClue("nothing legal to tap, so the cost is unpayable") {
                    result.error shouldNotBe null
                }
                game.state.projectedState.isCreature(mechcycle) shouldBe false
            }

            test("it is an exhaust ability — activate only once") {
                val game = mechcycleGame(otherVehicles = 2)
                val mechcycle = game.findPermanent("Spire Mechcycle")!!
                val ferries = game.findPermanents("Skybox Ferry")

                game.activateTapping(mechcycle, ferries[0]).error shouldBe null
                game.resolveStack()

                val second = game.activateTapping(mechcycle, ferries[1])
                withClue("a second activation must be rejected even with an untapped Vehicle left") {
                    second.error shouldNotBe null
                }
                game.state.getEntity(mechcycle)!!.get<CountersComponent>()!!
                    .getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            }
        }
    }

    /** Activate the exhaust ability, paying by tapping [tapped]. */
    private fun TestGame.activateTapping(mechcycle: EntityId, tapped: EntityId) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = mechcycle,
            abilityId = exhaustAbilityId,
            costPayment = AdditionalCostPayment(tappedPermanents = listOf(tapped))
        )
    )

    /**
     * Spire Mechcycle plus [otherVehicles] copies of Skybox Ferry — an ability-free Vehicle, so the
     * only things in play that could tap, animate or count are the ones under test.
     */
    private fun mechcycleGame(otherVehicles: Int): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Spire Mechcycle")
        repeat(otherVehicles) {
            builder.withCardOnBattlefield(1, "Skybox Ferry")
        }
        return builder
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }
}
