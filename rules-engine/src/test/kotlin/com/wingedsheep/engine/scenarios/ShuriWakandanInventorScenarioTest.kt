package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.msh.cards.ShuriWakandanInventor
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Shuri, Wakandan Inventor (MSH #75) — {1}{U} Legendary Creature, 2/1, Uncommon.
 *
 * "Artifact spells you cast cost {1} less to cast.
 *  {1}, {T}: Target artifact you control becomes a copy of a second target artifact you control
 *  until end of turn, except it isn't legendary. Activate only as a sorcery."
 *
 * Focus: the **removal** direction of a copy exception (CR 707.9b) —
 * `CopyExceptions.removedSupertypes`. Copying a legendary artifact without stripping the supertype
 * would give one player two legendary permanents with the same name, and the legend rule
 * (CR 704.5j) would bin one of them as a state-based action.
 */
class ShuriWakandanInventorScenarioTest : ScenarioTestBase() {

    init {
        val copyAbilityId = ShuriWakandanInventor.activatedAbilities.first().id

        context("Shuri, Wakandan Inventor") {

            // Captain America's Shield is a Legendary Artifact — Equipment with indestructible;
            // Futurist Forge is a plain Artifact. The Forge becomes a copy of the Shield.
            fun board(): ScenarioTestBase.TestGame {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Shuri, Wakandan Inventor")
                    .withCardOnBattlefield(1, "Futurist Forge")
                    .withCardOnBattlefield(1, "Captain America's Shield")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                // The end-of-turn case crosses into the opponent's turn, so both libraries need
                // cards — the builder starts them empty and decking would end the game.
                repeat(5) {
                    builder = builder.withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")
                }
                return builder.build()
            }

            fun ScenarioTestBase.TestGame.copyShieldOntoForge(): Boolean {
                val shuri = findPermanent("Shuri, Wakandan Inventor")!!
                val forge = findPermanent("Futurist Forge")!!
                val shield = findPermanent("Captain America's Shield")!!
                val result = execute(
                    ActivateAbility(
                        playerId = player1Id,
                        sourceId = shuri,
                        abilityId = copyAbilityId,
                        targets = listOf(
                            ChosenTarget.Permanent(forge),
                            ChosenTarget.Permanent(shield),
                        ),
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
                resolveStack()
                return true
            }

            test("the artifact becomes a copy of the second target but isn't legendary") {
                val game = board()
                val forge = game.findPermanent("Futurist Forge")!!
                game.copyShieldOntoForge()

                val card = game.state.getEntity(forge)!!.get<CardComponent>()!!
                withClue("it copied the second target's copiable values") {
                    card.name shouldBe "Captain America's Shield"
                    card.typeLine.isEquipment shouldBe true
                }
                withClue("except it isn't legendary — the removal direction of the exception") {
                    card.typeLine.isLegendary shouldBe false
                }
                withClue("everything else is copied, including printed keywords") {
                    game.state.projectedState.hasKeyword(forge, Keyword.INDESTRUCTIBLE) shouldBe true
                }
            }

            test("both artifacts survive the legend rule") {
                val game = board()
                val forge = game.findPermanent("Futurist Forge")!!
                game.copyShieldOntoForge()
                game.checkStateBasedActions()

                withClue("two same-named permanents, only one legendary — nothing is sacrificed") {
                    game.state.getBattlefield().contains(forge) shouldBe true
                    game.findAllPermanents("Captain America's Shield").size shouldBe 2
                }
                withClue("the copy source keeps its own legendary supertype") {
                    val sourceId = game.findAllPermanents("Captain America's Shield")
                        .first { it != forge }
                    game.state.getEntity(sourceId)!!.get<CardComponent>()!!
                        .typeLine.isLegendary shouldBe true
                }
            }

            test("the copy is only until end of turn") {
                val game = board()
                val forge = game.findPermanent("Futurist Forge")!!
                game.copyShieldOntoForge()
                game.state.getEntity(forge)!!.get<CardComponent>()!!.name shouldBe
                    "Captain America's Shield"

                // Into the opponent's turn: the end-of-turn cleanup reverts the copy.
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                withClue("cleanup restored the pre-copy card component") {
                    game.state.getEntity(forge)!!.get<CardComponent>()!!.name shouldBe
                        "Futurist Forge"
                }
            }

            test("artifact spells you cast cost {1} less") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Shuri, Wakandan Inventor")
                    .withCardInHand(1, "Futurist Forge")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Futurist Forge costs {1}{U}; the discount makes it castable off a single Island.
                val result = game.castSpell(1, "Futurist Forge")
                withClue("cast should succeed with only one land: ${result.error}") {
                    result.error shouldBe null
                }
                if (game.getPendingDecision() is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()
                game.isOnBattlefield("Futurist Forge") shouldBe true
            }
        }
    }
}
