package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Birthing Ritual (MH3 #146) — {1}{G} Enchantment.
 *
 * "At the beginning of your end step, if you control a creature, look at the top seven cards of
 *  your library. Then you may sacrifice a creature. If you do, you may put a creature card with
 *  mana value X or less from among those cards onto the battlefield, where X is 1 plus the
 *  sacrificed creature's mana value. Put the rest on the bottom of your library in a random order."
 */
class BirthingRitualScenarioTest : ScenarioTestBase() {

    init {
        context("Birthing Ritual") {

            fun ScenarioTestBase.TestGame.libraryNames(): List<String> =
                state.getLibrary(player1Id).map { state.getEntity(it)!!.get<CardComponent>()!!.name }

            test("sacrificing a creature lets you put a creature card with mana value up to 1 more onto the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Birthing Ritual")
                    .withCardOnBattlefield(1, "Grizzly Bears") // MV 2 -> X = 3
                    // Top of library first; the first seven are looked at.
                    .withCardInLibrary(1, "Hill Giant")       // MV 4 — too big
                    .withCardInLibrary(1, "Centaur Courser")  // MV 3 — eligible
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Llanowar Elves")   // eighth — never looked at
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                val sacDecision = game.getPendingDecision()
                withClue("the sacrifice is offered among your creatures") {
                    (sacDecision is SelectCardsDecision) shouldBe true
                    (sacDecision as SelectCardsDecision).options shouldContainExactlyInAnyOrder listOf(bears)
                }
                game.selectCards(listOf(bears)).error shouldBe null

                val pickDecision = game.getPendingDecision()
                withClue("a creature card choice follows among the looked-at cards") {
                    (pickDecision is SelectCardsDecision) shouldBe true
                }
                val pick = pickDecision as SelectCardsDecision
                val courser = game.state.getLibrary(game.player1Id).first {
                    game.state.getEntity(it)!!.get<CardComponent>()!!.name == "Centaur Courser"
                }
                val giant = game.state.getLibrary(game.player1Id).first {
                    game.state.getEntity(it)!!.get<CardComponent>()!!.name == "Hill Giant"
                }
                withClue("only creature cards with mana value 3 or less are choosable") {
                    pick.options shouldContainExactlyInAnyOrder listOf(courser)
                    pick.options.contains(giant) shouldBe false
                }
                game.selectCards(listOf(courser)).error shouldBe null
                game.resolveStack()

                withClue("Bears sacrificed, Courser put onto the battlefield") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
                withClue("the unseen eighth card is now on top; the other six went to the bottom") {
                    val library = game.libraryNames()
                    library.size shouldBe 7
                    library.first() shouldBe "Llanowar Elves"
                    library.drop(1) shouldContainExactlyInAnyOrder
                        listOf("Hill Giant", "Mountain", "Forest", "Island", "Swamp", "Plains")
                }
            }

            test("declining the sacrifice puts all seven looked-at cards on the bottom") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Birthing Ritual")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Centaur Courser")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Llanowar Elves")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.skipSelection().error shouldBe null
                game.resolveStack()

                withClue("no sacrifice, no creature put onto the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Centaur Courser") shouldBe false
                }
                withClue("the seven looked-at cards went to the bottom beneath the unseen eighth") {
                    val library = game.libraryNames()
                    library.size shouldBe 8
                    library.first() shouldBe "Llanowar Elves"
                    library.drop(1) shouldContainExactlyInAnyOrder listOf(
                        "Centaur Courser", "Mountain", "Forest", "Island", "Swamp", "Plains", "Hill Giant"
                    )
                }
            }

            test("does not trigger when you control no creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Birthing Ritual")
                    .withCardInLibrary(1, "Centaur Courser")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("intervening if failed — nothing looked at, library order untouched") {
                    (game.getPendingDecision() is SelectCardsDecision) shouldBe false
                    game.libraryNames() shouldBe listOf("Centaur Courser", "Mountain")
                }
            }
        }
    }
}
