package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Rip the Seams. */
class RipTheSeamsScenarioTest : ScenarioTestBase() {

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
        context("Rip the Seams — 'target tapped creature' is a targeting restriction") {
            test("a tapped creature is destroyed, and the card exiles itself (CR 715.3d)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Threadbind Clique")
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val clique = game.findCardsInHand(1, "Threadbind Clique").single()
                val bears = game.findPermanent("Grizzly Bears")!!

                // faceIndex = 0 is the Adventure face (CR 715).
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = clique,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                        faceIndex = 0,
                    )
                ).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isInExile(1, "Threadbind Clique") shouldBe true
            }

            test("an untapped creature is not a legal target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Threadbind Clique")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val clique = game.findCardsInHand(1, "Threadbind Clique").single()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = clique,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                        faceIndex = 0,
                    )
                ).error shouldNotBe null
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
