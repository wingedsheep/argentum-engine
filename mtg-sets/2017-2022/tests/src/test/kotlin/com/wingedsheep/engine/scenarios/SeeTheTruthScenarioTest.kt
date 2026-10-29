package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * See the Truth (M21 #69) — {1}{U} Sorcery.
 *
 *   Look at the top three cards of your library. Put one of those cards into your hand and the
 *   rest on the bottom of your library in any order. If this spell was cast from anywhere other
 *   than your hand, put each of those cards into your hand instead.
 *
 * Proves the cast-origin gate: a hand cast digs one of three, a graveyard cast (permitted by a
 * test enabler) takes all three without a selection.
 */
class SeeTheTruthScenarioTest : ScenarioTestBase() {

    private val graveyardSorceries = card("Test Sorcery Recaster") {
        manaCost = "{U}"
        typeLine = "Enchantment"
        oracleText = "You may cast sorcery spells from your graveyard."
        staticAbility { ability = MayCastFromGraveyard(filter = GameObjectFilter.Sorcery) }
    }

    init {
        cardRegistry.register(graveyardSorceries)

        context("See the Truth") {

            test("cast from hand: keep one of the top three, the rest go to the bottom") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "See the Truth")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Glory Seeker")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                game.castSpell(1, "See the Truth").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Looks at exactly the top three cards") {
                    decision.options.size shouldBe 3
                }
                decision.maxSelections shouldBe 1
                game.selectCards(decision.options.take(1)).error shouldBe null
                if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()

                withClue("Hand: the spell left (-1), one kept card arrived (+1)") {
                    game.handSize(1) shouldBe handBefore
                }
                withClue("The two unchosen cards stay in the library, on the bottom") {
                    game.librarySize(1) shouldBe 3
                }
            }

            test("cast from the graveyard: all three cards go to hand with no selection") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Test Sorcery Recaster")
                    .withCardInGraveyard(1, "See the Truth")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Glory Seeker")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                game.castSpellFromGraveyard(1, "See the Truth").error shouldBe null
                game.resolveStack()

                withClue("No keep-one selection when cast from outside the hand") {
                    (game.getPendingDecision() is SelectCardsDecision) shouldBe false
                }
                withClue("All three looked-at cards are put into hand") {
                    game.handSize(1) shouldBe handBefore + 3
                }
                game.librarySize(1) shouldBe 1
            }
        }
    }
}
