package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.arn.cards.Sindbad
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Sindbad (ARN) — "{T}: Draw a card and reveal it. If it isn't a
 * land card, discard it."
 *
 * Sindbad is authored as a pure pipeline composition (peek top → draw → InZone(HAND)
 * re-check → reveal → partition by Land → discard the nonland remainder), replacing the
 * former bespoke DrawRevealDiscardUnlessEffect. These tests pin the printed behaviour:
 * a real draw (draw triggers see it), a public reveal either way, and a discard (not a
 * bare zone move) for the nonland case.
 */
class SindbadScenarioTest : ScenarioTestBase() {

    private val abilityId = Sindbad.activatedAbilities.first().id

    init {
        context("Sindbad — draw, reveal, discard unless land") {

            test("a drawn land is revealed and kept in hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Sindbad", summoningSickness = false)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sindbadId = game.findPermanent("Sindbad")!!
                val activation = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = sindbadId, abilityId = abilityId)
                )
                withClue("Activating Sindbad should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                val results = game.resolveStack()
                val events = results.flatMap { it.events }

                withClue("the land should be drawn and kept in hand") {
                    game.isInHand(1, "Island") shouldBe true
                }
                withClue("the draw must be a real draw (CardsDrawnEvent for draw triggers)") {
                    events.filterIsInstance<CardsDrawnEvent>().isNotEmpty() shouldBe true
                }
                withClue("the drawn card must be revealed even when kept") {
                    events.filterIsInstance<CardsRevealedEvent>()
                        .any { it.cardNames.contains("Island") } shouldBe true
                }
                withClue("a land is not discarded") {
                    game.isInGraveyard(1, "Island") shouldBe false
                    events.filterIsInstance<CardsDiscardedEvent>().isEmpty() shouldBe true
                }
            }

            test("a drawn nonland is revealed and discarded (as a discard, not a bare move)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Sindbad", summoningSickness = false)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sindbadId = game.findPermanent("Sindbad")!!
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = sindbadId, abilityId = abilityId)
                ).error shouldBe null
                val results = game.resolveStack()
                val events = results.flatMap { it.events }

                withClue("the nonland should end in the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInHand(1, "Grizzly Bears") shouldBe false
                }
                withClue("the card must be revealed") {
                    events.filterIsInstance<CardsRevealedEvent>()
                        .any { it.cardNames.contains("Grizzly Bears") } shouldBe true
                }
                withClue("the move must be a discard so discard triggers can see it") {
                    events.filterIsInstance<CardsDiscardedEvent>()
                        .any { it.cardIds.isNotEmpty() } shouldBe true
                }
            }

            test("empty library: nothing is revealed or discarded") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Sindbad", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sindbadId = game.findPermanent("Sindbad")!!
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = sindbadId, abilityId = abilityId)
                ).error shouldBe null
                val results = game.resolveStack()
                val events = results.flatMap { it.events }

                withClue("no reveal or discard may fire when no card was drawn") {
                    events.filterIsInstance<CardsRevealedEvent>().isEmpty() shouldBe true
                    events.filterIsInstance<CardsDiscardedEvent>().isEmpty() shouldBe true
                }
            }
        }
    }
}
