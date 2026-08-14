package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Living Lectern. */
class LivingLecternScenarioTest : ScenarioTestBase() {

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
        context("Living Lectern — 'up to one other target creature you control'") {
            test("with a target chosen: draw a card and attach a Sorcerer Role to it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Living Lectern")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lectern = game.findPermanent("Living Lectern")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val handBefore = game.handSize(1)

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = lectern,
                        abilityId = lecternAbilityId,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                ).error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe handBefore + 1
                withClue("the Lectern sacrificed itself as part of the cost") {
                    game.isOnBattlefield("Living Lectern") shouldBe false
                }
                rolesAttachedTo(game, "Sorcerer Role", bears) shouldBe 1
                withClue("Sorcerer Role grants +1/+1 to the 2/2 Bears") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                }
            }

            test("declining the target still draws — even with no other creature to target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    // Deliberately your only creature, so there is no legal "other" target at all.
                    .withCardOnBattlefield(1, "Living Lectern")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lectern = game.findPermanent("Living Lectern")!!
                val handBefore = game.handSize(1)

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = lectern,
                        abilityId = lecternAbilityId,
                        targets = emptyList(),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("'up to one' declined — the draw is not gated on the Role") {
                    game.handSize(1) shouldBe handBefore + 1
                }
                withClue("no target chosen means no Sorcerer Role is created") {
                    game.findPermanent("Sorcerer Role") shouldBe null
                }
            }
        }
    }
}
