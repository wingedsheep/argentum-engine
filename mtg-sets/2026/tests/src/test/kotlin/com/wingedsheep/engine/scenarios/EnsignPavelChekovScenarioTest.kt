package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.trk.cards.EnsignPavelChekov
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Ensign Pavel Chekov (TRK #57) — {3}{U} Legendary Creature — Human Pilot 1/5.
 *
 * "Strange new worlds — When Ensign Chekov enters, each opponent exiles the top X cards of their
 *  library, where X is the number of differently named lands you control. For each opponent, choose
 *  a land card that player owns exiled this way. You may play the chosen cards for as long as they
 *  remain exiled."
 *
 * Three seats: inside the per-opponent loop X must still count *Chekov's controller's* lands, the
 * land pick must be made by Chekov's controller from that opponent's own exiled cards, and only the
 * chosen land — not the rest of the exiled cards — becomes playable, for Chekov's controller.
 */
class EnsignPavelChekovScenarioTest : FunSpec({

    test("each opponent exiles X cards; Chekov's controller picks one land per opponent and may play it") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EnsignPavelChekov))
        val players = driver.initMultiplayer(
            decks = List(3) { Deck.of("Swamp" to 40) },
            skipMulligans = true,
            startingPlayer = 0,
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (you, firstOpp, secondOpp) = players

        // Three Islands and a Plains: four lands, but only two different names, so X = 2.
        repeat(3) { driver.putPermanentOnBattlefield(you, "Island") }
        driver.putPermanentOnBattlefield(you, "Plains")

        // First opponent: top two are Grizzly Bears and Forest — a single land to choose.
        val firstForest = driver.putCardOnTopOfLibrary(firstOpp, "Forest")
        val firstBears = driver.putCardOnTopOfLibrary(firstOpp, "Grizzly Bears")
        // Second opponent: top two are Mountain and Forest — a real choice.
        val secondForest = driver.putCardOnTopOfLibrary(secondOpp, "Forest")
        val secondMountain = driver.putCardOnTopOfLibrary(secondOpp, "Mountain")
        val firstOppLibraryBefore = driver.state.getZone(firstOpp, Zone.LIBRARY).size

        val chekov = driver.putCardInHand(you, "Ensign Pavel Chekov")
        driver.castSpell(you, chekov)

        val choicesOffered = mutableListOf<List<String?>>()
        var steps = 0
        while ((driver.state.stack.isNotEmpty() || driver.pendingDecision != null) && steps++ < 40) {
            val decision = driver.pendingDecision
            if (decision is SelectCardsDecision && secondMountain in decision.options) {
                withClue("the land pick belongs to Chekov's controller, not the visited opponent") {
                    decision.playerId shouldBe you
                }
                withClue("only that opponent's exiled lands are offered") {
                    decision.options shouldContainExactlyInAnyOrder listOf(secondMountain, secondForest)
                }
                choicesOffered += decision.options.map { driver.getCardName(it) }
                driver.submitCardSelection(you, listOf(secondMountain))
            } else if (decision != null) {
                driver.autoResolveDecision()
            } else {
                driver.passPriority(driver.state.priorityPlayerId!!)
            }
        }

        withClue("the second opponent's land choice was actually asked") {
            choicesOffered.size shouldBe 1
        }
        withClue("X counts differently named lands Chekov's controller controls: exactly two cards each") {
            driver.getExile(firstOpp) shouldContainExactlyInAnyOrder listOf(firstBears, firstForest)
            driver.getExile(secondOpp) shouldContainExactlyInAnyOrder listOf(secondMountain, secondForest)
            driver.state.getZone(firstOpp, Zone.LIBRARY).size shouldBe firstOppLibraryBefore - 2
        }

        val playable = driver.state.mayPlayPermissions.flatMap { it.cardIds }
        withClue("only the chosen land of each opponent is playable — not the nonland, not the unchosen land") {
            playable shouldContain firstForest
            playable shouldContain secondMountain
            playable shouldNotContain firstBears
            playable shouldNotContain secondForest
        }
        driver.state.mayPlayPermissions.filter { firstForest in it.cardIds || secondMountain in it.cardIds }
            .forEach { permission ->
                withClue("the permission is Chekov's controller's, for as long as the card remains exiled") {
                    permission.controllerId shouldBe you
                    permission.permanent shouldBe true
                }
            }

        withClue("Chekov's controller plays the chosen land straight out of the opponent's exile") {
            driver.playLand(you, secondMountain).error shouldBe null
            driver.getPermanents(you) shouldContain secondMountain
        }
    }
})
