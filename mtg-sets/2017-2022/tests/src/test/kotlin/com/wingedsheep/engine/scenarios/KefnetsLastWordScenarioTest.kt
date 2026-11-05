package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.SkipUntapComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hou.cards.KefnetsLastWord
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kefnet's Last Word — "Gain control of target artifact, creature, or enchantment. Lands you
 * control don't untap during your next untap step."
 *
 * The control change outlasts the turn (2017-07-14 ruling), lands are not legal targets, the
 * caster's lands skip exactly their next untap step, and a spell whose target vanished fizzles
 * entirely — so the lands untap as normal (2017-07-14 ruling).
 */
class KefnetsLastWordScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(KefnetsLastWord))
        initMirrorMatch(Deck.of("Island" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castAt(me: EntityId, target: EntityId) =
        putCardInHand(me, "Kefnet's Last Word").let { spell ->
            giveMana(me, Color.BLUE, 4)
            castSpellWithTargets(me, spell, listOf(ChosenTarget.Permanent(target)))
        }

    for (name in listOf("Grizzly Bears", "Ornithopter", "Glorious Anthem")) {
        test("steals the opponent's $name for good") {
            val d = driver()
            val me = d.activePlayer!!
            val opp = d.getOpponent(me)
            val stolen = d.putPermanentOnBattlefield(opp, name)

            d.castAt(me, stolen).error shouldBe null
            d.bothPass().error shouldBe null
            d.state.projectedState.getController(stolen) shouldBe me

            d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
            d.passPriorityUntil(Step.PRECOMBAT_MAIN, me)
            withClue("the control change doesn't wear off at end of turn") {
                d.state.projectedState.getController(stolen) shouldBe me
            }
        }
    }

    test("a land is not a legal target") {
        val d = driver()
        val me = d.activePlayer!!
        val theirIsland = d.putPermanentOnBattlefield(d.getOpponent(me), "Island")
        d.castAt(me, theirIsland).error shouldNotBe null
    }

    test("the caster's lands skip their next untap step, then untap the one after") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val bear = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val myIsland = d.putPermanentOnBattlefield(me, "Island").also(d::tapPermanent)
        val theirIsland = d.putPermanentOnBattlefield(opp, "Island").also(d::tapPermanent)

        d.castAt(me, bear).error shouldBe null
        d.bothPass().error shouldBe null
        val lateIsland = d.putPermanentOnBattlefield(me, "Island").also(d::tapPermanent)

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        withClue("the opponent untaps normally") {
            d.isTapped(theirIsland) shouldBe false
            d.isTapped(myIsland) shouldBe true
        }

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, me)
        withClue("the caster's next untap step leaves every land they control tapped") {
            d.isTapped(myIsland) shouldBe true
            d.isTapped(lateIsland) shouldBe true
            d.state.getEntity(me)!!.has<SkipUntapComponent>() shouldBe false
        }

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, me)
        withClue("the following untap step untaps them") {
            d.isTapped(myIsland) shouldBe false
            d.isTapped(lateIsland) shouldBe false
        }
    }

    test("with its target gone the spell fizzles and the caster's lands untap as normal") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val bear = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val myIsland = d.putPermanentOnBattlefield(me, "Island").also(d::tapPermanent)

        d.castAt(me, bear).error shouldBe null
        d.moveToGraveyard(bear)
        d.bothPass().error shouldBe null

        d.state.getEntity(me)!!.has<SkipUntapComponent>() shouldBe false
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, me)
        d.isTapped(myIsland) shouldBe false
    }
})
