package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.rtr.cards.EssenceBacklash
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Essence Backlash (RTR #160): "Counter target creature spell. Essence Backlash deals damage equal to
 * that spell's power to its controller."
 *
 * Pins that the amount is read off the creature *spell* on the stack — its power, not its toughness or
 * mana value — and lands on that spell's controller, not Essence Backlash's.
 */
class EssenceBacklashScenarioTest : FunSpec({

    /** A 4/2 for {3}: power, toughness and mana value all differ. */
    val brute = card("Backlash Test Brute") {
        manaCost = "{3}"
        typeLine = "Creature — Giant"
        power = 4
        toughness = 2
    }

    val cards = TestCards.all + listOf(EssenceBacklash, brute)

    test("counters the creature spell and deals its power in damage to its controller") {
        val d = GameTestDriver()
        d.registerCards(cards)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bruteCard = d.putCardInHand(d.player1, "Backlash Test Brute")
        d.giveColorlessMana(d.player1, 3)
        d.castSpell(d.player1, bruteCard).error shouldBe null
        d.passPriority(d.player1).error shouldBe null

        val backlash = d.putCardInHand(d.player2, "Essence Backlash")
        d.giveColorlessMana(d.player2, 2)
        d.giveMana(d.player2, Color.BLUE, 1)
        d.giveMana(d.player2, Color.RED, 1)
        d.castSpellWithTargets(d.player2, backlash, listOf(ChosenTarget.Spell(bruteCard))).error shouldBe null
        d.bothPass().error shouldBe null

        withClue("the creature spell is countered into its owner's graveyard") {
            d.state.getZone(ZoneKey(d.player1, Zone.GRAVEYARD)).contains(bruteCard) shouldBe true
            d.state.getBattlefield().contains(bruteCard) shouldBe false
        }
        withClue("4 damage — the spell's power — to the spell's controller") {
            d.getLifeTotal(d.player1) shouldBe 16
        }
        withClue("Essence Backlash's controller takes nothing") {
            d.getLifeTotal(d.player2) shouldBe 20
        }
    }
})
