package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.chosenCardName
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Sorcerous Spyglass (canonical XLN #248; reprinted LCI #261).
 *
 * Sorcerous Spyglass ({2}, Artifact):
 *   As this artifact enters, look at an opponent's hand, then choose any card name.
 *   Activated abilities of sources with the chosen name can't be activated unless they're mana
 *     abilities.
 *
 * "As this artifact enters, look at an opponent's hand, then choose any card name" is a single
 * as-enters replacement modeled as EntersWithChoice(ChoiceType.CARD_NAME, cardNamePool = ANY,
 * lookAtOpponentHand = true): the ANY pool offers every registered card name (not just lands), and
 * the look reveals the opponent's hand to the controller before the choice. The pick is stored
 * durably under ChoiceSlot.CARD_NAME and read by the name-keyed static
 * PreventActivatedAbilities(nonManaAbilitiesOnly = true) via .namedFromChosenComponent().
 */
class SorcerousSpyglassScenarioTest : ScenarioTestBase() {

    private fun TestGame.castSpyglassToChoice(): ChooseOptionDecision {
        castSpell(1, "Sorcerous Spyglass")
        resolveStack()
        val decision = getPendingDecision()
        withClue("Sorcerous Spyglass must present an as-enters card-name choice on resolution") {
            (decision is ChooseOptionDecision) shouldBe true
        }
        return decision as ChooseOptionDecision
    }

    private fun TestGame.chooseName(decision: ChooseOptionDecision, name: String) {
        withClue("The chosen name must be offered by the pool") {
            decision.options shouldContain name
        }
        submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(name)))
    }

    private fun TestGame.activatableNonManaAbilityCount(sourceId: EntityId): Int =
        getLegalActions(1)
            .filter { (it.action as? ActivateAbility)?.sourceId == sourceId }
            .count { !it.isManaAbility }

    private fun TestGame.activatableManaAbilityCount(sourceId: EntityId): Int =
        getLegalActions(1)
            .filter { (it.action as? ActivateAbility)?.sourceId == sourceId }
            .count { it.isManaAbility }

    init {
        context("Sorcerous Spyglass — as-enters look + choose any card name") {

            test("offers ANY card name (including nonlands), looks at opponent's hand, stores the pick") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sorcerous Spyglass")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInHand(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val decision = game.castSpyglassToChoice()

                withClue("The ANY pool offers a nonland creature name — the land-only pool would not") {
                    decision.options shouldContain "Grizzly Bears"
                }

                val oppCard = game.findCardsInHand(2, "Grizzly Bears").first()
                withClue("Looking at the opponent's hand reveals it to Spyglass's controller") {
                    game.state.getEntity(oppCard)?.get<RevealedToComponent>()
                        ?.isRevealedTo(game.player1Id) shouldBe true
                }

                game.chooseName(decision, "Grizzly Bears")

                val spyglass = game.findPermanent("Sorcerous Spyglass")!!
                withClue("The chosen name is stored durably under ChoiceSlot.CARD_NAME") {
                    game.state.getEntity(spyglass)?.chosenCardName() shouldBe "Grizzly Bears"
                }
            }

            test("empty opponent hand still lets you choose any name") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sorcerous Spyglass")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val decision = game.castSpyglassToChoice()
                withClue("Even with no cards to see, any card name may still be named") {
                    decision.options shouldContain "Grizzly Bears"
                }
                game.chooseName(decision, "Grizzly Bears")
                game.findPermanent("Sorcerous Spyglass") shouldNotBe null
            }
        }

        context("Sorcerous Spyglass — PreventActivatedAbilities (non-mana only) on the named source") {

            test("naming a source shuts off its non-mana ability but leaves its mana ability legal") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sorcerous Spyglass")
                    .withCardOnBattlefield(1, "Keldon Necropolis", tapped = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 10)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val necropolis = game.findPermanent("Keldon Necropolis")!!

                withClue("Before Spyglass names it, Keldon Necropolis's non-mana ability is legal") {
                    game.activatableNonManaAbilityCount(necropolis) shouldBe 1
                }

                val decision = game.castSpyglassToChoice()
                game.chooseName(decision, "Keldon Necropolis")

                withClue("After Spyglass names it, the non-mana damage ability can't be activated") {
                    game.activatableNonManaAbilityCount(necropolis) shouldBe 0
                }
                withClue("…but Keldon Necropolis's mana ability still works") {
                    game.activatableManaAbilityCount(necropolis) shouldNotBe 0
                }
            }

            test("naming an unrelated card leaves Keldon Necropolis's non-mana ability legal") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sorcerous Spyglass")
                    .withCardOnBattlefield(1, "Keldon Necropolis", tapped = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 10)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val necropolis = game.findPermanent("Keldon Necropolis")!!

                val decision = game.castSpyglassToChoice()
                game.chooseName(decision, "Grizzly Bears")

                withClue("Naming an unrelated card does not lock Keldon Necropolis") {
                    game.activatableNonManaAbilityCount(necropolis) shouldBe 1
                }
            }
        }
    }
}
