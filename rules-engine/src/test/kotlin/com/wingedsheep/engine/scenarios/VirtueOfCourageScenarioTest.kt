package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Virtue of Courage // Embereth Blaze.
 *
 * The new vocabulary here is the dynamic-count impulse — `Patterns.Exile.impulse(DynamicAmount)` —
 * driven off `ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT`, so the exiled count follows the noncombat
 * damage just dealt. The Adventure face is ordinary machinery, smoke-tested at the end.
 */
class VirtueOfCourageScenarioTest : ScenarioTestBase() {

    init {
        context("Virtue of Courage") {
            test("noncombat damage to an opponent exiles that many cards, playable this turn") {
                val game = scenario()
                    .withPlayers("Virtue", "Opponent")
                    .withCardOnBattlefield(1, "Virtue of Courage")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libraryBefore = game.librarySize(1)

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17

                // "you may exile that many cards from the top of your library" — accept.
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                // Three damage → three cards, not a fixed count.
                game.librarySize(1) shouldBe libraryBefore - 3
                game.isInExile(1, "Grizzly Bears") shouldBe true

                // "You may play those cards this turn."
                game.castSpellFromExile(1, "Grizzly Bears").error shouldBe null
            }

            test("declining the may exiles nothing") {
                val game = scenario()
                    .withPlayers("Virtue", "Opponent")
                    .withCardOnBattlefield(1, "Virtue of Courage")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libraryBefore = game.librarySize(1)

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.librarySize(1) shouldBe libraryBefore
                game.isInExile(1, "Grizzly Bears") shouldBe false
            }

            test("noncombat damage to yourself does not trigger it") {
                val game = scenario()
                    .withPlayers("Virtue", "Opponent")
                    .withCardOnBattlefield(1, "Virtue of Courage")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libraryBefore = game.librarySize(1)

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 17
                game.hasPendingDecision() shouldBe false
                game.librarySize(1) shouldBe libraryBefore
            }
        }

        context("Embereth Blaze") {
            test("the Adventure deals 2 damage and exiles itself for the enchantment to be cast later") {
                val game = scenario()
                    .withPlayers("Virtue", "Opponent")
                    .withCardInHand(1, "Virtue of Courage")
                    .withLandsOnBattlefield(1, "Mountain", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val card = game.findCardsInHand(1, "Virtue of Courage").single()
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = card,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        faceIndex = 0
                    )
                ).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 18
                game.isInExile(1, "Virtue of Courage") shouldBe true

                game.castSpellFromExile(1, "Virtue of Courage").error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Virtue of Courage") shouldBe true
            }
        }
    }
}
