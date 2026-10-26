package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.ForetellCard
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Niko Defies Destiny (KHM #226) — Saga.
 *   I — You gain 2 life for each foretold card you own in exile.
 *   II — Add {W}{U}. Spend this mana only to foretell cards or cast spells that have foretell.
 *   III — Return target card with foretell from your graveyard to your hand.
 *
 * Walks the saga through all three chapters: two foretold cards make chapter I gain 4, chapter
 * II's mana pays a foretell's {2}, and chapter III returns only the card that has foretell.
 */
class NikoDefiesDestinyScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Advances to [p]'s next main phase; [target] answers the chapter ability's target prompt. */
    fun GameTestDriver.toMyNextMain(p: EntityId, target: EntityId? = null) {
        passPriorityUntil(Step.PRECOMBAT_MAIN, getOpponent(p))
        passPriorityUntil(Step.PRECOMBAT_MAIN, p)
        val prompt = state.pendingDecision as? ChooseTargetsDecision
        if (prompt != null) {
            prompt.legalTargets[0] shouldBe listOf(target)
            submitTargetSelection(p, listOf(target!!))
        }
        while (state.stack.isNotEmpty()) bothPass()
    }

    test("chapter I gains 2 life per foretold card, II's mana foretells, III returns a foretell card") {
        val d = driver()
        val p = d.activePlayer!!
        repeat(2) {
            val cup = d.putCardInHand(p, "Poison the Cup")
            d.giveMana(p, Color.BLACK, 2)
            d.submitSuccess(ForetellCard(p, cup))
        }
        val niko = d.putCardInHand(p, "Niko Defies Destiny")
        d.giveMana(p, Color.WHITE, 2)
        d.giveMana(p, Color.BLUE, 1)
        d.castSpell(p, niko).error shouldBe null
        d.bothPass() // the saga resolves; chapter I triggers
        d.bothPass()
        d.getLifeTotal(p) shouldBe 24

        d.toMyNextMain(p)
        val pool = d.state.getEntity(p)!!.get<ManaPoolComponent>()!!
        pool.restrictedMana.size shouldBe 2
        val third = d.putCardInHand(p, "Poison the Cup")
        d.submitSuccess(ForetellCard(p, third))
        d.getExile(p).contains(third) shouldBe true
        d.state.getEntity(p)!!.get<ManaPoolComponent>()!!.restrictedMana.size shouldBe 0

        val inGraveyard = d.putCardInGraveyard(p, "Poison the Cup")
        val bears = d.putCardInGraveyard(p, "Grizzly Bears")
        d.toMyNextMain(p, target = inGraveyard) // the Bears, without foretell, isn't a legal target
        d.state.getHand(p).contains(inGraveyard) shouldBe true
        d.state.getGraveyard(p).contains(bears) shouldBe true
    }
})
