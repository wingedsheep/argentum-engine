package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.chosenCardName
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Petrified Hamlet (Secrets of Strixhaven #259).
 *
 * Petrified Hamlet (Land):
 *   When this land enters, choose a land card name.
 *   Activated abilities of sources with the chosen name can't be activated unless they're
 *     mana abilities.
 *   Lands with the chosen name have "{T}: Add {C}."
 *   {T}: Add {C}.
 *
 * "Choose a land card name" is an as-enters choice (ChoiceType.CARD_NAME) stored durably on the
 * permanent's CastChoicesComponent under ChoiceSlot.CARD_NAME. Both name-keyed static abilities
 * read that choice through CardPredicate.NameEqualsChosenComponent (filter
 * .namedFromChosenComponent()), which is static-projection / activation-legality safe.
 */
class PetrifiedHamletScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseCardNameOnEnter(name: String) {
        val decision = getPendingDecision()
        withClue("Petrified Hamlet must present an as-enters card-name choice") {
            (decision is ChooseOptionDecision) shouldBe true
        }
        decision as ChooseOptionDecision
        withClue("The chosen land name must be offered") {
            decision.options shouldContain name
        }
        submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(name)))
    }

    private fun TestGame.activatableAbilityCount(sourceId: com.wingedsheep.sdk.model.EntityId): Int =
        getLegalActions(1)
            .filter { (it.action as? ActivateAbility)?.sourceId == sourceId }
            .count()

    private fun TestGame.activatableNonManaAbilityCount(sourceId: com.wingedsheep.sdk.model.EntityId): Int =
        getLegalActions(1)
            .filter { (it.action as? ActivateAbility)?.sourceId == sourceId }
            .count { !it.isManaAbility }

    private fun TestGame.activatableManaAbilityCount(sourceId: com.wingedsheep.sdk.model.EntityId): Int =
        getLegalActions(1)
            .filter { (it.action as? ActivateAbility)?.sourceId == sourceId }
            .count { it.isManaAbility }

    init {
        context("Petrified Hamlet — choose a land card name on enter") {

            test("entering presents land card names and stores the pick durably") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Petrified Hamlet")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hamletInHand = game.findCardsInHand(1, "Petrified Hamlet").first()
                game.execute(PlayLand(game.player1Id, hamletInHand))

                game.chooseCardNameOnEnter("Keldon Necropolis")

                val hamlet = game.findPermanent("Petrified Hamlet")!!
                withClue("The chosen name is stored under ChoiceSlot.CARD_NAME") {
                    game.state.getEntity(hamlet)?.chosenCardName() shouldBe "Keldon Necropolis"
                }
            }
        }

        context("Petrified Hamlet — PreventActivatedAbilities (non-mana only) on the named source") {

            test("naming a land shuts off its non-mana ability but leaves its mana ability legal") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Petrified Hamlet")
                    .withCardOnBattlefield(1, "Keldon Necropolis", tapped = false)
                    // Sacrifice fodder + a creature on the opponent so the damage ability could
                    // otherwise be enumerable.
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val necropolis = game.findPermanent("Keldon Necropolis")!!

                withClue("Before Hamlet names it, Keldon Necropolis's non-mana damage ability is legal") {
                    game.activatableNonManaAbilityCount(necropolis) shouldBe 1
                }

                val hamletInHand = game.findCardsInHand(1, "Petrified Hamlet").first()
                game.execute(PlayLand(game.player1Id, hamletInHand))
                game.chooseCardNameOnEnter("Keldon Necropolis")

                withClue("After Hamlet names it, the non-mana damage ability can't be activated") {
                    game.activatableNonManaAbilityCount(necropolis) shouldBe 0
                }
                withClue("…but Keldon Necropolis's mana ability still works") {
                    game.activatableManaAbilityCount(necropolis) shouldNotBe 0
                }
            }

            test("naming an unrelated card leaves Keldon Necropolis's non-mana ability legal") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Petrified Hamlet")
                    .withCardOnBattlefield(1, "Keldon Necropolis", tapped = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val necropolis = game.findPermanent("Keldon Necropolis")!!

                val hamletInHand = game.findCardsInHand(1, "Petrified Hamlet").first()
                game.execute(PlayLand(game.player1Id, hamletInHand))
                game.chooseCardNameOnEnter("Plains")

                withClue("Naming Plains does not affect Keldon Necropolis — its damage ability stays legal") {
                    game.activatableNonManaAbilityCount(necropolis) shouldBe 1
                }
            }
        }

        context("Petrified Hamlet — GrantActivatedAbility ({T}: Add {C}) to lands with the chosen name") {

            test("naming a basic land grants every copy an extra {T}: Add {C} mana ability") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Petrified Hamlet")
                    .withCardOnBattlefield(1, "Plains", tapped = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val plains = game.findPermanent("Plains")!!

                withClue("A vanilla Plains has exactly one mana ability before the grant") {
                    game.activatableManaAbilityCount(plains) shouldBe 1
                }

                val hamletInHand = game.findCardsInHand(1, "Petrified Hamlet").first()
                game.execute(PlayLand(game.player1Id, hamletInHand))
                game.chooseCardNameOnEnter("Plains")

                withClue("The named Plains gains the granted {T}: Add {C} ability (now two mana abilities)") {
                    game.activatableManaAbilityCount(plains) shouldBe 2
                }
            }
        }

        context("Petrified Hamlet — its own intrinsic mana ability") {

            test("Petrified Hamlet itself can tap for {C}") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Petrified Hamlet", tapped = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hamlet = game.findPermanent("Petrified Hamlet")!!
                withClue("Petrified Hamlet has its own {T}: Add {C} mana ability") {
                    game.getLegalActions(1)
                        .mapNotNull { it.action as? ActivateAbility }
                        .any { it.sourceId == hamlet } shouldBe true
                }
                // (It made no name choice in this fixture, so the name-keyed statics are inert —
                // fail-closed — which is the intended behaviour.)
                hamlet shouldNotBe null
            }
        }
    }
}
