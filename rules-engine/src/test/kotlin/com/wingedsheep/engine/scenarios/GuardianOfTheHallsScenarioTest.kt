package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Guardian of the Halls (HOB #127) — {1}{G} Creature — Elf Soldier 2/2.
 *
 * "Trample
 *  {5}{G}{G}: Put three +1/+1 counters on this creature."
 *
 * Covers the printed trample, that the pump is *counters* (permanent, not until-end-of-turn),
 * and that the ability is unaffordable without seven mana.
 */
class GuardianOfTheHallsScenarioTest : ScenarioTestBase() {

    init {
        context("Guardian of the Halls") {

            test("it is a 2/2 with trample") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Guardian of the Halls")
                    .build()

                val guardian = game.findPermanent("Guardian of the Halls")!!
                game.state.projectedState.getPower(guardian) shouldBe 2
                game.state.projectedState.getToughness(guardian) shouldBe 2
                game.state.projectedState.hasKeyword(guardian, Keyword.TRAMPLE) shouldBe true
            }

            test("{5}{G}{G} puts three +1/+1 counters on it, making it a 5/5") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Guardian of the Halls")
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val guardian = game.findPermanent("Guardian of the Halls")!!
                val pump = cardRegistry.requireCard("Guardian of the Halls").activatedAbilities.single().id

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = guardian, abilityId = pump)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("three +1/+1 counters, not a temporary pump") {
                    game.state.getEntity(guardian)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
                }
                withClue("the counters show through the projection") {
                    game.state.projectedState.getPower(guardian) shouldBe 5
                    game.state.projectedState.getToughness(guardian) shouldBe 5
                }
            }

            test("the ability cannot be activated without seven mana available") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Guardian of the Halls")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val guardian = game.findPermanent("Guardian of the Halls")!!
                val pump = cardRegistry.requireCard("Guardian of the Halls").activatedAbilities.single().id

                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = guardian, abilityId = pump)
                )
                withClue("{5}{G}{G} is seven mana; six lands is not enough") {
                    result.error shouldNotBe null
                }
                withClue("no counters were placed") {
                    (game.state.getEntity(guardian)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
                }
            }
        }
    }
}
