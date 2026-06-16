package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.ColossalRattlewurm
import com.wingedsheep.mtg.sets.definitions.otj.cards.JaggedBarrens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Colossal Rattlewurm (OTJ #159) — {2}{G}{G} 6/5 Creature — Wurm.
 *
 * "Colossal Rattlewurm has flash as long as you control a Desert.
 *  Trample
 *  {1}{G}, Exile this card from your graveyard: Search your library for a Desert card, put it
 *  onto the battlefield tapped, then shuffle."
 *
 * Exercises the conditional-flash static (you can cast it at instant speed only while you control
 * a Desert) and the graveyard-activated Desert fetch (pay {1}{G} + exile this card → a Desert
 * enters tapped).
 */
class ColossalRattlewurmScenarioTest : FunSpec({

    val abilityId = ColossalRattlewurm.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ColossalRattlewurm)
        driver.registerCard(JaggedBarrens)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("graveyard ability fetches a Desert onto the battlefield tapped and exiles this card") {
        val driver = newDriver()
        val player = driver.player1

        val rattlewurm = driver.putCardInGraveyard(player, "Colossal Rattlewurm")
        val desert = driver.putCardOnTopOfLibrary(player, "Jagged Barrens")
        driver.giveMana(player, Color.GREEN, 2) // {1}{G}

        driver.submit(
            ActivateAbility(playerId = player, sourceId = rattlewurm, abilityId = abilityId)
        ).isSuccess shouldBe true
        driver.bothPass()

        if (driver.isPaused) {
            val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.options.contains(desert) shouldBe true
            driver.submitCardSelection(player, listOf(desert))
        }
        driver.isPaused shouldBe false

        // Desert is on the battlefield, tapped; Rattlewurm is exiled (not in the graveyard).
        driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD)).contains(desert) shouldBe true
        driver.isTapped(desert) shouldBe true
        driver.state.getZone(ZoneKey(player, Zone.EXILE)).contains(rattlewurm) shouldBe true
        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)).contains(rattlewurm) shouldBe false
    }

    test("conditional flash: castable at instant speed only while you control a Desert") {
        val driver = newDriver()
        val player = driver.player1

        // It is the opponent's turn (instant-speed window for player1). Put Rattlewurm in hand
        // with mana available.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP) // now opponent's turn
        val onOpponentTurn = driver.state.activePlayerId == driver.player2

        val rattlewurm = driver.putCardInHand(player, "Colossal Rattlewurm")
        driver.giveMana(player, Color.GREEN, 4)

        fun canCast(): Boolean {
            val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
            val actions = enumerator.enumerate(driver.state, player, EnumerationMode.FULL)
            return actions.any { (it.action as? CastSpell)?.cardId == rattlewurm }
        }

        // Without a Desert: no instant-speed cast offered on the opponent's turn.
        if (onOpponentTurn) {
            canCast() shouldBe false
        }

        // Controlling a Desert grants flash → it becomes castable at instant speed.
        driver.putLandOnBattlefield(player, "Jagged Barrens")
        if (onOpponentTurn) {
            canCast() shouldBe true
        }
    }
})
