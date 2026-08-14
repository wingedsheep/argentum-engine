package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Web of Life and Destiny (SPM #122) — {6}{G}{G} Enchantment.
 *
 *   Convoke (Your creatures can help cast this spell. Each creature you tap while casting this
 *   spell pays for {1} or one mana of that creature's color.)
 *   At the beginning of combat on your turn, look at the top five cards of your library. You may
 *   put a creature card from among them onto the battlefield. Put the rest on the bottom of your
 *   library in a random order.
 *
 * Covers (1) Convoke helping to pay the cost (a green creature tapped for one {G} lets seven lands
 * finish an eight-mana spell), and (2) the begin-combat dig — look at the top five, put a creature
 * card onto the battlefield, and bottom the rest.
 */
class WebOfLifeAndDestinyScenarioTest : ScenarioTestBase() {

    init {
        context("Web of Life and Destiny") {

            test("Convoke: a green creature tapped for {G} lets seven lands cast {6}{G}{G}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Web of Life and Destiny")
                    // Only seven lands — one short of the eight-mana cost. Convoke supplies the rest.
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withCardOnBattlefield(1, "Grizzly Bears") // green 2/2, untapped
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.findCardsInHand(1, "Web of Life and Destiny").single()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        alternativePayment = AlternativePaymentChoice(
                            convokedCreatures = mapOf(bears to ConvokePayment(color = Color.GREEN))
                        )
                    )
                )
                withClue("Convoke should let seven lands + one creature pay {6}{G}{G}: ${cast.error}") {
                    cast.error shouldBe null
                }
                withClue("The convoked creature was tapped to pay for the spell") {
                    game.state.getEntity(bears)?.get<TappedComponent>().shouldNotBeNull()
                }

                game.resolveStack()
                withClue("Web of Life and Destiny resolved onto the battlefield") {
                    game.isOnBattlefield("Web of Life and Destiny") shouldBe true
                }
            }

            test("Begin combat: look at top five, put a creature onto the battlefield, bottom the rest") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Web of Life and Destiny")
                    // Library is exactly the five looked-at cards: one creature + four lands.
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Advance into the begin-combat step; the trigger is queued on the stack.
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                val decision = game.getPendingDecision()
                    .shouldNotBeNull()
                    .shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Putting a creature onto the battlefield is optional (\"may\"), up to one") {
                    decision.minSelections shouldBe 0
                    decision.maxSelections shouldBe 1
                }

                // Only the creature card is eligible.
                val creatureOptions = decision.options.filter {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                creatureOptions.size shouldBe 1
                game.selectCards(creatureOptions)

                withClue("The chosen creature entered the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("The four non-creature cards were bottomed, none kept in hand") {
                    game.handSize(1) shouldBe 0
                    game.librarySize(1) shouldBe 4
                    game.findCardsInLibrary(1, "Forest").size shouldBe 4
                    game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 0
                }
            }
        }
    }
}
