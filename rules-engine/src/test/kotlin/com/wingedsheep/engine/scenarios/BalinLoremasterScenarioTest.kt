package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.mechanics.enduringstory.EnduringStoryService
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Balin, Loremaster — {3}{R}{R} Legendary Creature — Dwarf Bard 4/4.
 *
 *   Storied.
 *   Whenever Balin or another Dwarf you control enters, you may discard your hand. Draw X cards,
 *   where X is the number of cards discarded this way. If you have an enduring story, Balin deals
 *   X damage to each opponent.
 *
 * X is "discarded **this way**", so it is the size of the collection the discard actually moved —
 * `discardedHand_count`, published by the `GatherCardsEffect` inside `Patterns.Hand.discardHand`.
 * The fragile part is that the "may" pauses for a yes/no *between* the gather and the read: the
 * pipeline storage has to survive that continuation round-trip, or the draw silently reads 0
 * (an unset `VariableReference` evaluates to 0, so nothing errors). Both branches are exercised
 * below, and the decline branch pins that declining reads X = 0 rather than skipping the rest of
 * the ability.
 */
class BalinLoremasterScenarioTest : ScenarioTestBase() {

    init {

        test("discarding your hand draws that many, and deals no damage without an enduring story") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Balin, Loremaster")
                .withCardInHand(1, "Dwarven Mauler")
                .withCardsInHand(1, "Grizzly Bears", 2)
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("Balin alone is one legendary permanent — no enduring story") {
                EnduringStoryService.has(game.state, game.player1Id) shouldBe false
            }

            game.castSpell(1, "Dwarven Mauler").error shouldBe null
            game.resolveStack()

            (game.getPendingDecision() is YesNoDecision) shouldBe true
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            withClue("the two Grizzly Bears left in hand were discarded") {
                game.graveyardSize(1) shouldBe 2
            }
            withClue("X = 2 cards discarded this way, so two cards were drawn") {
                game.handSize(1) shouldBe 2
                game.librarySize(1) shouldBe 1
            }
            withClue("no enduring story — the damage clause is skipped entirely") {
                game.getLifeTotal(2) shouldBe 20
            }
        }

        test("declining the discard reads X = 0 — no draw, no damage, hand intact") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Balin, Loremaster")
                .withCardOnBattlefield(1, "Óin the Brave")
                .withCardOnBattlefield(1, "Thorin Oakenshield")
                .withCardInHand(1, "Dwarven Mauler")
                .withCardsInHand(1, "Grizzly Bears", 2)
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            EnduringStoryService.has(game.state, game.player1Id) shouldBe true

            game.castSpell(1, "Dwarven Mauler").error shouldBe null
            game.resolveStack()

            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            withClue("nothing was discarded, so X is 0 — not 'the ability was skipped'") {
                game.graveyardSize(1) shouldBe 0
                game.handSize(1) shouldBe 2
                game.librarySize(1) shouldBe 2
            }
            withClue("the story is on but X = 0, so Balin deals no damage") {
                game.getLifeTotal(2) shouldBe 20
            }
        }

        test("with an enduring story Balin also burns each opponent for X") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Balin, Loremaster")
                .withCardOnBattlefield(1, "Óin the Brave")
                .withCardOnBattlefield(1, "Thorin Oakenshield")
                .withCardInHand(1, "Dwarven Mauler")
                .withCardsInHand(1, "Grizzly Bears", 2)
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            EnduringStoryService.has(game.state, game.player1Id) shouldBe true

            game.castSpell(1, "Dwarven Mauler").error shouldBe null
            game.resolveStack()

            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.graveyardSize(1) shouldBe 2
            game.handSize(1) shouldBe 2
            withClue("the same X that drove the draw drives the damage — two discarded, two damage") {
                game.getLifeTotal(2) shouldBe 18
            }
            withClue("'each opponent', not 'each player' — Balin's controller is untouched") {
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
