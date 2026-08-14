package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Voyager Glidecar (DFT #36).
 *
 * Voyager Glidecar {W} — Artifact — Vehicle 2/3
 * When this Vehicle enters, scry 1.
 * Tap three other untapped creatures you control: Until end of turn, this Vehicle becomes an
 * artifact creature and gains flying. Put a +1/+1 counter on it.
 * Crew 1
 *
 * The load-bearing claims:
 *  - the tap-three cost is enforced — two creatures isn't enough;
 *  - paying it taps exactly the three chosen creatures and animates the Vehicle with flying,
 *    keeping its printed 2/3 as the base P/T (so the +1/+1 counter reads 3/4);
 *  - only the animate is duration-bounded — the counter survives into the next turn.
 */
class VoyagerGlidecarScenarioTest : ScenarioTestBase() {

    private val abilityId
        get() = cardRegistry.getCard("Voyager Glidecar")!!.script.activatedAbilities[0].id

    private fun boardWith(creatureCount: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Voyager Glidecar")
        .also { builder -> repeat(creatureCount) { builder.withCardOnBattlefield(1, "Grizzly Bears") } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Voyager Glidecar") {

            test("tapping three other creatures animates the Vehicle and adds a +1/+1 counter") {
                val game = boardWith(creatureCount = 3)
                val glidecar = game.findPermanent("Voyager Glidecar")!!
                val bears = game.findPermanents("Grizzly Bears")
                bears.size shouldBe 3

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = glidecar,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(tappedPermanents = bears)
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("all three creatures paid the cost") {
                    bears.forEach { game.state.getEntity(it)!!.get<TappedComponent>().shouldNotBeNull() }
                }

                val projected = game.state.projectedState
                withClue("the Vehicle is an artifact creature with flying at 2/3 base plus a counter") {
                    projected.isCreature(glidecar) shouldBe true
                    projected.hasType(glidecar, "ARTIFACT") shouldBe true
                    projected.hasKeyword(glidecar, Keyword.FLYING) shouldBe true
                    projected.getPower(glidecar) shouldBe 3
                    projected.getToughness(glidecar) shouldBe 4
                }

                game.state.getEntity(glidecar)!!.get<CountersComponent>()!!
                    .getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("only the animate was until-end-of-turn; the counter is permanent") {
                    game.state.projectedState.isCreature(glidecar) shouldBe false
                    game.state.getEntity(glidecar)!!.get<CountersComponent>()!!
                        .getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
            }

            test("cannot be activated with only two other untapped creatures") {
                val game = boardWith(creatureCount = 2)
                val glidecar = game.findPermanent("Voyager Glidecar")!!
                val bears = game.findPermanents("Grizzly Bears")

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = glidecar,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(tappedPermanents = bears)
                    )
                )
                withClue("the cost demands three creatures") { (result.error != null) shouldBe true }
            }
        }
    }
}
