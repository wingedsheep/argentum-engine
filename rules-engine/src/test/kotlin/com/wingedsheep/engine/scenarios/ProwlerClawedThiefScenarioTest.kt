package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.DocOcksHenchmen
import com.wingedsheep.mtg.sets.definitions.spm.cards.ProwlerClawedThief
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Prowler, Clawed Thief (SPM #138) — "Whenever another Villain you control enters, Prowler connives."
 *
 * Pins the connive-on-Villain-enters trigger (which was entirely missing before the fix — only Menace
 * was implemented). Another Villain (Doc Ock's Henchmen) entering makes Prowler connive; discarding a
 * nonland to the connive grows Prowler with a +1/+1 counter.
 */
class ProwlerClawedThiefScenarioTest : FunSpec({

    test("another Villain entering makes Prowler connive (discard nonland → +1/+1 counter)") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ProwlerClawedThief)
        driver.registerCard(DocOcksHenchmen)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20, skipMulligans = true)
        val you = driver.player1
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val prowler = driver.putCreatureOnBattlefield(you, "Prowler, Clawed Thief")
        // The connive draw pulls the top card; the pre-placed hand card is the nonland we discard.
        driver.putCardOnTopOfLibrary(you, "Grizzly Bears")
        val toDiscard = driver.putCardInHand(you, "Grizzly Bears")

        // Cast another Villain you control — Doc Ock's Henchmen has Flash, {2}{U}.
        val henchmen = driver.putCardInHand(you, "Doc Ock's Henchmen")
        driver.giveMana(you, Color.BLUE, 3)
        driver.castSpell(you, henchmen)

        var guard = 0
        while (guard++ < 40 && (driver.isPaused || driver.state.stack.isNotEmpty())) {
            if (driver.isPaused) {
                when (val dec = driver.pendingDecision) {
                    is SelectCardsDecision ->
                        driver.submitDecision(dec.playerId, CardsSelectedResponse(dec.id, listOf(toDiscard)))
                    else -> error("unexpected decision resolving Prowler's connive: $dec")
                }
            } else {
                driver.bothPass()
            }
        }

        driver.state.getGraveyard(you).contains(toDiscard) shouldBe true
        val counters = driver.state.getEntity(prowler)?.get<CountersComponent>()
        (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 1
    }
})
