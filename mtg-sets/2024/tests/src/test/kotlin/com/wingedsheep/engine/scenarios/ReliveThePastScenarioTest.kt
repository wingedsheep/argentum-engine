package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.exo.cards.SoulWarden
import com.wingedsheep.mtg.sets.definitions.lea.cards.SolRing
import com.wingedsheep.mtg.sets.definitions.mkm.cards.ReliveThePast
import com.wingedsheep.mtg.sets.definitions.usg.cards.GloriousAnthem
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Relive the Past — "Return up to one target artifact card, up to one target land card, and up to
 * one target non-Aura enchantment card from your graveyard to the battlefield. They are 5/5
 * Elemental creatures in addition to their other types."
 *
 * Pins that all three return together already 5/5 Elemental creatures, so a "creature enters"
 * trigger sees each of them.
 */
class ReliveThePastScenarioTest : FunSpec({

    test("artifact, land and enchantment return as 5/5 Elemental creatures, seen entering as creatures") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + listOf(ReliveThePast, SoulWarden, SolRing, GloriousAnthem))
            initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Soul Warden")
        val ring = d.putCardInGraveyard(me, "Sol Ring")
        val forest = d.putCardInGraveyard(me, "Forest")
        val anthem = d.putCardInGraveyard(me, "Glorious Anthem")
        val life = d.getLifeTotal(me)

        val relive = d.putCardInHand(me, "Relive the Past")
        d.giveMana(me, Color.GREEN, 6)
        d.giveMana(me, Color.WHITE, 1)
        d.submit(
            CastSpell(
                playerId = me,
                cardId = relive,
                targets = listOf(ring, forest, anthem).map { ChosenTarget.Card(it, me, Zone.GRAVEYARD) },
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        d.bothPass()
        repeat(4) { if (d.state.stack.isNotEmpty()) d.bothPass() }

        val projected = d.state.projectedState
        listOf(ring, forest, anthem).forEach {
            (it in d.state.getBattlefield()) shouldBe true
            projected.isCreature(it) shouldBe true
            projected.getSubtypes(it) shouldContain "Elemental"
            // 5/5, plus Glorious Anthem's +1/+1.
            projected.getPower(it) shouldBe 6
        }
        projected.getSubtypes(forest) shouldContain "Forest"
        d.getLifeTotal(me) shouldBe life + 3
    }
})
