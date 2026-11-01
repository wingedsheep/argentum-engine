package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import io.kotest.matchers.shouldBe

/**
 * Sprouting Renewal (GRN #145) — {2}{G} Sorcery.
 *
 *   Convoke
 *   Choose one —
 *   • Create a 2/2 green and white Elf Knight creature token with vigilance.
 *   • Destroy target artifact or enchantment.
 */
class SproutingRenewalScenarioTest : ScenarioTestBase() {

    init {
        test("mode one creates a 2/2 Elf Knight with vigilance") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Sprouting Renewal")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellWithMode(1, "Sprouting Renewal", 0).error shouldBe null
            game.resolveStack()

            val knight = game.findPermanents("Elf Knight Token").single()
            game.state.projectedState.getPower(knight) shouldBe 2
            game.state.projectedState.getToughness(knight) shouldBe 2
            game.state.projectedState.hasKeyword(knight, Keyword.VIGILANCE) shouldBe true
        }

        test("mode two, fully convoked, destroys the opponent's enchantment") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Sprouting Renewal")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Glorious Anthem")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanents("Grizzly Bears")
            val target = listOf(ChosenTarget.Permanent(game.findPermanent("Glorious Anthem")!!))
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Sprouting Renewal").single(),
                    targets = target,
                    chosenModes = listOf(1),
                    modeTargetsOrdered = listOf(target),
                    alternativePayment = AlternativePaymentChoice(
                        convokedCreatures = bears.drop(1).associateWith { ConvokePayment(color = null) } +
                            (bears.first() to ConvokePayment(color = Color.GREEN))
                    )
                )
            ).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Glorious Anthem") shouldBe false
            game.isInGraveyard(2, "Glorious Anthem") shouldBe true
        }
    }
}
