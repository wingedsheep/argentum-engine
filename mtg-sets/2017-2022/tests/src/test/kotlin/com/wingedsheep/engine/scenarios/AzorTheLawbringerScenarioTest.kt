package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Azor, the Lawbringer (RIX #154) — enters: each opponent can't cast instant or sorcery spells
 * during that player's next turn; attacks: you may pay {X}{W}{U}{U} to gain X life and draw X cards.
 */
class AzorTheLawbringerScenarioTest : ScenarioTestBase() {

    init {
        test("entering bans the opponent's instants and sorceries during their next turn only") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Azor, the Lawbringer")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardsInHand(2, "Lightning Bolt", 2)
                .withLandsOnBattlefield(2, "Mountain", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Azor, the Lawbringer").error shouldBe null
            game.resolveStack()
            game.findPermanent("Azor, the Lawbringer") shouldNotBe null

            withClue("still the caster's turn: the opponent may cast an instant") {
                game.passPriority().error shouldBe null
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player2Id
            withClue("the opponent's next turn: no instants") {
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldNotBe null
            }
        }

        test("attacking and paying {X}{W}{U}{U} with X = 2 gains 2 life and draws 2 cards") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Azor, the Lawbringer")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val handBefore = game.handSize(1)
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Azor, the Lawbringer" to 2)).error shouldBe null
            game.resolveStack()

            val ask = game.getPendingDecision()
            ask.shouldBeInstanceOf<YesNoDecision>()
            ask.yesText shouldBe "Pay {X}{W}{U}{U}"
            game.answerYesNo(true).error shouldBe null

            val number = game.getPendingDecision()
            number.shouldBeInstanceOf<ChooseNumberDecision>()
            withClue("five lands, three reserved for {W}{U}{U}, leave X up to 2") {
                number.maxValue shouldBe 2
            }
            game.chooseNumber(2).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 22
            game.handSize(1) shouldBe handBefore + 2
        }
    }
}
