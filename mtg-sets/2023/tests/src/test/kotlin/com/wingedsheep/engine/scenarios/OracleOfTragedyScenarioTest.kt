package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Oracle of Tragedy {1}{U} — Creature — Human Wizard 1/3 (MOM #71)
 *
 * When this creature enters or dies, choose one —
 * • Draw a card, then discard a card.
 * • Shuffle up to four target cards with mana value 3 or greater from your graveyard into your library.
 */
class OracleOfTragedyScenarioTest : ScenarioTestBase() {

    init {
        context("Oracle of Tragedy") {

            fun ScenarioTestBase.TestGame.chooseMode(index: Int) {
                val modeDecision = getPendingDecision() as? ChooseOptionDecision
                    ?: error("expected the mode choice; got ${getPendingDecision()}")
                submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = index)).error shouldBe null
            }

            fun ScenarioTestBase.TestGame.castOracle() {
                castSpell(1, "Oracle of Tragedy").error shouldBe null
                if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
                resolveStack()
            }

            test("enters — loot mode draws a card, then discards a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Oracle of Tragedy")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castOracle()
                game.chooseMode(0)
                game.resolveStack()

                val discard = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected the discard choice; got ${game.getPendingDecision()}")
                withClue("drew one card: hand is Grizzly Bears plus the drawn card") {
                    discard.options.size shouldBe 2
                }
                val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.handSize(1) shouldBe 1
                game.librarySize(1) shouldBe 1
            }

            test("enters — shuffle mode targets only your MV 3+ graveyard cards, up to four") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Oracle of Tragedy")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(1, "Centaur Courser")
                    .withCardInGraveyard(1, "Centaur Courser")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(2, "Hill Giant")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giants = game.findCardsInGraveyard(1, "Hill Giant")
                val coursers = game.findCardsInGraveyard(1, "Centaur Courser")
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val theirGiant = game.findCardsInGraveyard(2, "Hill Giant").single()

                game.castOracle()
                game.chooseMode(1)

                val targeting = game.getPendingDecision() as? ChooseTargetsDecision
                    ?: error("expected target selection; got ${game.getPendingDecision()}")
                withClue("only your graveyard's MV 3+ cards are legal") {
                    targeting.legalTargets[0]!! shouldContainExactlyInAnyOrder giants + coursers
                }
                withClue("up to four") {
                    targeting.targetRequirements[0].maxTargets shouldBe 4
                    targeting.targetRequirements[0].minTargets shouldBe 0
                }

                val chosen = giants + coursers.first()
                game.selectTargets(chosen).error shouldBe null
                game.resolveStack()

                val library = game.state.getLibrary(game.player1Id)
                val graveyard = game.state.getGraveyard(game.player1Id)
                withClue("the four chosen cards were shuffled into the library") {
                    chosen.all { it in library } shouldBe true
                    library.size shouldBe 5
                }
                withClue("unchosen and illegal cards stay put") {
                    (coursers.last() in graveyard) shouldBe true
                    (bears in graveyard) shouldBe true
                    (theirGiant in game.state.getGraveyard(game.player2Id)) shouldBe true
                }
            }

            test("dies — the trigger fires again and can shuffle cards back") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Oracle of Tragedy")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val oracle = game.findPermanent("Oracle of Tragedy")!!
                val giant = game.findCardsInGraveyard(1, "Hill Giant").single()

                game.castSpell(1, "Lightning Bolt", oracle).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.isInGraveyard(1, "Oracle of Tragedy") shouldBe true
                game.chooseMode(1)
                val targeting = game.getPendingDecision() as? ChooseTargetsDecision
                    ?: error("expected target selection; got ${game.getPendingDecision()}")
                withClue("the Oracle itself is MV 2, so it is not a legal target") {
                    targeting.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(giant)
                }
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                (giant in game.state.getLibrary(game.player1Id)) shouldBe true
                game.isInGraveyard(1, "Oracle of Tragedy") shouldBe true
            }
        }
    }
}
