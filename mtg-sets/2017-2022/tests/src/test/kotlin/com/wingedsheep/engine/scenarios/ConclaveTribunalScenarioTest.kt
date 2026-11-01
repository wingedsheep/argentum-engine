package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Conclave Tribunal (GRN #6) — {3}{W} Enchantment.
 *
 *   Convoke
 *   When this enchantment enters, exile target nonland permanent an opponent controls until this
 *   enchantment leaves the battlefield.
 */
class ConclaveTribunalScenarioTest : ScenarioTestBase() {

    init {
        val removeEnchantment = card("Test Unravel") {
            manaCost = "{0}"
            typeLine = "Sorcery"
            spell {
                val e = target(TargetFilter.Enchantment)
                effect = Effects.Destroy(e)
            }
        }
        cardRegistry.register(listOf(removeEnchantment))

        test("fully convoked: exiles the opponent's permanent until Tribunal leaves") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Conclave Tribunal")
                .withCardInHand(1, "Test Unravel")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Savannah Lions")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanents("Grizzly Bears")
            val lions = game.findPermanent("Savannah Lions")!!
            val giant = game.findPermanent("Hill Giant")!!

            val cast = game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Conclave Tribunal").single(),
                    alternativePayment = AlternativePaymentChoice(
                        convokedCreatures = bears.associateWith { ConvokePayment(color = null) } +
                            (lions to ConvokePayment(color = Color.WHITE))
                    )
                )
            )
            withClue("three Bears pay {3}, Savannah Lions pays {W}: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()
            if (game.hasPendingDecision()) {
                game.selectTargets(listOf(giant))
                game.resolveStack()
            }

            game.isInExile(2, "Hill Giant") shouldBe true
            val tribunal = game.findPermanent("Conclave Tribunal")!!

            game.castSpell(1, "Test Unravel", targetId = tribunal).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Conclave Tribunal") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
        }
    }
}
