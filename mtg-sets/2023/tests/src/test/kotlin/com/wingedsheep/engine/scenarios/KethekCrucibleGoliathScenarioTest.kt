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
 * Kethek, Crucible Goliath (ONE #206) — {2}{B}{R} Legendary Creature — Phyrexian Beast 4/4.
 *
 * "At the beginning of your end step, you may sacrifice another creature. If you do, reveal cards
 *  from the top of your library until you reveal a nonlegendary creature card with lesser mana
 *  value, put it onto the battlefield, then put the rest on the bottom of your library in a random
 *  order."
 */
class KethekCrucibleGoliathScenarioTest : ScenarioTestBase() {

    init {
        context("Kethek, Crucible Goliath") {

            fun ScenarioTestBase.TestGame.libraryNames(): List<String> =
                state.getLibrary(player1Id).map { state.getEntity(it)!!.get<CardComponent>()!!.name }

            test("sacrificing a creature puts the first nonlegendary creature with lesser mana value onto the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kethek, Crucible Goliath")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    // Top of library first.
                    .withCardInLibrary(1, "Hill Giant")             // MV 4 — not lesser
                    .withCardInLibrary(1, "Centaur Courser")        // MV 3 — equal, not lesser
                    .withCardInLibrary(1, "Isamaru, Hound of Konda") // MV 1 but legendary
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Grizzly Bears")          // MV 2 — the hit
                    .withCardInLibrary(1, "Llanowar Elves")         // never revealed
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                val kethek = game.findPermanent("Kethek, Crucible Goliath")!!
                val courser = game.findPermanent("Centaur Courser")!!

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                val decision = game.getPendingDecision()
                withClue("only *another* creature is offered — not Kethek itself") {
                    (decision is SelectCardsDecision) shouldBe true
                    (decision as SelectCardsDecision).options shouldContainExactlyInAnyOrder listOf(courser)
                }
                (decision as SelectCardsDecision).options.contains(kethek) shouldBe false
                game.selectCards(listOf(courser)).error shouldBe null
                game.resolveStack()

                withClue("the Courser was sacrificed and Grizzly Bears came out of the library") {
                    game.isInGraveyard(1, "Centaur Courser") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isOnBattlefield("Isamaru, Hound of Konda") shouldBe false
                }
                withClue("the unrevealed Elves stay on top; the four other revealed cards go to the bottom") {
                    val library = game.libraryNames()
                    library.size shouldBe 5
                    library.first() shouldBe "Llanowar Elves"
                    library.drop(1) shouldContainExactlyInAnyOrder
                        listOf("Hill Giant", "Centaur Courser", "Isamaru, Hound of Konda", "Mountain")
                }
            }

            test("declining the sacrifice reveals nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kethek, Crucible Goliath")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.skipSelection().error shouldBe null
                game.resolveStack()

                withClue("'If you do' never fired — no sacrifice, library untouched") {
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.libraryNames() shouldBe listOf("Hill Giant", "Grizzly Bears")
                }
            }

            test("sacrificing a mana value 0 creature finds nothing and bottoms the whole library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kethek, Crucible Goliath")
                    .withCardOnBattlefield(1, "Memnite")
                    .withCardInLibrary(1, "Ornithopter")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                val memnite = game.findPermanent("Memnite")!!

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.selectCards(listOf(memnite)).error shouldBe null
                game.resolveStack()

                withClue("nothing has mana value less than 0 — even the 0-drop Ornithopter is not lesser") {
                    game.isInGraveyard(1, "Memnite") shouldBe true
                    game.isOnBattlefield("Ornithopter") shouldBe false
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.libraryNames() shouldContainExactlyInAnyOrder listOf("Ornithopter", "Grizzly Bears")
                }
            }
        }
    }
}
