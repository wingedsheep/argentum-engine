package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Xathrid Gorgon (M13 #118) — {5}{B} Creature — Gorgon 3/6, deathtouch.
 *
 *   {2}{B}, {T}: Put a petrification counter on target creature. It gains defender and becomes a
 *   colorless artifact in addition to its other types. Its activated abilities can't be activated.
 *
 * Every rider is permanent, so the petrified creature is still petrified on its controller's turn.
 */
class XathridGorgonScenarioTest : ScenarioTestBase() {

    private val gorgonAbilityId =
        cardRegistry.getCard("Xathrid Gorgon")!!.activatedAbilities.first().id
    private val elvesManaAbilityId =
        cardRegistry.getCard("Llanowar Elves")!!.script.activatedAbilities.first().id

    private fun TestGame.canActivateElves(playerNumber: Int, elves: EntityId): Boolean =
        getLegalActions(playerNumber).any {
            val a = it.action
            a is ActivateAbility && a.sourceId == elves && a.abilityId == elvesManaAbilityId
        }

    init {
        context("Xathrid Gorgon") {

            test("petrifies a creature: counter, defender, colorless artifact creature, no activations") {
                var builder = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Xathrid Gorgon", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(2, "Llanowar Elves", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) {
                    builder = builder.withCardInLibrary(1, "Swamp").withCardInLibrary(2, "Forest")
                }
                val game = builder.build()
                val gorgon = game.findPermanent("Xathrid Gorgon")!!
                val elves = game.findPermanent("Llanowar Elves")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = gorgon,
                        abilityId = gorgonAbilityId,
                        targets = listOf(ChosenTarget.Permanent(elves)),
                    )
                )
                withClue("activation: ${result.error}") { result.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.state.getEntity(elves)?.get<CountersComponent>()
                    ?.getCount(CounterType.PETRIFICATION) shouldBe 1

                // On its controller's turn the petrification still holds: nothing was until-end-of-turn.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id
                val projected = game.state.projectedState
                withClue("gains defender") { projected.hasKeyword(elves, Keyword.DEFENDER) shouldBe true }
                withClue("becomes an artifact") { projected.hasType(elves, "ARTIFACT") shouldBe true }
                withClue("in addition to its other types") { projected.isCreature(elves) shouldBe true }
                withClue("becomes colorless") { projected.getColors(elves).shouldBeEmpty() }
                withClue("its mana ability can't be activated") {
                    game.canActivateElves(2, elves) shouldBe false
                }
            }
        }
    }
}
