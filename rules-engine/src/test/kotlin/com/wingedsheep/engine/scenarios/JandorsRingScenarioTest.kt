package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Jandor's Ring (ARN #64) — {6} Artifact.
 *
 * "{2}, {T}, Discard the last card you drew this turn: Draw a card."
 *
 * Exercises the new [com.wingedsheep.sdk.scripting.AbilityCost.DiscardLastDrawnThisTurn]
 * SDK cost and the per-player `GameState.lastCardDrawnThisTurnByPlayer` tracker the cost
 * reads from. Covers the happy path (draw → activate → discard tracked card, draw fresh
 * card), both unpayable cases from the Scryfall rulings ("haven't drawn this turn" and
 * "the drawn card is no longer in your hand"), the multi-card-draw rule ("if you draw
 * more than one card, you must discard the last one of those drawn"), and the turn
 * boundary (tracker resets at start of next turn).
 */
class JandorsRingScenarioTest : ScenarioTestBase() {

    private val ringAbilityId =
        cardRegistry.getCard("Jandor's Ring")!!.activatedAbilities.first().id

    // Inline draw-2 sorcery used to drive a single CardsDrawnEvent of size 2, so we can
    // assert the tracker records the *last* of the batch (Scryfall's multi-card rule).
    private val drawTwoTest = card("Draw Two Test") {
        manaCost = "{2}"
        typeLine = "Sorcery"
        spell { effect = Effects.DrawCards(2) }
    }

    init {
        cardRegistry.register(drawTwoTest)

        context("Jandor's Ring") {

            test("happy path: discards the tracked drawn card and draws a fresh one") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jandor's Ring", summoningSickness = false)
                    .withCardInHand(1, "Mountain")           // pretend this was just drawn
                    .withCardInHand(1, "Forest")             // pretend this was drawn earlier
                    .withLandsOnBattlefield(1, "Plains", 2)  // pay {2}
                    .withCardInLibrary(1, "Island")          // top of library (drawn next)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tracked = game.findCardsInHand(1, "Mountain").single()
                game.state = game.state.copy(
                    lastCardDrawnThisTurnByPlayer = mapOf(game.player1Id to tracked)
                )
                val ring = game.findPermanent("Jandor's Ring")!!

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ring,
                        abilityId = ringAbilityId,
                    )
                )
                withClue("Activating Jandor's Ring should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("Tracked card (Mountain) moves from hand to graveyard as the discard cost") {
                    game.isInHand(1, "Mountain") shouldBe false
                    game.isInGraveyard(1, "Mountain") shouldBe true
                }
                withClue("Earlier-drawn cards in hand are not discarded") {
                    game.isInHand(1, "Forest") shouldBe true
                }
                withClue("The replacement draw pulls the top of the library (Island)") {
                    game.isInHand(1, "Island") shouldBe true
                }
                withClue(
                    "After the draw resolved, the tracker now points at the freshly drawn " +
                        "Island — a subsequent activation would target it."
                ) {
                    val island = game.findCardsInHand(1, "Island").single()
                    game.state.lastCardDrawnThisTurnByPlayer[game.player1Id] shouldBe island
                }
            }

            test("ability is not legal when controller has not drawn a card this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jandor's Ring", summoningSickness = false)
                    .withCardInHand(1, "Mountain")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // No entry in lastCardDrawnThisTurnByPlayer for the controller.
                game.state.lastCardDrawnThisTurnByPlayer.containsKey(game.player1Id) shouldBe false

                val ring = game.findPermanent("Jandor's Ring")!!
                val ringActivations = game.getLegalActions(1).filter { info ->
                    (info.action as? ActivateAbility)?.sourceId == ring &&
                        info.action.abilityId == ringAbilityId
                }
                // The enumerator still emits the ability as a greyed-out unaffordable entry so
                // the UI can show "you can't activate this yet" — the ability is not omitted
                // entirely, it is offered as a single unaffordable action.
                withClue("With no card drawn this turn, the ring is offered only as a greyed-out unaffordable action") {
                    ringActivations.map { it.isAffordable } shouldBe listOf(false)
                }
            }

            test("ability is not legal when the tracked card has left the hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jandor's Ring", summoningSickness = false)
                    .withCardInGraveyard(1, "Mountain") // tracked card is in graveyard, not hand
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val stale = game.findCardsInGraveyard(1, "Mountain").single()
                game.state = game.state.copy(
                    lastCardDrawnThisTurnByPlayer = mapOf(game.player1Id to stale)
                )

                val ring = game.findPermanent("Jandor's Ring")!!
                val ringActivations = game.getLegalActions(1).filter { info ->
                    (info.action as? ActivateAbility)?.sourceId == ring &&
                        info.action.abilityId == ringAbilityId
                }
                withClue(
                    "Per Scryfall ruling: 'If you do not have the card still in your hand, " +
                        "you can't pay the cost.' — the ring stays enumerated but only as a " +
                        "greyed-out unaffordable action."
                ) {
                    ringActivations.map { it.isAffordable } shouldBe listOf(false)
                }
            }

            test("draw step populates the tracker with the drawn entity id") {
                // The simpler half of the rulings: the draw step's CardsDrawnEvent updates the
                // tracker, which is what an activated DiscardLastDrawnThisTurn cost reads. Using
                // turn 2 because the first player skips their turn-1 draw (CR 103.8a).
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jandor's Ring", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .withTurnNumber(2)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                // Stack the library so we know exactly which entity will be drawn.
                builder = builder.withCardInLibrary(1, "Island")
                repeat(3) { builder = builder.withCardInLibrary(1, "Mountain") }
                val game = builder.build()

                game.state.lastCardDrawnThisTurnByPlayer[game.player1Id] shouldBe null

                // Advance through the draw step. After the draw step the player has drawn one
                // card from the top of the library; passUntilPhase stops at the precombat main
                // priority window with the draw having happened.
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                val drawnEntity = game.findCardsInHand(1, "Island").firstOrNull()
                    ?: game.findCardsInHand(1, "Mountain").firstOrNull()
                withClue("The draw step actually moved a card into the player's hand") {
                    drawnEntity shouldNotBe null
                }
                withClue("The tracker is populated by the draw step's CardsDrawnEvent") {
                    game.state.lastCardDrawnThisTurnByPlayer[game.player1Id] shouldBe drawnEntity
                }
            }

            test("multi-card draw points the tracker at the LAST card drawn, and the ring discards that one") {
                // Per the Jandor's Ring Scryfall ruling: "If you draw more than one card due to
                // a spell or ability, you must discard the last one of those drawn." Cast a
                // draw-2 sorcery with a stacked library, then assert the tracker holds the
                // *second*-drawn id (not the first), and that activating the ring discards that
                // second card — leaving the first-drawn one untouched in hand.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jandor's Ring", summoningSickness = false)
                    .withCardInHand(1, "Draw Two Test")
                    .withLandsOnBattlefield(1, "Plains", 4) // {2} for the spell + {2} for the ring
                    .withCardInLibrary(1, "Mountain") // drawn first by the draw-2 spell
                    .withCardInLibrary(1, "Forest")   // drawn second — should become the tracked card
                    .withCardInLibrary(1, "Island")   // drawn by the ring's replacement draw
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Draw Two Test")
                game.resolveStack()

                val forest = game.findCardsInHand(1, "Forest").single()
                withClue(
                    "Tracker holds the LAST of the draw-2 batch (Forest, drawn 2nd), not the " +
                        "first (Mountain). Per Scryfall: 'you must discard the last one of those drawn.'"
                ) {
                    game.state.lastCardDrawnThisTurnByPlayer[game.player1Id] shouldBe forest
                }

                val ring = game.findPermanent("Jandor's Ring")!!
                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ring,
                        abilityId = ringAbilityId,
                    )
                )
                withClue("Activating Jandor's Ring should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("Forest (the LAST of the draw-2) is discarded as the cost") {
                    game.isInGraveyard(1, "Forest") shouldBe true
                    game.isInHand(1, "Forest") shouldBe false
                }
                withClue("Mountain (drawn first, but NOT 'last') stays in hand") {
                    game.isInHand(1, "Mountain") shouldBe true
                }
                withClue("The ring's replacement draw pulls Island from the top of the library") {
                    game.isInHand(1, "Island") shouldBe true
                }
            }

            test("turn boundary clears the tracker so the ability is not legal at the start of next turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jandor's Ring", summoningSickness = false)
                    .withCardInHand(1, "Mountain")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(2, "Forest") // opponent needs to be able to draw on their turn
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tracked = game.findCardsInHand(1, "Mountain").single()
                game.state = game.state.copy(
                    lastCardDrawnThisTurnByPlayer = mapOf(game.player1Id to tracked)
                )

                // We're already in PRECOMBAT_MAIN, so step out of it first (passUntilPhase exits
                // immediately if the target is the current state). Walk through end step, then
                // continue to opponent's main — TurnManager fires at the turn boundary in between
                // and must clear the per-player tracker map.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                withClue(
                    "The active player flipped to the opponent — confirms we actually crossed " +
                        "a turn boundary, not just a step boundary on the same turn."
                ) {
                    game.state.activePlayerId shouldBe game.player2Id
                }
                withClue(
                    "Player 1's stale entry from the prior turn must be cleared. The opponent " +
                        "will have its own entry from its draw step on this new turn."
                ) {
                    game.state.lastCardDrawnThisTurnByPlayer[game.player1Id] shouldBe null
                }
                withClue("Mountain is still in player 1's hand — it was never discarded") {
                    val handZone = com.wingedsheep.engine.state.ZoneKey(game.player1Id, Zone.HAND)
                    (tracked in game.state.getZone(handZone)) shouldBe true
                }
            }
        }
    }
}
