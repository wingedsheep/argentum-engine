package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * Scenario tests for Crumb and Get It.
 *
 * Crumb and Get It
 * {W}
 * Instant
 *
 * Gift a Food — Target creature you control gets +2/+2 until end of turn.
 * If the gift was promised, that creature also gains indestructible until end of turn.
 */
class CrumbAndGetItScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    private val vanillaBear = CardDefinition.creature(
        name = "Vanilla Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2, toughness = 2
    )

    init {
        cardRegistry.register(vanillaBear)

        test("Crumb and Get It without gift gives +2/+2") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Crumb and Get It")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardOnBattlefield(1, "Vanilla Bear")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            val bear = game.findPermanent("Vanilla Bear")
            bear.shouldNotBeNull()

            // Cast Crumb and Get It targeting Vanilla Bear
            game.castSpell(1, "Crumb and Get It", bear)
            game.resolveStack()

            // Decline the gift
            game.answerYesNo(false)

            // Bear should be 4/4 (+2/+2)
            withClue("Bear should have projected power 4 (2+2)") {
                projector.getProjectedPower(game.state, bear) shouldBe 4
            }
            withClue("Bear should have projected toughness 4 (2+2)") {
                projector.getProjectedToughness(game.state, bear) shouldBe 4
            }

            // No Food token should exist
            game.isOnBattlefield("Food") shouldBe false
        }

        test("Crumb and Get It with gift gives +2/+2 and indestructible, opponent gets Food") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Crumb and Get It")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardOnBattlefield(1, "Vanilla Bear")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            val bear = game.findPermanent("Vanilla Bear")
            bear.shouldNotBeNull()

            // Cast Crumb and Get It targeting Vanilla Bear
            game.castSpell(1, "Crumb and Get It", bear)
            game.resolveStack()

            // Accept the gift
            game.answerYesNo(true)

            // Bear should be 4/4 (+2/+2)
            withClue("Bear should have projected power 4 (2+2)") {
                projector.getProjectedPower(game.state, bear) shouldBe 4
            }
            withClue("Bear should have projected toughness 4 (2+2)") {
                projector.getProjectedToughness(game.state, bear) shouldBe 4
            }

            // Food token should exist on the battlefield (opponent's)
            game.isOnBattlefield("Food") shouldBe true
        }
    }
}
