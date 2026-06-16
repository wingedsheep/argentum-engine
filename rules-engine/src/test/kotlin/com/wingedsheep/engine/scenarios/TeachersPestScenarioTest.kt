package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Teacher's Pest (Secrets of Strixhaven #238).
 *
 * Teacher's Pest ({B}{G}, 1/1, Skeleton Pest):
 *   Menace
 *   Whenever this creature attacks, you gain 1 life.
 *   {B}{G}: Return this card from your graveyard to the battlefield tapped.
 *
 * Exercises the attacks-gain-life trigger and the graveyard-activated recursion that
 * returns the card to the battlefield tapped.
 */
class TeachersPestScenarioTest : ScenarioTestBase() {

    private fun life(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<LifeTotalComponent>()?.life ?: 0

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.has<TappedComponent>() ?: false

    init {
        context("Teacher's Pest — attack life gain + graveyard recursion") {

            test("attacking gains 1 life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Teacher's Pest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                    .build()

                val player1 = game.state.activePlayerId!!
                val startLife = life(game, player1)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Teacher's Pest" to 2))
                game.resolveStack()

                withClue("Attacking gains 1 life") {
                    life(game, player1) shouldBe startLife + 1
                }
            }

            test("graveyard ability returns the card to the battlefield tapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInGraveyard(1, "Teacher's Pest")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Starts in the graveyard, not on the battlefield") {
                    game.isInGraveyard(1, "Teacher's Pest") shouldBe true
                    game.isOnBattlefield("Teacher's Pest") shouldBe false
                }

                val graveyardCard = game.state.getGraveyard(game.state.activePlayerId!!)
                    .first { game.state.getEntity(it)
                        ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Teacher's Pest" }
                val abilityId = cardRegistry.getCard("Teacher's Pest")!!
                    .activatedAbilities.first().id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.state.activePlayerId!!,
                        sourceId = graveyardCard,
                        abilityId = abilityId
                    )
                )
                withClue("Activating the {B}{G} graveyard ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                val returned = game.findPermanent("Teacher's Pest")
                withClue("Teacher's Pest is back on the battlefield") { (returned != null) shouldBe true }
                withClue("It returns tapped") { isTapped(game, returned!!) shouldBe true }
            }
        }
    }
}
