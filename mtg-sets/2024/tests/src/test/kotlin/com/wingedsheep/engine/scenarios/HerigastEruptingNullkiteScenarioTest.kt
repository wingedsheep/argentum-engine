package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Herigast, Erupting Nullkite — Emerge {6}{R}{R}; "When you cast this spell, you may exile your
 * hand. If you do, draw three cards."; flying; "Each creature spell you cast has emerge. The emerge
 * cost is equal to its mana cost."
 */
class HerigastEruptingNullkiteScenarioTest : ScenarioTestBase() {
    init {
        fun emergeActionFor(game: TestGame, cardName: String) = game.getLegalActions(1).firstOrNull { la ->
            val cast = la.action as? CastSpell
            cast != null && cast.alternativeCostType == AlternativeCostType.EMERGE &&
                cast.cardId == game.findCardsInHand(1, cardName).firstOrNull()
        }

        test("creature spells you cast gain emerge priced at their own mana cost") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Herigast, Erupting Nullkite")
                .withCardInHand(1, "Craw Wurm") // {4}{G}{G}
                .withCardOnBattlefield(1, "Centaur Courser") // mana value 3
                .withLandsOnBattlefield(1, "Forest", 3) // {4}{G}{G} - 3 = {1}{G}{G}
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val offer = emergeActionFor(game, "Craw Wurm")!!
            offer.manaCostString shouldBe "{4}{G}{G}"

            game.castSpellWithEmerge(1, "Craw Wurm", "Centaur Courser").error shouldBe null
            game.isInGraveyard(1, "Centaur Courser") shouldBe true
            game.resolveStack()
            game.isOnBattlefield("Craw Wurm") shouldBe true
        }

        test("without Herigast the same creature has no emerge") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Craw Wurm")
                .withCardOnBattlefield(1, "Centaur Courser")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            emergeActionFor(game, "Craw Wurm") shouldBe null
        }

        test("emerging Herigast lets you exile your hand and draw three") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Herigast, Erupting Nullkite")
                .withCardInHand(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Craw Wurm") // mana value 6: {6}{R}{R} - 6 = {R}{R}
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellWithEmerge(1, "Herigast, Erupting Nullkite", "Craw Wurm").error shouldBe null
            game.isInGraveyard(1, "Craw Wurm") shouldBe true

            game.resolveStack()
            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Grizzly Bears") shouldBe true
            game.handSize(1) shouldBe 3
            game.isOnBattlefield("Herigast, Erupting Nullkite") shouldBe true
        }

        test("declining the cast trigger keeps your hand") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Herigast, Erupting Nullkite")
                .withCardInHand(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellWithEmerge(1, "Herigast, Erupting Nullkite", "Craw Wurm").error shouldBe null
            game.resolveStack()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.handSize(1) shouldBe 1
        }
    }
}
