package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.SkipUntapComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hou.cards.OketrasLastMercy
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Oketra's Last Mercy — "Your life total becomes equal to your starting life total. Lands you
 * control don't untap during your next untap step."
 *
 * The life total moves to the starting total from either side — gaining from below, losing from
 * above (2017-07-14 ruling) — and the caster's lands, including one played after resolution, stay
 * tapped through their next untap step only.
 */
class OketrasLastMercyScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(OketrasLastMercy))
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castMercy(me: EntityId) {
        val spell = putCardInHand(me, "Oketra's Last Mercy")
        giveMana(me, Color.WHITE, 3)
        castSpell(me, spell).error shouldBe null
        bothPass().error shouldBe null
    }

    for ((before, label) in listOf(4 to "raised", 25 to "lowered")) {
        test("a life total of $before is $label to the starting 20; the opponent's is untouched") {
            val d = driver()
            val me = d.activePlayer!!
            val opp = d.getOpponent(me)
            d.setLifeTotal(me, before)
            d.setLifeTotal(opp, 7)

            d.castMercy(me)

            d.getLifeTotal(me) shouldBe 20
            d.getLifeTotal(opp) shouldBe 7
        }
    }

    test("the caster's lands skip their next untap step, then untap the one after") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val myPlains = d.putPermanentOnBattlefield(me, "Plains").also(d::tapPermanent)
        val theirPlains = d.putPermanentOnBattlefield(opp, "Plains").also(d::tapPermanent)

        d.castMercy(me)
        val latePlains = d.putPermanentOnBattlefield(me, "Plains").also(d::tapPermanent)

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        withClue("the opponent untaps normally") {
            d.isTapped(theirPlains) shouldBe false
            d.isTapped(myPlains) shouldBe true
        }

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, me)
        withClue("the caster's next untap step leaves every land they control tapped") {
            d.isTapped(myPlains) shouldBe true
            d.isTapped(latePlains) shouldBe true
            d.state.getEntity(me)!!.has<SkipUntapComponent>() shouldBe false
        }

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, me)
        withClue("the following untap step untaps them") {
            d.isTapped(myPlains) shouldBe false
            d.isTapped(latePlains) shouldBe false
        }
    }
})
