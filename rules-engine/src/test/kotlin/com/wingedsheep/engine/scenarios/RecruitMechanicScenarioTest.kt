package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for the Recruit keyword action (The Hobbit) as a pure pipeline composition —
 * `Patterns.Mechanic.recruit()`.
 *
 * Recruit: "Draw a card, then discard a card. If you discarded a nonland card, create a 1/1 white
 * Human Soldier creature token."
 *
 * The composition is Draw → Gather (hand) → Select 1 → FilterCollection partition by Land →
 * MoveCollection(→ graveyard, [MoveType.Discard]) → GatedEffect over the *nonland* partition. No
 * bespoke Recruit effect type exists.
 *
 * The mechanic-level edge cases live here (one file per mechanic, per AGENTS.md); each of the five
 * HOB recruit cards has its own scenario test for its own trigger wiring.
 */
class RecruitMechanicScenarioTest : ScenarioTestBase() {

    private val recruiter = card("Test Recruiter") {
        manaCost = "{W}"
        typeLine = "Creature — Human Soldier"
        power = 1
        toughness = 1
        oracleText = "When this creature enters, recruit."

        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            effect = Patterns.Mechanic.recruit()
        }
    }

    init {
        cardRegistry.register(recruiter)

        // CreateTokenExecutor derives the name as "<types joined> Token" — not the bare type line.
        fun TestGame.soldierTokens(): Int = findAllPermanents("Human Soldier Token").size

        context("Recruit as a pipeline composition") {

            test("discarding a nonland card mints a 1/1 Human Soldier token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Test Recruiter")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Recruiter").error shouldBe null
                game.resolveStack()

                withClue("recruit should pause to choose the card to discard") {
                    game.hasPendingDecision() shouldBe true
                }
                val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                game.selectCards(listOf(bears))
                game.resolveStack()

                withClue("the drawn Forest should be in hand (draw happens before the discard)") {
                    game.isInHand(1, "Forest") shouldBe true
                }
                withClue("the chosen nonland card should be in the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("discarding a nonland card creates exactly one Soldier token") {
                    game.soldierTokens() shouldBe 1
                }
            }

            test("discarding a land card creates no token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Test Recruiter")
                    .withCardInHand(1, "Mountain")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Recruiter").error shouldBe null
                game.resolveStack()

                val mountain = game.findCardsInHand(1, "Mountain").single()
                game.selectCards(listOf(mountain))
                game.resolveStack()

                withClue("the discarded land should be in the graveyard") {
                    game.isInGraveyard(1, "Mountain") shouldBe true
                }
                withClue("'if you discarded a nonland card' is false for a land — no token") {
                    game.soldierTokens() shouldBe 0
                }
            }

            test("the discard is a real discard, not a bare move to the graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Test Recruiter")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Recruiter").error shouldBe null
                game.resolveStack()

                val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                val results = buildList {
                    add(game.selectCards(listOf(bears)))
                    addAll(game.resolveStack())
                }

                withClue("madness and 'whenever you discard' triggers need a CardsDiscardedEvent") {
                    results.flatMap { it.events }
                        .filterIsInstance<CardsDiscardedEvent>()
                        .any { bears in it.cardIds } shouldBe true
                }
            }

            test("empty library and empty hand: nothing is discarded, so no token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Test Recruiter")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Recruiter").error shouldBe null
                game.resolveStack()

                withClue("nothing to draw and nothing to discard — no discard decision") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("no card was discarded, so 'you discarded a nonland card' is false") {
                    game.soldierTokens() shouldBe 0
                }
            }
        }
    }
}
