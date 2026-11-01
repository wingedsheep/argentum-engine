package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.war.cards.DavrielRogueShadowmage
import com.wingedsheep.mtg.sets.definitions.war.cards.VraskaSwarmsEminence
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Vraska, Swarm's Eminence {2}{B/G}{B/G} — WAR #236.
 *
 * Pins the observer trigger putting its counter on the damage *source* (not the recipient), for
 * damage to a player and to a planeswalker, gated on deathtouch; and the −2 token's own
 * "deals damage to a planeswalker, destroy that planeswalker" trigger.
 */
class VraskaSwarmsEminenceScenarioTest : FunSpec({

    val minusTwo = VraskaSwarmsEminence.script.activatedAbilities[0].id

    data class Board(val d: GameTestDriver, val you: EntityId, val opp: EntityId, val vraska: EntityId)

    fun setup(): Board {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(VraskaSwarmsEminence, DavrielRogueShadowmage))
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = d.activePlayer!!
        val vraska = d.putPermanentOnBattlefield(you, "Vraska, Swarm's Eminence").also {
            d.addComponent(it, CountersComponent(mapOf(CounterType.LOYALTY to 5)))
        }
        return Board(d, you, d.getOpponent(you), vraska)
    }

    fun GameTestDriver.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.attackUnblocked(attacker: EntityId, attackerController: EntityId, defender: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(attackerController, mapOf(attacker to defender))
        bothPass()
        declareNoBlockers(getOpponent(attackerController))
        passPriorityUntil(Step.POSTCOMBAT_MAIN)
    }

    test("a deathtouch creature that damages a player gets a +1/+1 counter") {
        val (d, you, opp, _) = setup()
        val rat = d.putCreatureOnBattlefield(you, "Deathtouch Rat")
        d.removeSummoningSickness(rat)

        d.attackUnblocked(rat, you, opp)

        d.getLifeTotal(opp) shouldBe 19
        d.plusOnes(rat) shouldBe 1
    }

    test("a creature without deathtouch gets no counter") {
        val (d, you, opp, _) = setup()
        val bears = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.removeSummoningSickness(bears)

        d.attackUnblocked(bears, you, opp)

        d.getLifeTotal(opp) shouldBe 18
        d.plusOnes(bears) shouldBe 0
    }

    test("−2 Assassin destroys the planeswalker it damages and gets Vraska's counter") {
        val (d, you, opp, vraska) = setup()
        val davriel = d.putPermanentOnBattlefield(opp, "Davriel, Rogue Shadowmage").also {
            d.addComponent(it, CountersComponent(mapOf(CounterType.LOYALTY to 3)))
        }

        d.submitSuccess(ActivateAbility(you, vraska, minusTwo))
        d.bothPass()
        d.state.getEntity(vraska)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 3

        val assassin = d.getCreatures(you).single()
        d.state.projectedState.hasKeyword(assassin, Keyword.DEATHTOUCH) shouldBe true
        d.state.projectedState.getPower(assassin) shouldBe 1
        d.state.projectedState.getToughness(assassin) shouldBe 1
        d.removeSummoningSickness(assassin)

        d.attackUnblocked(assassin, you, davriel)

        d.findPermanent(opp, "Davriel, Rogue Shadowmage") shouldBe null
        d.findPermanent(you, "Vraska, Swarm's Eminence") shouldNotBe null
        d.plusOnes(assassin) shouldBe 1
        d.getLifeTotal(opp) shouldBe 20
    }
})
