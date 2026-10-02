package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ulamog, the Defiler (MH3 #15) — {10} Legendary Creature — Eldrazi 7/7
 *
 *   When you cast this spell, target opponent exiles the top half of their library, rounded up.
 *   Ward—Sacrifice two permanents.
 *   Ulamog enters with a number of +1/+1 counters on it equal to the greatest mana value among
 *   cards in exile.
 *   Ulamog has annihilator X, where X is the number of +1/+1 counters on it.
 *
 * The entry count is measured before Ulamog leaves the zone it comes from: blinked through exile it
 * sees itself there and enters with ten counters (the card's ruling). Cast from hand it sits on the
 * stack, so only what the cast trigger exiled counts.
 */
class UlamogTheDefilerScenarioTest : ScenarioTestBase() {

    private fun plusCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Ulamog, the Defiler") {

            test("the cast trigger exiles half the opponent's library rounded up, and Ulamog counts the exile") {
                var builder = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Ulamog, the Defiler")
                    .withLandsOnBattlefield(1, "Island", 10)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(7) { builder = builder.withCardInLibrary(2, "Craw Wurm") }
                val game = builder.build()

                game.castSpell(1, "Ulamog, the Defiler").error shouldBe null
                if (game.hasPendingDecision()) game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                withClue("seven cards, half rounded up is four exiled") {
                    game.librarySize(2) shouldBe 3
                    game.state.getZone(game.player2Id, Zone.EXILE).size shouldBe 4
                }
                val ulamog = game.findPermanent("Ulamog, the Defiler")!!
                withClue("Craw Wurm (mana value 6) is the greatest in exile; Ulamog was on the stack") {
                    plusCounters(game, ulamog) shouldBe 6
                }
            }

            test("blinked through exile, Ulamog sees itself among the cards in exile and enters with ten counters") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Ulamog, the Defiler")
                    .withCardInHand(1, "Momentary Blink")
                    .withCardInExile(2, "Craw Wurm")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ulamog = game.findPermanent("Ulamog, the Defiler")!!
                plusCounters(game, ulamog) shouldBe 0

                game.castSpell(1, "Momentary Blink", ulamog).error shouldBe null
                game.resolveStack()

                val returned = game.findPermanent("Ulamog, the Defiler")!!
                withClue("Ulamog's own mana value 10 beats Craw Wurm's 6") {
                    plusCounters(game, returned) shouldBe 10
                }
            }

            test("annihilator X reads the +1/+1 counters on Ulamog") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Ulamog, the Defiler", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ulamog = game.findPermanent("Ulamog, the Defiler")!!
                game.state = game.state.updateEntity(ulamog) { c ->
                    c.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 2))
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Ulamog, the Defiler" to 2)).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the defending player sacrifices two of their four permanents") {
                    decision.playerId shouldBe game.player2Id
                    decision.options.size shouldBe 4
                }
                val bears = game.findPermanent("Grizzly Bears")!!
                val forest = game.state.getBattlefield().first { game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Forest" }
                game.selectCards(listOf(bears, forest)).error shouldBe null
                game.resolveStack()

                withClue("two permanents were sacrificed, two remain") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.state.getBattlefield(game.player2Id).size shouldBe 2
                }
            }
        }
    }
}
