package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh1.cards.FallenShinobi
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Fallen Shinobi — {3}{U}{B} 5/4, Ninjutsu {2}{U}{B}.
 * Whenever this creature deals combat damage to a player, that player exiles the top two cards of
 * their library. Until end of turn, you may play those cards without paying their mana costs.
 *
 * Pins that the exile comes off the *damaged* player's library, that the Shinobi's controller
 * (not the card owner) gets the free play — a spell at {0} and a land drop — and that the
 * permission ends with the turn.
 */
class FallenShinobiScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + FallenShinobi)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.connectWithShinobi() {
        val shinobi = putPermanentOnBattlefield(player1, "Fallen Shinobi")
        removeSummoningSickness(shinobi)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, listOf(shinobi), player2).outcome shouldBe Outcome.Done
        bothPass()
        declareNoBlockers(player2)
        var guard = 0
        while (guard++ < 30 && (state.stack.isNotEmpty() || state.step != Step.POSTCOMBAT_MAIN)) bothPass()
    }

    test("the damaged player exiles their top two and you may play them for free this turn") {
        val d = createDriver()
        val thirdCard = d.putCardOnTopOfLibrary(d.player2, "Grizzly Bears")
        val forest = d.putCardOnTopOfLibrary(d.player2, "Forest")
        val hillGiant = d.putCardOnTopOfLibrary(d.player2, "Hill Giant")

        d.connectWithShinobi()

        d.getLifeTotal(d.player2) shouldBe 15
        d.getExile(d.player2).toSet() shouldBe setOf(forest, hillGiant)
        d.state.getZone(ZoneKey(d.player2, Zone.LIBRARY)).first() shouldBe thirdCard

        val actions = d.legalActions(d.player1)
        actions.any { (it.action as? PlayLand)?.cardId == forest } shouldBe true
        val cast = actions.single { (it.action as? CastSpell)?.cardId == hillGiant }
        cast.affordable shouldBe true
        cast.manaCostString shouldBe "{0}"
        d.legalActions(d.player2).any { (it.action as? CastSpell)?.cardId == hillGiant } shouldBe false

        d.submit(
            CastSpell(playerId = d.player1, cardId = hillGiant, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        var guard = 0
        while (d.state.stack.isNotEmpty() && guard++ < 10) d.bothPass()
        d.findPermanent(d.player1, "Hill Giant") shouldBe hillGiant
    }

    test("cards left unplayed stay exiled and can't be played after the turn ends") {
        val d = createDriver()
        d.putCardOnTopOfLibrary(d.player2, "Forest")
        val hillGiant = d.putCardOnTopOfLibrary(d.player2, "Hill Giant")

        d.connectWithShinobi()
        d.legalActions(d.player1).any { (it.action as? CastSpell)?.cardId == hillGiant } shouldBe true

        d.passPriorityUntil(Step.END, maxPasses = 300)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)

        d.state.activePlayerId shouldBe d.player2
        d.getExile(d.player2).contains(hillGiant) shouldBe true
        d.legalActions(d.player1).map { (it.action as? CastSpell)?.cardId } shouldNotContain hillGiant
    }
})
