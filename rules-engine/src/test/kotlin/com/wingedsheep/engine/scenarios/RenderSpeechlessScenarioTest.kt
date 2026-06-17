package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Render Speechless (SOS #220).
 *
 * {2}{W}{B} Sorcery.
 *   "Target opponent reveals their hand. You choose a nonland card from it. That player discards
 *    that card.
 *    Put two +1/+1 counters on up to one target creature."
 *
 * Composed from existing primitives (RevealHand → gather → choose one nonland → discard, then
 * AddCounters(+1/+1, 2) on an optional creature target). The scenarios prove (1) the happy path
 * where a nonland card is discarded and two counters are placed, and (2) declining the optional
 * creature target still strips a card but places no counter.
 */
class RenderSpeechlessScenarioTest : ScenarioTestBase() {

    init {
        context("reveal, discard a nonland card, and put two +1/+1 counters") {

            test("discards the chosen nonland and buffs a creature with two counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Render Speechless")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Hill Giant") // nonland — the only legal discard choice
                    .withCardInHand(2, "Swamp")       // land — not a legal choice
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myCreature = game.findPermanent("Grizzly Bears")!!
                val cardId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Render Speechless"
                }

                // Target the opponent (index 0) and the optional creature (index 1).
                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        cardId,
                        listOf(
                            ChosenTarget.Player(game.player2Id),
                            ChosenTarget.Permanent(myCreature),
                        ),
                    ),
                )
                withClue("Casting Render Speechless should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                // The controller chooses the only nonland card to discard.
                if (game.hasPendingDecision()) {
                    val giant = game.state.getHand(game.player2Id).first {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant"
                    }
                    game.selectCards(listOf(giant))
                    game.resolveStack()
                }

                withClue("Hill Giant is discarded to the opponent's graveyard") {
                    game.state.getGraveyard(game.player2Id).mapNotNull {
                        game.state.getEntity(it)?.get<CardComponent>()?.name
                    } shouldBe listOf("Hill Giant")
                }
                withClue("The land stays in the opponent's hand") {
                    game.state.getZone(game.player2Id, Zone.HAND).mapNotNull {
                        game.state.getEntity(it)?.get<CardComponent>()?.name
                    } shouldBe listOf("Swamp")
                }
                withClue("Grizzly Bears gets two +1/+1 counters") {
                    val counters = game.state.getEntity(myCreature)
                        ?.get<CountersComponent>()
                        ?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                    counters shouldBe 2
                }
            }

            test("declining the optional creature target still discards a card, places no counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Render Speechless")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myCreature = game.findPermanent("Grizzly Bears")!!
                val cardId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Render Speechless"
                }

                // Provide only the mandatory opponent target — decline the "up to one" creature.
                val cast = game.execute(
                    CastSpell(game.player1Id, cardId, listOf(ChosenTarget.Player(game.player2Id))),
                )
                withClue("Declining the optional target is legal: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    val giant = game.state.getHand(game.player2Id).first {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant"
                    }
                    game.selectCards(listOf(giant))
                    game.resolveStack()
                }

                withClue("Hill Giant is still discarded") {
                    game.state.getGraveyard(game.player2Id).size shouldBe 1
                }
                withClue("No counter is placed when the optional target is declined") {
                    val counters = game.state.getEntity(myCreature)
                        ?.get<CountersComponent>()
                        ?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                    counters shouldBe 0
                }
            }
        }
    }
}
