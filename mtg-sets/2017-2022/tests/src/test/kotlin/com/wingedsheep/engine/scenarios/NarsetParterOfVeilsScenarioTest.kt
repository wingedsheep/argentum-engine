package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.war.cards.NarsetParterOfVeils
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Narset, Parter of Veils (WAR #61; {1}{U}{U}, Loyalty 5).
 *
 *   Each opponent can't draw more than one card each turn.
 *   −2: Look at the top four cards of your library. You may reveal a noncreature, nonland card from
 *   among them and put it into your hand. Put the rest on the bottom of your library in a random order.
 *
 * The draw cap's rules matrix lives in the engine's `CardsDrawnPerTurnCapTest`; this pins the card.
 */
class NarsetParterOfVeilsScenarioTest : ScenarioTestBase() {

    private val minusTwo = NarsetParterOfVeils.activatedAbilities[0].id

    init {
        context("Narset, Parter of Veils") {

            test("an opponent's Divination draws only one card, and a second draw that turn draws none") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Narset, Parter of Veils")
                    .withCardInHand(1, "Divination")
                    .withCardInHand(1, "Divination")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .apply { repeat(6) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Divination").error shouldBe null
                game.resolveStack()
                withClue("Divination's two draws are cut to one") { game.handSize(1) shouldBe 2 }

                game.castSpell(1, "Divination").error shouldBe null
                game.resolveStack()
                withClue("a second draw instruction the same turn draws nothing") { game.handSize(1) shouldBe 1 }
                game.librarySize(1) shouldBe 5
            }

            test("−2 takes a noncreature, nonland card and bottoms the rest") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Narset, Parter of Veils")
                    .withCardInLibrary(1, "Divination")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val narset = game.findPermanent("Narset, Parter of Veils")!!
                game.state = game.state.updateEntity(narset) { c ->
                    c.with(CountersComponent().withAdded(CounterType.LOYALTY, 5))
                }

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = narset, abilityId = minusTwo))
                    .error shouldBe null
                game.resolveStack()

                val choice = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected a reveal selection; got ${game.getPendingDecision()}")
                val bolt = game.findCardsInLibrary(1, "Lightning Bolt").single()
                withClue("creatures and lands among the top four are not offered") {
                    choice.options.toSet() shouldBe setOf(game.findCardsInLibrary(1, "Divination").single(), bolt)
                }
                game.selectCards(listOf(bolt)).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Lightning Bolt") shouldBe true
                game.isInHand(1, "Divination") shouldBe false
                withClue("the fifth card stays on top; the other three go to the bottom") {
                    game.state.getZone(game.player1Id, com.wingedsheep.sdk.core.Zone.LIBRARY).first() shouldBe
                        game.findCardsInLibrary(1, "Hill Giant").single()
                }
                game.librarySize(1) shouldBe 4
                game.state.getEntity(narset)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 3
            }
        }
    }
}
