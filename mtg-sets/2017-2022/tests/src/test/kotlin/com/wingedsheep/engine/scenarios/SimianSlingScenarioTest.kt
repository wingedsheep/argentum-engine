package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.SimianSling
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Simian Sling — {R} Artifact Creature — Equipment Monkey 1/1
 * Equipped creature gets +1/+1.
 * Whenever this creature or equipped creature becomes blocked, it deals 1 damage to defending player.
 * Reconfigure {2}
 *
 * On the attached trigger the Sling is not attacking, so "defending player" must be read off the
 * equipped (blocked) creature, which is also the damage source (2022-02-18 ruling).
 */
class SimianSlingScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + SimianSling)
        initMirrorMatch(deck = Deck.of("Mountain" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun resolveStack(d: GameTestDriver) {
        var guard = 0
        while (guard++ < 20 && d.state.stack.isNotEmpty()) d.bothPass()
    }

    fun attackAndBlock(d: GameTestDriver, attacker: EntityId, blocker: EntityId?) {
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(attacker), d.player2).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        if (blocker == null) d.declareNoBlockers(d.player2)
        else d.declareBlockers(d.player2, mapOf(blocker to listOf(attacker))).outcome shouldBe Outcome.Done
        resolveStack(d)
    }

    test("equipped creature becomes blocked: defending player takes 1") {
        val d = driver()
        val me = d.player1
        val sling = d.putPermanentOnBattlefield(me, SimianSling.name)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.removeSummoningSickness(bears)
        val wall = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")

        d.giveColorlessMana(me, 2)
        d.submit(
            ActivateAbility(me, sling, SimianSling.activatedAbilities.first().id, listOf(ChosenTarget.Permanent(bears)))
        ).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.projectedState.isCreature(sling) shouldBe false
        d.state.projectedState.getPower(bears) shouldBe 3

        attackAndBlock(d, bears, wall)

        d.getLifeTotal(d.player2) shouldBe 19
        d.getLifeTotal(me) shouldBe 20
    }

    test("unattached Sling becomes blocked: defending player takes 1") {
        val d = driver()
        val sling = d.putPermanentOnBattlefield(d.player1, SimianSling.name)
        d.removeSummoningSickness(sling)
        val wall = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")

        attackAndBlock(d, sling, wall)

        d.getLifeTotal(d.player2) shouldBe 19
    }

    test("no trigger when the attacker is unblocked") {
        val d = driver()
        val sling = d.putPermanentOnBattlefield(d.player1, SimianSling.name)
        d.removeSummoningSickness(sling)

        attackAndBlock(d, sling, null)

        d.getLifeTotal(d.player2) shouldBe 20
    }
})
