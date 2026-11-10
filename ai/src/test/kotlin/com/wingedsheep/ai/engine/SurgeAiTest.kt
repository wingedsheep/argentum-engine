package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Surge (CR 702.117a) reaches the AI as its own `CastWithSurge` offer once a spell has been cast
 * this turn; the AI should take the cheaper cast that also turns on the card's surge rider.
 */
class SurgeAiTest : FunSpec({

    val surgeCreature = card("Surge AI Test Creature") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Goblin"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.surge("{1}{R}"))
        triggeredAbility {
            trigger = Triggers.self.enters()
            interveningIf = Conditions.SurgeCostWasPaid
            effect = Effects.GainLife(4)
        }
    }

    test("with another spell cast this turn, the AI casts the surge creature for its surge cost") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + surgeCreature)
        d.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = d.activePlayer!!
        val cardId = d.putCardInHand(player, surgeCreature.name)
        d.giveMana(player, Color.RED, 3)
        d.replaceState(d.state.copy(playerSpellsCastThisTurn = mapOf(player to 1)))

        val simulator = GameSimulator(d.services.cardRegistry)
        val offers = simulator.getLegalActions(d.state, player).filter { (it.action as? CastSpell)?.cardId == cardId }
        offers.map { it.actionType }.toSet() shouldBe setOf("CastSpell", "CastWithSurge")

        val chosen = AIPlayer.create(d.services.cardRegistry, player).chooseAction(d.state)
            .shouldBeInstanceOf<CastSpell>()
        chosen.cardId shouldBe cardId
        chosen.alternativeCostType shouldBe AlternativeCostType.SURGE
    }
})
