package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lrw.cards.FlamekinBladewhirl
import com.wingedsheep.mtg.sets.definitions.lrw.cards.FlamekinSpitfire
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * The AI must name the revealed card when it takes a "reveal a card from your hand" leg. Before,
 * `withAutomaticPayments` had no `RevealCard` branch, so the cast went out without
 * `revealedCards`, the processor refused it, and the AI passed — an engine-vs-engine LRW game sat
 * on Flamekin Bladewhirl with an Elemental in hand (2026-10-12 logs, game 1).
 */
class RevealCostPaymentAiTest : FunSpec({
    test("AI casts Flamekin Bladewhirl by revealing the Elemental in its hand") {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all + listOf(FlamekinBladewhirl, FlamekinSpitfire))
            initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val player = driver.player1
        driver.putLandOnBattlefield(player, "Mountain")
        val bladewhirl = driver.putCardInHand(player, "Flamekin Bladewhirl")
        val elemental = driver.putCardInHand(player, "Flamekin Spitfire")
        val legal = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, player)
            .single { action ->
                (action.action as? CastSpell)?.cardId == bladewhirl &&
                    action.additionalCostInfo?.costType == "RevealCard"
            }

        val chosen = AIPlayer.create(driver.cardRegistry, player).chooseFrom(driver.state, listOf(legal)).action
            as CastSpell
        chosen.additionalCostPayment?.revealedCards shouldBe listOf(elemental)
        driver.submit(chosen).outcome shouldBe Outcome.Done
        driver.state.getZone(player, Zone.HAND) shouldContain elemental
    }
})
