package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Knight-Errant of Eos (MOM #26) — {4}{W} 4/4, convoke. "When this creature enters, look at the top
 * six cards of your library. You may reveal up to two creature cards with mana value X or less from
 * among them, where X is the number of creatures that convoked this creature. Put the revealed cards
 * into your hand, then shuffle."
 */
class KnightErrantOfEosScenarioTest : ScenarioTestBase() {

    private fun TestGame.nameOf(id: com.wingedsheep.sdk.model.EntityId): String? =
        state.getEntity(id)?.get<CardComponent>()?.name

    init {
        context("Knight-Errant of Eos") {

            test("three convoking creatures make X = 3: only creatures of mana value 3 or less are takeable") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Knight-Errant of Eos")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Gray Ogre")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Gray Ogre")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanents("Grizzly Bears")
                val handBefore = game.handSize(1)
                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Knight-Errant of Eos").single(),
                        alternativePayment = AlternativePaymentChoice(
                            convokedCreatures = bears.associateWith { ConvokePayment(color = null) }
                        )
                    )
                )
                withClue("three bears pay {3}, two Plains pay {1}{W}: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Grizzly Bears (MV 2) and both Gray Ogres (MV 3) are eligible; Hill Giant (MV 4) and lands are not") {
                    decision.options.map { game.nameOf(it) } shouldContainExactlyInAnyOrder
                        listOf("Grizzly Bears", "Gray Ogre", "Gray Ogre")
                }

                val chosen = decision.options.filter { game.nameOf(it) == "Gray Ogre" }
                game.submitDecision(
                    CardsSelectedResponse(decisionId = decision.id, selectedCards = chosen)
                ).error shouldBe null
                game.resolveStack()

                withClue("two Gray Ogres went to hand (Knight left the hand)") {
                    game.findCardsInHand(1, "Gray Ogre").size shouldBe 2
                    game.handSize(1) shouldBe handBefore - 1 + 2
                }
                withClue("the other four were shuffled back into the library") {
                    game.librarySize(1) shouldBe 4
                }
            }

            test("hard-cast with mana: X = 0, so no creature card can be taken") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Knight-Errant of Eos")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Gray Ogre")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.castSpell(1, "Knight-Errant of Eos").error shouldBe null
                game.resolveStack()

                if (game.hasPendingDecision()) {
                    val decision = game.getPendingDecision()
                    decision.shouldBeInstanceOf<SelectCardsDecision>()
                    withClue("no creature card has mana value 0 or less") { decision.options.size shouldBe 0 }
                    game.skipSelection().error shouldBe null
                    game.resolveStack()
                }

                (game.findPermanent("Knight-Errant of Eos") != null) shouldBe true
                game.handSize(1) shouldBe handBefore - 1
                game.librarySize(1) shouldBe 6
            }
        }
    }
}
