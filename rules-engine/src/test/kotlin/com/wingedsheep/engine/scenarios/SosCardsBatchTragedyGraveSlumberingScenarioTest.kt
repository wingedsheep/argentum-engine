package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.LifeGainedAmountThisTurnComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for three Secrets of Strixhaven cards:
 *  - Tragedy Feaster: Trample + Ward—Discard a card + Infusion end-step "sacrifice a permanent
 *    unless you gained life this turn".
 *  - Grave Researcher // Reanimate: upkeep surveil + "become prepared if 3+ creature cards in your
 *    graveyard"; back-face Reanimate puts a creature from any graveyard onto the battlefield under
 *    your control and you lose life equal to its mana value.
 *  - Slumbering Trudge: {X}{G} 6/6 that enters with (3 − X) stun counters and enters tapped when X
 *    is 2 or less.
 */
class SosCardsBatchTragedyGraveSlumberingScenarioTest : ScenarioTestBase() {

    private fun stunCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.get<TappedComponent>() != null

    private fun TestGame.findExileCopy(playerNumber: Int, name: String): EntityId? {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).firstOrNull { id ->
            val e = state.getEntity(id)
            e?.get<CardComponent>()?.name == name && e.get<PreparedSpellCopyComponent>() != null
        }
    }

    /** Resolve surveil's keep/bin + top-card ordering decisions (keeping everything on top). */
    private fun resolveSurveil(game: TestGame) {
        var guard = 0
        while (guard++ < 6) {
            when (val pd = game.getPendingDecision()) {
                null -> return
                is ReorderLibraryDecision -> game.submitDecision(OrderedResponse(pd.id, pd.cards))
                else -> game.skipSelection()
            }
            game.resolveStack()
        }
    }

    init {
        context("Tragedy Feaster — Infusion end step (sacrifice unless you gained life)") {

            test("did NOT gain life this turn: must sacrifice a permanent of your choice") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tragedy Feaster", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Mountain") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Mountain") }
                val game = builder.build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("End-step trigger fires (no life gained) and prompts a sacrifice") {
                    game.getPendingDecision() shouldNotBe null
                }

                // Sacrifice the Forest (a permanent of the controller's choice).
                val forest = game.findPermanent("Forest")!!
                game.selectCards(listOf(forest))
                game.resolveStack()

                withClue("The chosen permanent is sacrificed to the graveyard") {
                    game.isInGraveyard(1, "Forest") shouldBe true
                }
                withClue("Tragedy Feaster itself can be kept (the Forest was sacrificed)") {
                    game.isOnBattlefield("Tragedy Feaster") shouldBe true
                }
            }

            test("gained life this turn: trigger does not fire, nothing is sacrificed") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tragedy Feaster", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Mountain") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Mountain") }
                val game = builder.build()

                // Record that the controller gained life this turn — the intervening-if fails,
                // so the trigger never goes on the stack.
                game.state = game.state.updateEntity(game.player1Id) {
                    it.withComponent(LifeGainedAmountThisTurnComponent(2))
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("No sacrifice prompt when you gained life this turn") {
                    game.getPendingDecision() shouldBe null
                }
                withClue("Both lands remain — nothing was sacrificed") {
                    game.isInGraveyard(1, "Forest") shouldBe false
                    game.isInGraveyard(1, "Swamp") shouldBe false
                }
            }
        }

        context("Slumbering Trudge — stun counters and tapped entry scale with X") {

            test("X = 0: enters with 3 stun counters and enters tapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Slumbering Trudge")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Slumbering Trudge", xValue = 0).error shouldBe null
                game.resolveStack()

                val trudge = game.findPermanent("Slumbering Trudge")!!
                withClue("3 − 0 = 3 stun counters") {
                    stunCounters(game, trudge) shouldBe 3
                }
                withClue("X is 2 or less → enters tapped") {
                    isTapped(game, trudge) shouldBe true
                }
            }

            test("X = 2: enters with 1 stun counter and enters tapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Slumbering Trudge")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Slumbering Trudge", xValue = 2).error shouldBe null
                game.resolveStack()

                val trudge = game.findPermanent("Slumbering Trudge")!!
                withClue("3 − 2 = 1 stun counter") {
                    stunCounters(game, trudge) shouldBe 1
                }
                withClue("X is 2 or less → enters tapped") {
                    isTapped(game, trudge) shouldBe true
                }
            }

            test("X = 3: enters with no stun counters and untapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Slumbering Trudge")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Slumbering Trudge", xValue = 3).error shouldBe null
                game.resolveStack()

                val trudge = game.findPermanent("Slumbering Trudge")!!
                withClue("3 − 3 = 0 stun counters") {
                    stunCounters(game, trudge) shouldBe 0
                }
                withClue("X is 3 (not 2 or less) → enters untapped") {
                    isTapped(game, trudge) shouldBe false
                }
            }
        }

        context("Grave Researcher — upkeep surveil + become prepared at 3+ creatures in GY") {

            test("becomes prepared when three or more creature cards are in your graveyard") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grave Researcher", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Savannah Lions")
                    .withCardInGraveyard(1, "Centaur Courser")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(6) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(6) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val researcher = game.findPermanent("Grave Researcher")!!

                // Advance to player 1's own upkeep so the "your upkeep" trigger fires.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                // Surveil 1 pauses for keep/bin (and a top-card ordering) decision — keep on top.
                resolveSurveil(game)

                withClue("With 3 creature cards in the graveyard, it becomes prepared") {
                    game.state.getEntity(researcher)?.get<PreparedComponent>() shouldNotBe null
                }
                withClue("A Reanimate prepare-spell copy now exists in exile") {
                    game.findExileCopy(1, "Grave Researcher") shouldNotBe null
                }
            }

            test("does NOT become prepared with fewer than three creature cards in graveyard") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grave Researcher", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Savannah Lions")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(6) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(6) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val researcher = game.findPermanent("Grave Researcher")!!

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
                resolveSurveil(game)

                withClue("Only 2 creature cards in graveyard → stays unprepared") {
                    game.state.getEntity(researcher)?.get<PreparedComponent>() shouldBe null
                }
                withClue("No prepare-spell copy should exist") {
                    game.findExileCopy(1, "Grave Researcher") shouldBe null
                }
            }
        }

        context("Grave Researcher — Reanimate (back face)") {

            test("casting Reanimate puts a creature from any graveyard under your control and loses life = its MV") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grave Researcher", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withLifeTotal(1, 20)
                    // Three creature cards in YOUR graveyard so the upkeep trigger prepares it.
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Savannah Lions")
                    .withCardInGraveyard(1, "Goblin Guide")
                    // Centaur Courser is a {2}{G} 3/3 (mana value 3) in the OPPONENT's graveyard.
                    .withCardInGraveyard(2, "Centaur Courser")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(8) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(8) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                // Advance to player 1's upkeep so it becomes prepared.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
                resolveSurveil(game)

                // Reach player 1's precombat main (sorcery-speed, empty stack) to cast the copy.
                var guard = 0
                while (!(game.state.activePlayerId == game.player1Id &&
                        game.state.phase == Phase.PRECOMBAT_MAIN) && guard++ < 6
                ) {
                    game.passUntilPhase(Phase.ENDING, Step.END)
                    game.resolveStack()
                    game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    game.resolveStack()
                }

                val copyId = game.findExileCopy(1, "Grave Researcher")!!
                val courser = game.state.getGraveyard(game.player2Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Centaur Courser"
                }
                val lifeBefore = game.getLifeTotal(1)

                game.execute(
                    CastSpell(
                        game.player1Id,
                        copyId,
                        targets = listOf(ChosenTarget.Card(courser, game.player2Id, Zone.GRAVEYARD)),
                        faceIndex = 0,
                    )
                )
                game.resolveStack()

                withClue("Centaur Courser enters the battlefield under player 1's control") {
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                }
                withClue("You lose life equal to Centaur Courser's mana value (3)") {
                    game.getLifeTotal(1) shouldBe lifeBefore - 3
                }
            }
        }
    }
}
