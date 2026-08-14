package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Tempest Hart. */
class TempestHartScenarioTest : ScenarioTestBase() {

    private val lecternAbilityId by lazy {
        cardRegistry.requireCard("Living Lectern").activatedAbilities[0].id
    }

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun rolesAttachedTo(game: TestGame, roleName: String, host: EntityId): Int =
        game.findAllPermanents(roleName).count { role ->
            game.state.getEntity(role)?.get<AttachedToComponent>()?.targetId == host
        }

    init {
        context("Tempest Hart — the mana value 5 threshold") {
            test("a mana value 6 spell adds a counter, a mana value 2 spell does not") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tempest Hart")
                    .withCardInHand(1, "Craw Wurm")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hart = game.findPermanent("Tempest Hart")!!
                plusOneCounters(game, hart) shouldBe 0

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                withClue("Grizzly Bears is mana value 2 — below the threshold") {
                    plusOneCounters(game, hart) shouldBe 0
                }

                game.castSpell(1, "Craw Wurm").error shouldBe null
                game.resolveStack()
                withClue("Craw Wurm is mana value 6 — at or above 5") {
                    plusOneCounters(game, hart) shouldBe 1
                }
                withClue("3/4 base plus the counter") {
                    game.state.projectedState.getPower(hart) shouldBe 4
                    game.state.projectedState.getToughness(hart) shouldBe 5
                }
            }
        }
    }
}
