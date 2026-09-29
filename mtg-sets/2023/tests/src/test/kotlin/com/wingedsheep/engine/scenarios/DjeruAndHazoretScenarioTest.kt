package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Djeru and Hazoret (MOM #221) — vigilance and haste while you have one or fewer cards in hand;
 * attacking looks at the top six, may exile a legendary creature card, the rest go to the bottom
 * in a random order, and the exiled card may be cast for free this turn.
 */
class DjeruAndHazoretScenarioTest : ScenarioTestBase() {

    init {
        context("Djeru and Hazoret") {
            test("has vigilance and haste only with one or fewer cards in hand") {
                val many = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Djeru and Hazoret")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val djeru = many.findPermanent("Djeru and Hazoret")!!
                withClue("two cards in hand: no keywords") {
                    many.state.projectedState.hasKeyword(djeru, Keyword.VIGILANCE) shouldBe false
                    many.state.projectedState.hasKeyword(djeru, Keyword.HASTE) shouldBe false
                }

                val one = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Djeru and Hazoret")
                    .withCardInHand(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val djeru1 = one.findPermanent("Djeru and Hazoret")!!
                withClue("one card in hand: vigilance and haste") {
                    one.state.projectedState.hasKeyword(djeru1, Keyword.VIGILANCE) shouldBe true
                    one.state.projectedState.hasKeyword(djeru1, Keyword.HASTE) shouldBe true
                }
            }

            test("attacking exiles a legendary creature from the top six, castable for free this turn") {
                var b = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Djeru and Hazoret", summoningSickness = false)
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Forest")
                    .withCardInLibrary(1, "Sharkey, Tyrant of the Shire")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(7) { b = b.withCardInLibrary(1, "Grizzly Bears") }
                repeat(4) { b = b.withCardInLibrary(2, "Forest") }
                val game = b.build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Djeru and Hazoret" to 2)).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val sharkey = game.findCardsInLibrary(1, "Sharkey, Tyrant of the Shire").single()
                withClue("a nonlegendary creature can't be chosen") {
                    val bears = game.findCardsInLibrary(1, "Grizzly Bears").first()
                    (game.selectCards(listOf(bears)).error != null) shouldBe true
                }
                decision.shouldNotBeNull()
                game.selectCards(listOf(sharkey)).error shouldBe null
                game.resolveStack()

                game.isInExile(1, "Sharkey, Tyrant of the Shire") shouldBe true
                withClue("the other five cards went to the bottom; library keeps them") {
                    game.librarySize(1) shouldBe 7
                }

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("no lands: the exiled legend is cast without paying its mana cost") {
                    game.castSpellFromExile(1, "Sharkey, Tyrant of the Shire").error shouldBe null
                }
                game.resolveStack()
                game.isOnBattlefield("Sharkey, Tyrant of the Shire") shouldBe true
            }

            test("declining leaves all six cards in the library") {
                var b = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Djeru and Hazoret", summoningSickness = false)
                    .withCardInLibrary(1, "Sharkey, Tyrant of the Shire")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(7) { b = b.withCardInLibrary(1, "Grizzly Bears") }
                repeat(4) { b = b.withCardInLibrary(2, "Forest") }
                val game = b.build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Djeru and Hazoret" to 2)).error shouldBe null
                game.resolveStack()
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                game.isInExile(1, "Sharkey, Tyrant of the Shire") shouldBe false
                game.librarySize(1) shouldBe 8
            }
        }
    }
}
