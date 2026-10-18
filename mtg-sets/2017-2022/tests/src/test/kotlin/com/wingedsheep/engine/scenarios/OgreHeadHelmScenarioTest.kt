package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Shatter
import com.wingedsheep.mtg.sets.definitions.neo.cards.OgreHeadHelm
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Ogre-Head Helm — {1}{R} Artifact Creature — Equipment Ogre 2/2
 * Equipped creature gets +2/+2.
 * Whenever this creature or equipped creature deals combat damage to a player, you may sacrifice it.
 * If you do, discard your hand, then draw three cards.
 * Reconfigure {3}
 *
 * On the attached trigger "it" is the creature that dealt the damage, not the Helm (2022-02-18
 * ruling) — and it stays that creature even when the Helm leaves before the trigger resolves.
 */
class OgreHeadHelmScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + OgreHeadHelm + Shatter)
        initMirrorMatch(deck = Deck.of("Mountain" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun settle(d: GameTestDriver, yes: Boolean) {
        var guard = 0
        while (guard++ < 20 && (d.state.stack.isNotEmpty() || d.pendingDecision != null)) {
            when (val decision = d.pendingDecision) {
                null -> d.bothPass()
                is YesNoDecision -> d.submitYesNo(decision.playerId, yes).error shouldBe null
                else -> error("unexpected decision $decision")
            }
        }
    }

    fun equip(d: GameTestDriver, helm: EntityId, creature: EntityId) {
        d.giveColorlessMana(d.player1, 3)
        d.submit(
            ActivateAbility(d.player1, helm, OgreHeadHelm.activatedAbilities.first().id, listOf(ChosenTarget.Permanent(creature)))
        ).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.projectedState.isCreature(helm) shouldBe false
        d.state.projectedState.getPower(creature) shouldBe 4
    }

    fun hitOpponent(d: GameTestDriver, attacker: EntityId) {
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(attacker), d.player2).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(d.player2)
        d.passPriorityUntil(Step.COMBAT_DAMAGE)
    }

    test("equipped creature connects: sacrifice that creature, discard the hand, draw three") {
        val d = driver()
        val me = d.player1
        val helm = d.putPermanentOnBattlefield(me, OgreHeadHelm.name)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.removeSummoningSickness(bears)
        equip(d, helm, bears)
        val oldHand = d.getHand(me)

        hitOpponent(d, bears)
        settle(d, yes = true)

        d.getLifeTotal(d.player2) shouldBe 16
        d.getGraveyard(me) shouldContain bears
        d.findPermanent(me, OgreHeadHelm.name) shouldBe helm
        d.getHandSize(me) shouldBe 3
        oldHand.forEach { d.getHand(me) shouldNotContain it }
    }

    test("the equipped creature is still the one sacrificed after the Helm is destroyed in response") {
        val d = driver()
        val me = d.player1
        val helm = d.putPermanentOnBattlefield(me, OgreHeadHelm.name)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.removeSummoningSickness(bears)
        equip(d, helm, bears)
        val shatter = d.putCardInHand(me, Shatter.name)

        hitOpponent(d, bears)
        d.state.stack.size shouldBe 1
        d.giveMana(me, Color.RED, 2)
        d.castSpell(me, shatter, listOf(helm)).outcome shouldBe Outcome.Done
        settle(d, yes = true)

        d.getGraveyard(me) shouldContain helm
        d.getGraveyard(me) shouldContain bears
        d.getHandSize(me) shouldBe 3
    }

    test("unattached Helm connects: sacrifice the Helm itself") {
        val d = driver()
        val me = d.player1
        val helm = d.putPermanentOnBattlefield(me, OgreHeadHelm.name)
        d.removeSummoningSickness(helm)

        hitOpponent(d, helm)
        settle(d, yes = true)

        d.getLifeTotal(d.player2) shouldBe 18
        d.getGraveyard(me) shouldContain helm
        d.getHandSize(me) shouldBe 3
    }

    test("declining keeps the creature and the hand") {
        val d = driver()
        val me = d.player1
        val helm = d.putPermanentOnBattlefield(me, OgreHeadHelm.name)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.removeSummoningSickness(bears)
        equip(d, helm, bears)
        val oldHand = d.getHand(me)

        hitOpponent(d, bears)
        settle(d, yes = false)

        d.findPermanent(me, "Grizzly Bears") shouldBe bears
        d.getHand(me) shouldBe oldHand
    }
})
