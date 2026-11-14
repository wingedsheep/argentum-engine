package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ori.cards.KytheonHeroOfAkros
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Kytheon, Hero of Akros (ORI #23) — "At end of combat, if Kytheon and at least two other
 * creatures attacked this combat, exile Kytheon, then return him to the battlefield transformed
 * under his owner's control."
 *
 * Ruling (2015-06-22): attackers that are no longer on the battlefield still count.
 */
class KytheonHeroOfAkrosScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + KytheonHeroOfAkros)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.ready(name: String, owner: EntityId = player1): EntityId =
        putCreatureOnBattlefield(owner, name).also { removeSummoningSickness(it) }

    fun GameTestDriver.attackWith(attackers: List<EntityId>) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, attackers.associateWith { player2 }).error shouldBe null
    }

    fun GameTestDriver.isGideon(id: EntityId): Boolean = getCardName(id) == "Gideon, Battle-Forged"

    test("Kytheon and two others attack — one dies in combat — and he flips into Gideon with 3 loyalty") {
        val d = driver()
        val kytheon = d.ready("Kytheon, Hero of Akros")
        val bear = d.ready("Grizzly Bears")
        val doomed = d.ready("Grizzly Bears")
        val wall = d.ready("Force of Nature", d.player2)

        d.attackWith(listOf(kytheon, bear, doomed))
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(d.player2, mapOf(wall to listOf(doomed))).error shouldBe null
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        d.state.getZone(ZoneKey(d.player1, Zone.GRAVEYARD)) shouldContain doomed
        val gideon = d.findPermanent(d.player1, "Gideon, Battle-Forged")!!
        d.isGideon(gideon) shouldBe true
        d.state.getEntity(gideon)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 3
        d.state.projectedState.isCreature(gideon) shouldBe false
    }

    test("Kytheon and only one other creature attacking is not enough") {
        val d = driver()
        val kytheon = d.ready("Kytheon, Hero of Akros")
        d.attackWith(listOf(kytheon, d.ready("Grizzly Bears")))
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        d.isGideon(kytheon) shouldBe false
        d.findPermanent(d.player1, "Kytheon, Hero of Akros") shouldBe kytheon
    }

    test("three other creatures attacking without Kytheon doesn't flip him") {
        val d = driver()
        val kytheon = d.ready("Kytheon, Hero of Akros")
        d.attackWith(List(3) { d.ready("Grizzly Bears") })
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        d.isGideon(kytheon) shouldBe false
        d.findPermanent(d.player1, "Gideon, Battle-Forged") shouldBe null
    }
})
