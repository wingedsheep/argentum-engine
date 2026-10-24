package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fut.cards.GrinningIgnus
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Grinning Ignus (FUT #116) — "{R}, Return this creature to its owner's hand: Add {C}{C}{R}.
 * Activate only as a sorcery."
 *
 * The card's loop: bounce it for {C}{C}{R} at sorcery speed, then recast it from that mana — a
 * net-zero cycle that only works because the ability is a mana ability (no stack, CR 605.3b) yet is
 * limited to sorcery timing (CR 602.5d), so it is offered only in its controller's main phase.
 */
class GrinningIgnusScenarioTest : FunSpec({

    val abilityId = GrinningIgnus.activatedAbilities.single().id

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + GrinningIgnus)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("bounce for {C}{C}{R} without using the stack, then recast it from that mana") {
        val d = driver()
        val you = d.player1
        d.putLandOnBattlefield(you, "Mountain")
        val ignus = d.putCreatureOnBattlefield(you, "Grinning Ignus")

        d.submit(ActivateAbility(you, ignus, abilityId)).error shouldBe null
        d.stackSize shouldBe 0
        d.state.getZone(ZoneKey(you, Zone.HAND)).contains(ignus) shouldBe true
        val pool = d.state.getEntity(you)!!.get<ManaPoolComponent>()!!
        pool.colorless shouldBe 2
        pool.red shouldBe 1

        d.submit(CastSpell(you, ignus)).error shouldBe null
        d.bothPass()
        d.findPermanent(you, "Grinning Ignus").shouldNotBeNull()
    }

    test("can't be activated in combat") {
        val d = driver()
        val you = d.player1
        d.putLandOnBattlefield(you, "Mountain")
        val ignus = d.putCreatureOnBattlefield(you, "Grinning Ignus")

        d.passPriorityUntil(Step.BEGIN_COMBAT)
        d.submit(ActivateAbility(you, ignus, abilityId)).error.shouldNotBeNull()
        d.findPermanent(you, "Grinning Ignus").shouldNotBeNull()
    }
})
