package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.isd.cards.UndeadAlchemist
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.DamageCantBePrevented
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Undead Alchemist (ISD 84).
 *
 * "If a Zombie you control would deal combat damage to a player, instead that player mills that
 * many cards. Whenever a creature card is put into an opponent's graveyard from their library,
 * exile that card and create a 2/2 black Zombie creature token."
 */
class UndeadAlchemistScenarioTest : FunSpec({

    val leyline = card("Test Leyline") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        oracleText = "Damage can't be prevented."
        replacementEffect(DamageCantBePrevented())
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(UndeadAlchemist, leyline))
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingLife = 20)
        return d
    }

    fun GameTestDriver.zombieTokens(player: com.wingedsheep.sdk.model.EntityId) =
        getCreatures(player).count { getCardName(it)?.startsWith("Zombie") == true }

    fun GameTestDriver.attackUnblocked(attacker: com.wingedsheep.sdk.model.EntityId) {
        val p1 = activePlayer!!
        val p2 = getOpponent(p1)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(p1, listOf(attacker), p2)
        bothPass()
        declareNoBlockers(p2)
        passPriorityUntil(Step.POSTCOMBAT_MAIN)
    }

    test("a Zombie's combat damage to a player is replaced: no life lost, that player mills that many") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val alchemist = d.putPermanentOnBattlefield(p1, "Undead Alchemist")
        d.removeSummoningSickness(alchemist)
        val libBefore = d.state.getLibrary(p2).size

        d.attackUnblocked(alchemist)

        d.assertLifeTotal(p2, 20)
        (libBefore - d.state.getLibrary(p2).size) shouldBe 4
        d.getGraveyard(p2).size shouldBe 4
    }

    test("it is not prevention: combat damage that can't be prevented is still replaced with the mill") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val alchemist = d.putPermanentOnBattlefield(p1, "Undead Alchemist")
        d.removeSummoningSickness(alchemist)
        d.putPermanentOnBattlefield(p1, "Test Leyline")
        val libBefore = d.state.getLibrary(p2).size

        d.attackUnblocked(alchemist)

        d.assertLifeTotal(p2, 20)
        (libBefore - d.state.getLibrary(p2).size) shouldBe 4
    }

    test("a non-Zombie creature you control deals its combat damage normally") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(p1, "Undead Alchemist")
        val bears = d.putPermanentOnBattlefield(p1, "Grizzly Bears")
        d.removeSummoningSickness(bears)
        val libBefore = d.state.getLibrary(p2).size

        d.attackUnblocked(bears)

        d.assertLifeTotal(p2, 18)
        d.state.getLibrary(p2).size shouldBe libBefore
    }

    test("a creature card milled from an opponent's library is exiled and you create a 2/2 Zombie") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val alchemist = d.putPermanentOnBattlefield(p1, "Undead Alchemist")
        d.removeSummoningSickness(alchemist)
        d.putCardOnTopOfLibrary(p2, "Grizzly Bears")

        d.attackUnblocked(alchemist)

        d.getGraveyardCardNames(p2).contains("Grizzly Bears") shouldBe false
        d.getExileCardNames(p2) shouldBe listOf("Grizzly Bears")
        d.zombieTokens(p1) shouldBe 1
        d.getGraveyard(p2).size shouldBe 3
    }
})
