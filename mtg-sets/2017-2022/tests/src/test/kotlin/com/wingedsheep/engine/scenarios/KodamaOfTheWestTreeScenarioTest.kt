package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Kodama of the West Tree (NEO #199) — "Modified creatures you control have trample. Whenever a
 * modified creature you control deals combat damage to a player, search your library for a basic
 * land card, put it onto the battlefield tapped, then shuffle."
 *
 * Both halves key on [StatePredicate.IsModified]: the trample grant must follow modified-ness in
 * projection, and the combat-damage trigger must only fire for modified creatures that connect.
 */
class KodamaOfTheWestTreeScenarioTest : ScenarioTestBase() {

    init {
        context("Kodama of the West Tree") {

            test("only modified creatures get trample") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kodama of the West Tree")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val lions = game.findPermanent("Savannah Lions")!!
                game.state = game.state.updateEntity(bears) {
                    it.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
                }

                withClue("the countered Bears are modified and gain trample") {
                    game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
                }
                withClue("the unmodified Lions don't") {
                    game.state.projectedState.hasKeyword(lions, Keyword.TRAMPLE) shouldBe false
                }
            }

            test("a modified creature connecting fetches a tapped basic; an unmodified one does not") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kodama of the West Tree")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = game.state.updateEntity(bears) {
                    it.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2, "Savannah Lions" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()

                withClue("exactly one search — for the modified Bears only") {
                    (game.getPendingDecision() is SelectCardsDecision) shouldBe true
                }
                val forest = game.findCardsInLibrary(1, "Forest").single()
                game.selectCards(listOf(forest)).error shouldBe null
                game.resolveStack()

                withClue("no second search for the unmodified Lions") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("3 (Bears + counter) + 1 (Lions) damage") {
                    game.getLifeTotal(2) shouldBe 16
                }
                withClue("the Forest entered the battlefield tapped") {
                    val landed = game.findPermanent("Forest")!!
                    game.state.getEntity(landed)?.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
