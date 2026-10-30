package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ironscale Hydra (THB #296) — "If a creature would deal combat damage to this creature, prevent
 * that damage and put a +1/+1 counter on this creature."
 *
 * The prevention's counter rider runs off the combat-damage path: one counter per creature whose
 * damage was prevented, however much it would have dealt (the ruling).
 */
class IronscaleHydraScenarioTest : ScenarioTestBase() {

    init {
        context("combat damage to Ironscale Hydra") {
            test("is prevented and becomes exactly one +1/+1 counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardOnBattlefield(2, "Ironscale Hydra")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Ironscale Hydra" to listOf("Hill Giant"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                val hydra = game.findPermanent("Ironscale Hydra")!!
                withClue("the Hill Giant's 3 combat damage was prevented") {
                    (game.state.getEntity(hydra)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
                }
                withClue("one counter for the one creature, not one per point of damage") {
                    game.state.getEntity(hydra)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
                withClue("the Hydra's own damage still lands") {
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                }
            }
        }
    }
}
