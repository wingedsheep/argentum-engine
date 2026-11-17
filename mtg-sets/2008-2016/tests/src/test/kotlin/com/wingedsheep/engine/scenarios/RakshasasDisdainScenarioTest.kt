package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Rakshasa's Disdain (FRF #45) — {2}{U} Instant.
 *
 *   Counter target spell unless its controller pays {1} for each card in your graveyard.
 *
 * The count is taken as Rakshasa's Disdain resolves, while it is still on the stack, so it never
 * counts itself (2014-11-24 ruling). "Your" is the counterspeller: Player 1 casts Rakshasa's
 * Disdain, Player 2 casts Grizzly Bears and pays the tax.
 */
class RakshasasDisdainScenarioTest : ScenarioTestBase() {

    init {
        context("Rakshasa's Disdain") {

            fun board(myGraveyard: Int = 0, theirGraveyard: Int = 0) = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Rakshasa's Disdain")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInHand(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", 4)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .apply { repeat(myGraveyard) { withCardInGraveyard(1, "Lightning Bolt") } }
                .apply { repeat(theirGraveyard) { withCardInGraveyard(2, "Lightning Bolt") } }
                .build()

            fun TestGame.castBearsThenDisdain() {
                castSpell(2, "Grizzly Bears").error shouldBe null
                passPriority()
                castSpellTargetingStackSpell(1, "Rakshasa's Disdain", "Grizzly Bears").error shouldBe null
                resolveStack()
            }

            test("taxes {1} per card in the caster's graveyard, not counting itself") {
                val game = board(myGraveyard = 2)
                game.castBearsThenDisdain()

                val decision = game.getPendingDecision()
                    .shouldNotBeNull()
                    .shouldBeInstanceOf<YesNoDecision>()
                withClue("The bill goes to the spell's controller") {
                    decision.playerId shouldBe game.player2Id
                }
                withClue("Two cards in the graveyard is {2} — Rakshasa's Disdain is still on the stack") {
                    decision.prompt shouldContain "{2}"
                }

                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                withClue("Declining the tax counters Grizzly Bears") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("with an empty graveyard the tax is {0}, so the spell resolves") {
                val game = board()
                game.castBearsThenDisdain()

                game.getPendingDecision().shouldBeNull()
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("the spell controller's graveyard does not count") {
                val game = board(theirGraveyard = 3)
                game.castBearsThenDisdain()

                withClue("'Your graveyard' is Player 1's, which is empty") {
                    game.getPendingDecision().shouldBeNull()
                }
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
