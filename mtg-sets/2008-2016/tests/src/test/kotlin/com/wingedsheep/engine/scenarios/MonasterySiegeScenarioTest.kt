package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.MonasterySiege
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Monastery Siege (FRF) — {2}{U} Enchantment. "As this enchantment enters, choose Khans or Dragons.
 * • Khans — At the beginning of your draw step, draw an additional card, then discard a card.
 * • Dragons — Spells your opponents cast that target you or a permanent you control cost {2} more
 * to cast."
 *
 * Each mode is checked on both settings, so a swapped gate fails: the loot fires only under Khans,
 * the tax applies only under Dragons.
 */
class MonasterySiegeScenarioTest : FunSpec({

    /** The caster is the active player; the Siege belongs to their opponent, set to [mode]. */
    fun setUp(mode: String): Triple<GameTestDriver, EntityId, EntityId> {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + MonasterySiege)
            initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        }
        val caster = d.activePlayer!!
        val owner = d.getOpponent(caster)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val siege = d.putPermanentOnBattlefield(owner, "Monastery Siege")
        d.addComponent(siege, CastChoicesComponent(chosen = mapOf(ChoiceSlot.MODE to ChoiceValue.TextChoice(mode))))
        return Triple(d, caster, owner)
    }

    test("Dragons: an opponent's Lightning Bolt at you costs {2} more") {
        val (d, caster, owner) = setUp("dragons")
        val bolt = d.putCardInHand(caster, "Lightning Bolt")
        d.giveMana(caster, Color.RED, 1)
        d.castSpell(caster, bolt, listOf(owner)).error shouldNotBe null
        d.giveMana(caster, Color.RED, 2)
        d.castSpell(caster, bolt, listOf(owner)).error shouldBe null
        d.bothPass()
        d.getLifeTotal(owner) shouldBe 17
    }

    test("Dragons: an opponent's spell at a creature you control costs {2} more") {
        val (d, caster, owner) = setUp("dragons")
        val courser = d.putCreatureOnBattlefield(owner, "Centaur Courser")
        val bolt = d.putCardInHand(caster, "Lightning Bolt")
        d.giveMana(caster, Color.RED, 1)
        d.castSpell(caster, bolt, listOf(courser)).error shouldNotBe null
    }

    test("Dragons: a spell the opponent aims at their own side is untaxed") {
        val (d, caster, _) = setUp("dragons")
        val bolt = d.putCardInHand(caster, "Lightning Bolt")
        d.giveMana(caster, Color.RED, 1)
        d.castSpell(caster, bolt, listOf(caster)).error shouldBe null
    }

    test("Khans: no tax on spells aimed at you") {
        val (d, caster, owner) = setUp("khans")
        val bolt = d.putCardInHand(caster, "Lightning Bolt")
        d.giveMana(caster, Color.RED, 1)
        d.castSpell(caster, bolt, listOf(owner)).error shouldBe null
    }

    test("Khans: at the beginning of your draw step, draw an additional card, then discard a card") {
        val (d, _, owner) = setUp("khans")
        d.passPriorityUntil(Step.DRAW, activePlayer = owner)
        val handAfterNormalDraw = d.getHandSize(owner)
        repeat(4) { if (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass() }

        d.pendingDecision shouldNotBe null
        d.submitCardSelection(owner, listOf(d.getHand(owner).first())).error shouldBe null
        d.getHandSize(owner) shouldBe handAfterNormalDraw
        d.getGraveyard(owner).size shouldBe 1
    }

    test("Dragons: no loot in your draw step") {
        val (d, _, owner) = setUp("dragons")
        d.passPriorityUntil(Step.DRAW, activePlayer = owner)
        d.state.stack.isEmpty() shouldBe true
        d.pendingDecision shouldBe null
        d.getGraveyard(owner).size shouldBe 0
    }
})
