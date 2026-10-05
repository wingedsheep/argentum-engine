package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Biblioplex Kraken ({4}{U}, 4/5 Kraken) — "When this creature enters, scry 3. Whenever this creature
 * attacks, you may return another creature you control to its owner's hand. If you do, this creature
 * can't be blocked this turn."
 *
 * The bounce is chosen on resolution (no target), and the unblockability is gated on a creature
 * actually having been returned.
 */
class BiblioplexKrakenScenarioTest : ScenarioTestBase() {

    private fun TestGame.krakenUnblockable(): Boolean =
        state.projectedState.hasKeyword(findPermanent("Biblioplex Kraken")!!, AbilityFlag.CANT_BE_BLOCKED.name)

    init {
        context("Biblioplex Kraken") {

            test("entering the battlefield scries 3") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Biblioplex Kraken")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Biblioplex Kraken").error shouldBe null
                game.resolveStack()
                game.resolveStack()

                game.isOnBattlefield("Biblioplex Kraken") shouldBe true
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("scry looks at exactly the top three cards") {
                    decision.options shouldHaveSize 3
                }
            }

            test("returning another creature makes the Kraken unblockable this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Biblioplex Kraken")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Biblioplex Kraken" to 2)).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)

                val selection = game.getPendingDecision()
                selection.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("only the controller's other creatures are offered") {
                    selection.options shouldHaveSize 2
                }
                game.selectCards(listOf(game.findPermanent("Grizzly Bears")!!))

                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Glory Seeker") shouldBe true
                game.krakenUnblockable() shouldBe true
            }

            test("declining returns nothing and grants no evasion") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Biblioplex Kraken")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Biblioplex Kraken" to 2)).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false)

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.krakenUnblockable() shouldBe false
            }

            test("with no other creature, nothing is returned and the Kraken stays blockable") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Biblioplex Kraken")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Biblioplex Kraken" to 2)).error shouldBe null
                game.resolveStack()
                if (game.getPendingDecision() is YesNoDecision) game.answerYesNo(true)

                withClue("an opponent's creature is never a candidate") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                game.krakenUnblockable() shouldBe false
            }
        }
    }
}
