package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fdn.cards.BlasphemousEdict
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Blasphemous Edict {3}{B}{B} — Sorcery.
 *
 * "You may pay {B} rather than pay this spell's mana cost if there are thirteen or more creatures
 *  on the battlefield.
 *  Each player sacrifices thirteen creatures of their choice."
 *
 * Two things are worth pinning: the alternative cost is gated on a *global* creature count (both
 * players' creatures, not just the caster's), and the gate is enforced on the authorization path
 * too — not only by hiding the action from the enumerator.
 */
class BlasphemousEdictScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BlasphemousEdict))
        return driver
    }

    /** Put [count] vanilla 2/2s onto [player]'s battlefield. */
    fun fillBoard(driver: GameTestDriver, player: EntityId, count: Int): List<EntityId> =
        (1..count).map { driver.putCreatureOnBattlefield(player, "Black Creature") }

    /** True when the enumerator is currently offering the self-alternative cast of [cardId]. */
    fun altCastOffered(driver: GameTestDriver, player: EntityId, cardId: EntityId): Boolean =
        driver.legalActions(player).any { legal: LegalAction ->
            val action = legal.action
            action is CastSpell &&
                action.cardId == cardId &&
                action.useAlternativeCost &&
                action.alternativeCostType == AlternativeCostType.SELF_ALTERNATIVE
        }

    /** Answer every sacrifice-selection decision until the stack is empty. */
    fun resolveSacrifices(driver: GameTestDriver) {
        var safety = 0
        while (safety < 60) {
            val pending = driver.state.pendingDecision
            when {
                pending is SelectCardsDecision ->
                    driver.submitCardSelection(pending.playerId, pending.options.take(pending.minSelections))
                driver.stackSize > 0 -> driver.bothPass()
                else -> return
            }
            safety++
        }
    }

    test("fewer than thirteen creatures on the battlefield: the {B} alternative is not offered") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        fillBoard(driver, you, 6)
        fillBoard(driver, opponent, 6) // 12 total — one short

        driver.giveMana(you, Color.BLACK, 1)
        val edict = driver.putCardInHand(you, "Blasphemous Edict")

        altCastOffered(driver, you, edict) shouldBe false

        // …and the gate is enforced on authorization, not just in the enumerator.
        driver.submit(
            CastSpell(
                playerId = you,
                cardId = edict,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE
            )
        ).error shouldBe "Alternative cost is not available: ${BlasphemousEdict.script.selfAlternativeCost!!.condition!!.description}"
    }

    test("thirteen creatures across BOTH battlefields unlocks the {B} alternative") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        // Deliberately lopsided: neither player alone reaches thirteen.
        fillBoard(driver, you, 4)
        fillBoard(driver, opponent, 9)

        driver.giveMana(you, Color.BLACK, 1)
        val edict = driver.putCardInHand(you, "Blasphemous Edict")

        altCastOffered(driver, you, edict) shouldBe true

        driver.submit(
            CastSpell(
                playerId = you,
                cardId = edict,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE
            )
        ).error shouldBe null
        resolveSacrifices(driver)

        // Everyone controlled fewer than thirteen, so every creature goes (per the card's ruling).
        driver.getCreatures(you).size shouldBe 0
        driver.getCreatures(opponent).size shouldBe 0
    }

    test("each player sacrifices at most thirteen — a fourteenth creature survives") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        fillBoard(driver, you, 14)
        fillBoard(driver, opponent, 2)

        driver.giveMana(you, Color.BLACK, 5)
        val edict = driver.putCardInHand(you, "Blasphemous Edict")
        driver.castSpell(you, edict).error shouldBe null
        resolveSacrifices(driver)

        driver.getCreatures(you).size shouldBe 1  // 14 - 13
        driver.getCreatures(opponent).size shouldBe 0 // fewer than 13 — all of them
    }
})
