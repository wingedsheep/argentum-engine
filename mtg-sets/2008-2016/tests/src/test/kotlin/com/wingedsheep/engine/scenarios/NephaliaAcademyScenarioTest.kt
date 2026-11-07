package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Nephalia Academy (EMN #205) — Land.
 *
 *   If a spell or ability an opponent controls causes you to discard a card, you may reveal that
 *   card and put it on top of your library instead of putting it anywhere else.
 *   {T}: Add {C}.
 *
 * Pins the cause scope (an opponent's Mind Rot is offered the choice, your own is not), the reveal,
 * the per-card choice, and that the card still counts as discarded (rulings 2016-07-13).
 */
class NephaliaAcademyScenarioTest : ScenarioTestBase() {

    init {
        context("Nephalia Academy") {

            fun board(caster: Int) = scenario()
                .withPlayers("Academy", "Opponent")
                .withCardOnBattlefield(1, "Nephalia Academy")
                .withCardInHand(caster, "Mind Rot")
                .withLandsOnBattlefield(caster, "Swamp", 3)
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(caster)
                .withPriorityPlayer(caster)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun TestGame.topOfLibrary(player: Int): String? =
                state.getLibrary(if (player == 1) player1Id else player2Id).firstOrNull()
                    ?.let { state.getEntity(it)?.get<CardComponent>()?.name }

            /** Answers every destination question with [revealFor]'s cards going to the library. */
            fun TestGame.resolveMindRot(revealFor: Set<String>, libraryOrder: List<String> = emptyList()): List<GameEvent> {
                val events = resolveStack().flatMap { it.events }.toMutableList()
                var guard = 0
                while (state.pendingDecision != null && guard++ < 10) {
                    val result = when (val decision = state.pendingDecision) {
                        is ChooseOptionDecision -> {
                            decision.playerId shouldBe player1Id
                            val reveal = revealFor.any { decision.prompt.contains(it) }
                            if (reveal) decision.options[0] shouldContain "Reveal"
                            submitDecision(OptionChosenResponse(decision.id, if (reveal) 0 else decision.options.lastIndex))
                        }
                        is SelectCardsDecision -> {
                            val byName = decision.options.associateBy { state.getEntity(it)?.get<CardComponent>()?.name }
                            val picked = if (decision.ordered) libraryOrder.map { byName.getValue(it) }
                                else decision.options.take(decision.minSelections)
                            submitDecision(CardsSelectedResponse(decision.id, picked))
                        }
                        else -> error("Unexpected decision $decision")
                    }
                    withClue("decision answer should be accepted: ${result.error}") { result.error shouldBe null }
                    events += result.events
                    events += resolveStack().flatMap { it.events }
                }
                return events
            }

            test("an opponent's discard spell lets you reveal the card and put it on top of your library") {
                val game = board(caster = 2)
                game.castSpellTargetingPlayer(2, "Mind Rot", 1).error shouldBe null
                val events = game.resolveMindRot(revealFor = setOf("Grizzly Bears"))

                game.topOfLibrary(1) shouldBe "Grizzly Bears"
                game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                game.handSize(1) shouldBe 0

                val revealed = events.filterIsInstance<CardsRevealedEvent>()
                withClue("the Academy card is revealed to the other players, naming the Academy") {
                    revealed shouldHaveSize 1
                    revealed.single().cardNames shouldBe listOf("Grizzly Bears")
                    revealed.single().revealingPlayerId shouldBe game.player1Id
                    revealed.single().source shouldBe "Nephalia Academy"
                    revealed.single().revealToSelf shouldBe false
                }
                val discarded = events.filterIsInstance<CardsDiscardedEvent>().single()
                withClue("both cards were still discarded, and the revealed one stays a known card") {
                    discarded.cardIds shouldHaveSize 2
                    discarded.undefinedCharacteristics shouldBe emptySet()
                }
            }

            test("two cards revealed onto the library are ordered by their owner") {
                val game = board(caster = 2)
                game.castSpellTargetingPlayer(2, "Mind Rot", 1).error shouldBe null
                val events = game.resolveMindRot(
                    revealFor = setOf("Grizzly Bears", "Lightning Bolt"),
                    libraryOrder = listOf("Lightning Bolt", "Grizzly Bears"),
                )
                withClue("both cards are shown in one reveal") {
                    events.filterIsInstance<CardsRevealedEvent>().single().cardIds shouldHaveSize 2
                }
                game.graveyardSize(1) shouldBe 0
                val library = game.state.getLibrary(game.player1Id)
                    .map { game.state.getEntity(it)?.get<CardComponent>()?.name }
                library.take(2) shouldBe listOf("Lightning Bolt", "Grizzly Bears")
            }

            test("a discard you cause yourself is not offered the replacement") {
                val game = board(caster = 1)
                game.castSpellTargetingPlayer(1, "Mind Rot", 1).error shouldBe null
                game.resolveStack()
                withClue("no destination question for your own spell") {
                    (game.state.pendingDecision is ChooseOptionDecision) shouldBe false
                }
                game.resolveMindRot(revealFor = emptySet())
                listOf("Grizzly Bears", "Lightning Bolt").forEach { game.isInGraveyard(1, it) shouldBe true }
                game.topOfLibrary(1) shouldBe "Forest"
            }

            test("declining keeps the normal destination") {
                val game = board(caster = 2)
                game.castSpellTargetingPlayer(2, "Mind Rot", 1).error shouldBe null
                game.resolveMindRot(revealFor = emptySet())
                game.graveyardSize(1) shouldBe 2
                game.topOfLibrary(1) shouldBe "Forest"
            }
        }
    }
}
