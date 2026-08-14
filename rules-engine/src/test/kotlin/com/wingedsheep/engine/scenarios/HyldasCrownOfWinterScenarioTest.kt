package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Hylda's Crown of Winter (WOE #247) — Legendary Artifact.
 *
 *   {1}, {T}: Tap target creature. This ability costs {1} less to activate during your turn.
 *   {3}, Sacrifice Hylda's Crown of Winter: Draw a card for each tapped creature your opponents
 *   control.
 *
 * The discount is proved by mana starvation rather than by reading a number: with **no lands at
 * all**, the tap ability is payable on your own turn (cost {0} + {T}) and not on an opponent's
 * (cost {1} + {T}), and one Island makes the off-turn activation work again.
 *
 * The draw counts only *tapped* creatures *opponents* control, so the third test deliberately
 * seeds an untapped opposing creature and a tapped creature of your own — neither may count.
 */
class HyldasCrownOfWinterScenarioTest : ScenarioTestBase() {

    init {
        context("Hylda's Crown of Winter") {

            test("during your turn the tap ability costs nothing but the {T}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hylda's Crown of Winter")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crown = game.findPermanent("Hylda's Crown of Winter")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val tapAbility = cardRegistry.getCard("Hylda's Crown of Winter")!!
                    .activatedAbilities[0].id

                // No lands anywhere: only the {1}-less discount makes this payable.
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crown,
                        abilityId = tapAbility,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                )
                withClue("with no mana available the discounted ability should still activate: ${result.error}") {
                    result.error shouldBe null
                }
                if (game.getPendingDecision() is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                withClue("the targeted creature is tapped") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
                }
            }

            test("on an opponent's turn the discount is gone, so {1} must be paid") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hylda's Crown of Winter")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crown = game.findPermanent("Hylda's Crown of Winter")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val tapAbility = cardRegistry.getCard("Hylda's Crown of Winter")!!
                    .activatedAbilities[0].id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crown,
                        abilityId = tapAbility,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                )

                withClue("the activation is rejected outright, not merely left unpaid") {
                    result.error shouldNotBe null
                }
                withClue("off-turn with no mana the {1} cannot be paid, so nothing is tapped") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
                    game.state.getEntity(crown)?.has<TappedComponent>() shouldBe false
                }
            }

            test("one Island covers the undiscounted {1} on an opponent's turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hylda's Crown of Winter")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crown = game.findPermanent("Hylda's Crown of Winter")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val tapAbility = cardRegistry.getCard("Hylda's Crown of Winter")!!
                    .activatedAbilities[0].id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crown,
                        abilityId = tapAbility,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                )
                withClue("Activating off-turn with one land should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                if (game.getPendingDecision() is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                withClue("the targeted creature is tapped") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
                }
            }

            test("the sacrifice ability draws one card per tapped creature opponents control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hylda's Crown of Winter")
                    // Two tapped creatures for the opponent → two cards.
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true, summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", tapped = true, summoningSickness = false)
                    // An untapped opposing creature and a tapped creature of *yours* must not count.
                    .withCardOnBattlefield(2, "Savannah Lions", summoningSickness = false)
                    .withCardOnBattlefield(1, "Air Elemental", tapped = true, summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Craw Wurm")
                    .withCardInLibrary(1, "Bog Imp")
                    .withCardInLibrary(1, "Wind Drake")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crown = game.findPermanent("Hylda's Crown of Winter")!!
                val sacrificeAbility = cardRegistry.getCard("Hylda's Crown of Winter")!!
                    .activatedAbilities[1].id
                val handBefore = game.handSize(1)

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crown,
                        abilityId = sacrificeAbility,
                    )
                )
                withClue("Activating the sacrifice ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                if (game.getPendingDecision() is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                withClue("two tapped opposing creatures → two cards; your own tapped Air Elemental is ignored") {
                    game.handSize(1) shouldBe handBefore + 2
                }
                withClue("the Crown was sacrificed as part of the cost") {
                    game.findPermanent("Hylda's Crown of Winter") shouldBe null
                }
            }

            test("no tapped creatures opponents control → no cards drawn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hylda's Crown of Winter")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Craw Wurm")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crown = game.findPermanent("Hylda's Crown of Winter")!!
                val sacrificeAbility = cardRegistry.getCard("Hylda's Crown of Winter")!!
                    .activatedAbilities[1].id
                val handBefore = game.handSize(1)

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crown,
                        abilityId = sacrificeAbility,
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                withClue("an empty count draws nothing rather than failing") {
                    game.handSize(1) shouldBe handBefore
                }
            }
        }
    }
}
