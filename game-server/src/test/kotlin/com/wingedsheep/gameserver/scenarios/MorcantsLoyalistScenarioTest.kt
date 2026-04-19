package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Morcant's Loyalist's dies trigger.
 *
 * Card reference:
 *   Morcant's Loyalist ({1}{B}{G}): 3/2 Creature — Elf Warrior
 *     Other Elves you control get +1/+1.
 *     When this creature dies, return another target Elf card from your graveyard to your hand.
 *
 * The returned Elf is auto-revealed via a `CardsRevealedEvent` carrying `fromZone`/`toZone`
 * context — the UI can then label the overlay with the zone transition (e.g.,
 * "Opponent Returned to hand from graveyard — Morcant's Loyalist").
 */
class MorcantsLoyalistScenarioTest : ScenarioTestBase() {

    init {
        context("Morcant's Loyalist dies trigger") {

            test("returns target Elf from graveyard to hand and reveals it to opponent") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Morcant's Loyalist")
                    .withCardInHand(1, "Nameless Inversion")  // +3/-3 — kills the 3/2 Loyalist
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInGraveyard(1, "Wirewood Herald")  // Elf card to return
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val loyaltyId = game.findPermanent("Morcant's Loyalist")!!

                // Player 1 casts Nameless Inversion on their own Loyalist (suicide).
                val castResult = game.castSpell(1, "Nameless Inversion", loyaltyId)
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve Nameless Inversion → Loyalist becomes 6/-1 → dies in SBA
                // → dies trigger goes on stack and pauses for target selection.
                game.resolveStack()

                withClue("Loyalist's dies trigger should be pending target selection") {
                    game.hasPendingDecision() shouldBe true
                }
                withClue("Loyalist should already be in graveyard (SBA)") {
                    game.isInGraveyard(1, "Morcant's Loyalist") shouldBe true
                }

                // Select Wirewood Herald (the other Elf card in graveyard) as the target.
                val heraldInGy = game.findCardsInGraveyard(1, "Wirewood Herald")
                withClue("Wirewood Herald should be in graveyard") {
                    heraldInGy.shouldNotBeNull()
                    heraldInGy.size shouldBe 1
                }

                val selectResult = game.selectTargets(heraldInGy)
                withClue("Target selection should succeed: ${selectResult.error}") {
                    selectResult.error shouldBe null
                }

                // Resolve the triggered ability on the stack.
                val resolveResults = game.resolveStack()

                // Collect every event emitted across the whole resolution chain
                // (selectTargets + the subsequent stack resolution passes).
                val allEvents = selectResult.events + resolveResults.flatMap { it.events }

                val revealEvent = allEvents.filterIsInstance<CardsRevealedEvent>().firstOrNull()
                withClue("A CardsRevealedEvent should fire when the Elf is returned") {
                    revealEvent.shouldNotBeNull()
                }
                withClue("Reveal should name Morcant's Loyalist as the source") {
                    revealEvent!!.source shouldBe "Morcant's Loyalist"
                }
                withClue("Revealed card list should contain Wirewood Herald") {
                    revealEvent!!.cardNames shouldContain "Wirewood Herald"
                }
                withClue("Reveal should be broadcast by the Loyalist's controller (Alice)") {
                    revealEvent!!.revealingPlayerId shouldBe game.player1Id
                }
                withClue("Reveal should carry graveyard → hand zone context for the UI label") {
                    revealEvent!!.fromZone shouldBe Zone.GRAVEYARD
                    revealEvent!!.toZone shouldBe Zone.HAND
                }

                withClue("Wirewood Herald should now be in hand") {
                    game.isInHand(1, "Wirewood Herald") shouldBe true
                }
                withClue("Wirewood Herald should no longer be in graveyard") {
                    game.isInGraveyard(1, "Wirewood Herald") shouldBe false
                }
            }
        }
    }
}
