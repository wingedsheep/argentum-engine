package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.SkipUntapComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hou.cards.BontusLastReckoning
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Bontu's Last Reckoning — "Destroy all creatures. Lands you control don't untap during your next
 * untap step."
 *
 * The untap clause holds back every land the caster controls at their next untap step — including
 * one that entered after the spell resolved (2017-07-14 ruling) — leaves the opponent's untap step
 * alone, and wears off after that one step.
 */
class BontusLastReckoningScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(BontusLastReckoning))
        initMirrorMatch(Deck.of("Swamp" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("destroys every creature, then the caster's lands skip only their next untap step") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val myBear = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val theirBear = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val mySwamp = d.putPermanentOnBattlefield(me, "Swamp").also(d::tapPermanent)
        val theirSwamp = d.putPermanentOnBattlefield(opp, "Swamp").also(d::tapPermanent)

        val spell = d.putCardInHand(me, "Bontu's Last Reckoning")
        d.giveMana(me, Color.BLACK, 3)
        d.castSpell(me, spell).error shouldBe null
        d.bothPass().error shouldBe null

        withClue("all creatures are destroyed") {
            d.findPermanent(me, "Grizzly Bears") shouldBe null
            d.findPermanent(opp, "Grizzly Bears") shouldBe null
            d.getGraveyard(me).contains(myBear) shouldBe true
            d.getGraveyard(opp).contains(theirBear) shouldBe true
        }

        // A land that arrives after resolution is caught too: the filter is read at the untap step.
        val lateSwamp = d.putPermanentOnBattlefield(me, "Swamp").also(d::tapPermanent)

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        withClue("the opponent's untap step is untouched, and doesn't consume the caster's marker") {
            d.isTapped(theirSwamp) shouldBe false
            d.isTapped(mySwamp) shouldBe true
            d.state.getEntity(me)!!.has<SkipUntapComponent>() shouldBe true
        }

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, me)
        withClue("the caster's next untap step leaves all their lands tapped") {
            d.isTapped(mySwamp) shouldBe true
            d.isTapped(lateSwamp) shouldBe true
            d.state.getEntity(me)!!.has<SkipUntapComponent>() shouldBe false
        }

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, me)
        withClue("the untap step after that one untaps them") {
            d.isTapped(mySwamp) shouldBe false
            d.isTapped(lateSwamp) shouldBe false
        }
    }
})
