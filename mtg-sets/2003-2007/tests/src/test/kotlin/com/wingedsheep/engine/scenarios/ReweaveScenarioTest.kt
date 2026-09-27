package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.Reweave
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Reweave (CHK #82) — "Target permanent's controller sacrifices it. If the player does, they
 * reveal cards from the top of their library until they reveal a permanent card that shares a
 * card type with the sacrificed permanent, put that card onto the battlefield, then shuffle."
 *
 * The reveal runs against the *target's controller's* library and the found card enters under
 * that player's control, so the main test aims Reweave at an opponent's permanent.
 */
class ReweaveScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + Reweave)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.fundReweave(player: EntityId) {
        giveMana(player, Color.BLUE, 1)
        giveColorlessMana(player, 5)
    }

    fun GameTestDriver.library(player: EntityId): List<EntityId> =
        state.getZone(ZoneKey(player, Zone.LIBRARY))

    fun GameTestDriver.battlefieldOf(player: EntityId): List<EntityId> =
        state.getZone(ZoneKey(player, Zone.BATTLEFIELD))

    test("the opponent sacrifices their creature and the first creature card they reveal enters under their control") {
        val d = driver()
        val victim = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        // Opponent's library top: a Swamp (not a creature), then Centaur Courser.
        val courser = d.putCardOnTopOfLibrary(d.player2, "Centaur Courser")
        val swamp = d.putCardOnTopOfLibrary(d.player2, "Swamp")
        val librarySizeBefore = d.library(d.player2).size

        val reweave = d.putCardInHand(d.player1, "Reweave")
        d.fundReweave(d.player1)
        d.castSpell(d.player1, reweave, listOf(victim)).error shouldBe null
        d.bothPass()

        withClue("the target was sacrificed to its owner's graveyard") {
            d.getGraveyard(d.player2) shouldContain victim
        }
        withClue("the first creature card revealed enters under the target controller's control") {
            d.getCreatures(d.player2) shouldBe listOf(courser)
            d.getCreatures(d.player1) shouldBe emptyList()
        }
        withClue("the skipped Swamp is shuffled back — only the found card left the library") {
            d.library(d.player2) shouldContain swamp
            d.library(d.player2).size shouldBe librarySizeBefore - 1
        }
    }

    test("a sacrificed land finds a land card, skipping creature cards on the way") {
        val d = driver()
        val land = d.putPermanentOnBattlefield(d.player2, "Swamp")
        // Top: Grizzly Bears (shares no card type with a land), then the deck's Swamps.
        val bears = d.putCardOnTopOfLibrary(d.player2, "Grizzly Bears")
        val nextSwamp = d.library(d.player2)[1]

        val reweave = d.putCardInHand(d.player1, "Reweave")
        d.fundReweave(d.player1)
        d.castSpell(d.player1, reweave, listOf(land)).error shouldBe null
        d.bothPass()

        withClue("the targeted land was sacrificed") {
            d.getGraveyard(d.player2) shouldContain land
        }
        withClue("the first land card revealed entered; the creature card stayed in the library") {
            d.battlefieldOf(d.player2) shouldContain nextSwamp
            d.battlefieldOf(d.player2) shouldNotContain bears
            d.library(d.player2) shouldContain bears
            d.getCreatures(d.player2) shouldBe emptyList()
        }
    }

    test("with no matching card the whole library is revealed and nothing enters") {
        val d = driver()
        val victim = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val librarySizeBefore = d.library(d.player2).size

        val reweave = d.putCardInHand(d.player1, "Reweave")
        d.fundReweave(d.player1)
        d.castSpell(d.player1, reweave, listOf(victim)).error shouldBe null
        d.bothPass()

        d.getGraveyard(d.player2) shouldContain victim
        d.getCreatures(d.player2) shouldBe emptyList()
        withClue("the library is untouched apart from the shuffle") {
            d.library(d.player2).size shouldBe librarySizeBefore
        }
    }
})
