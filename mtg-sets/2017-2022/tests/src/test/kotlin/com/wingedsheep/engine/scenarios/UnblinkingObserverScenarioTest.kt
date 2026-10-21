package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unblinking Observer (MID) — its {U} carries
 * `AnyOf(DisturbCostOnly, InstantOrSorceryOnly)`: it pays a disturb cast from the graveyard, but
 * not a creature spell cast from hand.
 */
class UnblinkingObserverScenarioTest : ScenarioTestBase() {

    init {
        context("Unblinking Observer") {

            test("its mana pays a disturb cost") {
                // Covetous Castaway's disturb is {3}{U}{U}: four Islands + the Observer's {U}.
                var b = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Unblinking Observer")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInGraveyard(1, "Covetous Castaway")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { b = b.withCardInLibrary(1, "Island") }
                repeat(5) { b = b.withCardInLibrary(2, "Island") }
                val game = b.build()

                val castaway = game.findCardsInGraveyard(1, "Covetous Castaway").single()
                withClue("the disturb cast is offered as affordable") {
                    game.getLegalActions(1).single {
                        it.actionType == "CastWithDisturb" && (it.action as CastSpell).cardId == castaway
                    }.isAffordable shouldBe true
                }

                val result = game.execute(
                    CastSpell(
                        game.player1Id, castaway,
                        useAlternativeCost = true,
                        alternativeCostType = AlternativeCostType.DISTURB,
                    )
                )
                withClue("error=${result.error}") { result.error shouldBe null }
                game.state.getEntity(game.findPermanent("Unblinking Observer")!!)?.get<TappedComponent>() shouldNotBe null
                game.state.stack.size shouldBe 1
            }

            test("its mana can't pay for a creature spell cast from hand") {
                // Covetous Castaway costs {1}{U} from hand: one Island + the Observer isn't enough.
                var b = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Unblinking Observer")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(1, "Covetous Castaway")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { b = b.withCardInLibrary(1, "Island") }
                repeat(5) { b = b.withCardInLibrary(2, "Island") }
                val game = b.build()

                game.castSpell(1, "Covetous Castaway").error shouldNotBe null
                game.isInHand(1, "Covetous Castaway") shouldBe true
            }
        }
    }
}
