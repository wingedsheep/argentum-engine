package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Rhet-Tomb Mystic (DRC #10) — {1}{U} Creature — Zombie Bird Wizard 2/1.
 *
 * "Flying. Each creature card in your hand has cycling {1}{U}."
 *
 * Covers `GrantCyclingToCardsInHand`: a creature card in the controller's hand cycles for {1}{U}
 * (paid, discarded, a card drawn); noncreature cards and the opponent's hand gain nothing; a card
 * with printed cycling keeps it and gets the granted cost as a second option; equal grants collapse
 * into one option; the grant counts as an activated ability of the card in hand; and it ends when
 * the Mystic leaves or loses its abilities.
 */
class RhetTombMysticScenarioTest : ScenarioTestBase() {

    private fun TestGame.cycleActions(cardId: EntityId) =
        getLegalActions(1).filter { (it.action as? CycleCard)?.cardId == cardId }

    init {
        context("granted cycling") {

            test("a creature card in your hand cycles for {1}{U}; a noncreature card does not") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Rhet-Tomb Mystic", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mystic = game.findPermanent("Rhet-Tomb Mystic")!!
                val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                val bolt = game.findCardsInHand(1, "Lightning Bolt").single()

                val offered = game.cycleActions(bears)
                withClue("the creature card gets exactly the granted cycling") {
                    offered.map { it.manaCostString } shouldBe listOf("{1}{U}")
                    (offered.single().action as CycleCard).grantedBy shouldBe mystic
                }
                withClue("an instant in hand is not a creature card") {
                    game.cycleActions(bolt).shouldBeEmpty()
                }

                game.execute(offered.single().action).error shouldBe null
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInHand(1, "Forest") shouldBe true
                withClue("both Islands paid the {1}{U}") {
                    game.state.getBattlefield().count { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe 2
                }
            }

            test("a card with printed cycling offers both costs, and either can be paid") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Rhet-Tomb Mystic", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(1, "Scion of Darkness")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mystic = game.findPermanent("Rhet-Tomb Mystic")!!
                val scion = game.findCardsInHand(1, "Scion of Darkness").single()
                val offered = game.cycleActions(scion)

                offered.map { it.manaCostString to (it.action as CycleCard).grantedBy } shouldContainExactlyInAnyOrder
                    listOf("{3}" to null, "{1}{U}" to mystic)
                withClue("two Islands can't pay the printed {3}, but can pay the granted {1}{U}") {
                    offered.single { (it.action as CycleCard).grantedBy == null }.isAffordable shouldBe false
                    game.execute(CycleCard(game.player1Id, scion)).error shouldNotBe null
                    game.execute(CycleCard(game.player1Id, scion, grantedBy = mystic)).error shouldBe null
                }
                game.isInGraveyard(1, "Scion of Darkness") shouldBe true
            }

            test("the opponent's creature cards don't gain cycling") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Rhet-Tomb Mystic", summoningSickness = false)
                    .withLandsOnBattlefield(2, "Island", 2)
                    .withCardInHand(2, "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mystic = game.findPermanent("Rhet-Tomb Mystic")!!
                val bears = game.findCardsInHand(2, "Grizzly Bears").single()
                game.getLegalActions(2).filter { it.action is CycleCard }.shouldBeEmpty()
                game.execute(CycleCard(game.player2Id, bears, grantedBy = mystic)).error shouldNotBe null
            }

            test("two Mystics grant the same cost once") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Rhet-Tomb Mystic", summoningSickness = false)
                    .withCardOnBattlefield(1, "Rhet-Tomb Mystic", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                game.cycleActions(bears).map { it.manaCostString } shouldBe listOf("{1}{U}")
            }

            test("a Mystic that has lost all abilities grants nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Rhet-Tomb Mystic", summoningSickness = false)
                    .withCardAttachedTo(2, "Lignify", "Rhet-Tomb Mystic")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                game.cycleActions(bears).shouldBeEmpty()
            }

            test("the granted cycling is an activated ability of the card in hand (CR 702.29a)") {
                fun hasActivatedAbility(withMystic: Boolean): Boolean {
                    val game = scenario()
                        .withPlayers("Player", "Opponent")
                        .apply { if (withMystic) withCardOnBattlefield(1, "Rhet-Tomb Mystic") }
                        .withCardInHand(1, "Grizzly Bears")
                        .withActivePlayer(1)
                        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                        .build()
                    val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                    return services.predicateEvaluator.matchesStatePredicate(
                        game.state, bears, StatePredicate.HasActivatedAbilities(1, false)
                    )
                }
                hasActivatedAbility(withMystic = true) shouldBe true
                hasActivatedAbility(withMystic = false) shouldBe false
            }

            test("the grant ends when Rhet-Tomb Mystic leaves the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Rhet-Tomb Mystic", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mystic = game.findPermanent("Rhet-Tomb Mystic")!!
                val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                game.castSpell(1, "Lightning Bolt", mystic).error shouldBe null
                game.resolveStack()

                game.findPermanent("Rhet-Tomb Mystic") shouldBe null
                game.cycleActions(bears).shouldBeEmpty()
                withClue("a stale grantedBy is refused") {
                    game.execute(CycleCard(game.player1Id, bears, grantedBy = mystic)).error shouldNotBe null
                }
            }
        }
    }
}
