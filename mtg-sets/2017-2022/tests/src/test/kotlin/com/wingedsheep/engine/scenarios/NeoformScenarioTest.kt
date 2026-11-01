package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Neoform (WAR #206) — {G}{U} Sorcery.
 *
 * "As an additional cost to cast this spell, sacrifice a creature.
 *  Search your library for a creature card with mana value equal to 1 plus the sacrificed
 *  creature's mana value, put that card onto the battlefield with an additional +1/+1 counter on
 *  it, then shuffle."
 *
 * Pins the cost-linked *exact* mana value (read from the sacrifice snapshot after the creature has
 * left the battlefield) and the entry counter on the found creature.
 */
class NeoformScenarioTest : ScenarioTestBase() {

    init {
        context("Neoform") {

            test("finds only a creature card with mana value exactly 1 more, entering with a +1/+1 counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Neoform")
                    .withCardOnBattlefield(1, "Grizzly Bears") // MV 2 -> needs MV 3
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Llanowar Elves")  // MV 1 — too small (not "or less")
                    .withCardInLibrary(1, "Centaur Courser") // MV 3 — the only legal pick
                    .withCardInLibrary(1, "Hill Giant")      // MV 4 — too big
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val neoform = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Neoform"
                }
                val courser = game.state.getLibrary(game.player1Id).first {
                    game.state.getEntity(it)!!.get<CardComponent>()!!.name == "Centaur Courser"
                }

                game.execute(
                    CastSpell(
                        game.player1Id,
                        neoform,
                        emptyList(),
                        additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears))
                    )
                ).error shouldBe null
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true

                game.resolveStack()

                val pick = game.getPendingDecision()
                withClue("a library search decision is pending") {
                    (pick is SelectCardsDecision) shouldBe true
                }
                withClue("only the mana value 3 creature card is choosable") {
                    (pick as SelectCardsDecision).options shouldContainExactlyInAnyOrder listOf(courser)
                }
                game.selectCards(listOf(courser)).error shouldBe null
                game.resolveStack()

                withClue("Centaur Courser is on the battlefield with one +1/+1 counter") {
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                    game.state.getEntity(courser)!!.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
                withClue("the rest of the library is untouched apart from the shuffle") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isOnBattlefield("Llanowar Elves") shouldBe false
                    game.state.getLibrary(game.player1Id).size shouldBe 3
                }
            }
        }
    }
}
