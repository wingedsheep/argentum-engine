package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.InitiateOfBlood
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Initiate of Blood // Goka the Unjust (CHK) — a flip card.
 *
 * "{T}: This creature deals 1 damage to target creature that was dealt damage this turn. When that
 * creature dies this turn, flip this creature."
 * Goka: "{T}: Goka deals 4 damage to target creature that was dealt damage this turn."
 */
class InitiateofBloodScenarioTest : FunSpec({

    val pingAbility = InitiateOfBlood.activatedAbilities.single().id
    val gokaAbility = InitiateOfBlood.flipSide!!.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + InitiateOfBlood)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.name(id: EntityId) = state.getEntity(id)!!.get<CardComponent>()!!.name
    fun GameTestDriver.damage(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    fun GameTestDriver.bolt(target: EntityId) {
        val bolt = putCardInHand(player1, "Lightning Bolt")
        giveMana(player1, Color.RED, 1)
        castSpell(player1, bolt, listOf(target)).error shouldBe null
        bothPass()
    }

    fun GameTestDriver.activate(source: EntityId, ability: com.wingedsheep.sdk.scripting.AbilityId, target: EntityId) =
        submit(
            ActivateAbility(
                player1, source, ability,
                targets = listOf(ChosenTarget.Permanent(target)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )

    test("cannot target a creature that wasn't dealt damage this turn") {
        val d = driver()
        val initiate = d.putCreatureOnBattlefield(d.player1, "Initiate of Blood")
        d.removeSummoningSickness(initiate)
        val force = d.putCreatureOnBattlefield(d.player2, "Force of Nature")

        (d.activate(initiate, pingAbility, force).error != null) shouldBe true
        d.isTapped(initiate) shouldBe false
    }

    test("pings a damaged creature; stays unflipped while it survives, flips when it later dies this turn") {
        val d = driver()
        val initiate = d.putCreatureOnBattlefield(d.player1, "Initiate of Blood")
        d.removeSummoningSickness(initiate)
        val force = d.putCreatureOnBattlefield(d.player2, "Force of Nature")

        d.bolt(force)
        d.damage(force) shouldBe 3

        d.activate(initiate, pingAbility, force).error shouldBe null
        d.bothPass()
        d.damage(force) shouldBe 4
        d.name(initiate) shouldBe "Initiate of Blood"

        // Dies to another source this turn — the watched creature dying still flips the Initiate.
        d.bolt(force)
        d.getGraveyard(d.player2) shouldContain force
        if (d.state.stack.isNotEmpty()) d.bothPass()

        d.name(initiate) shouldBe "Goka the Unjust"
        d.state.projectedState.getPower(initiate) shouldBe 4
        d.state.projectedState.getToughness(initiate) shouldBe 4
        d.state.projectedState.isLegendary(initiate) shouldBe true
    }

    test("the Initiate's own ping killing the creature flips it, and Goka deals 4") {
        val d = driver()
        val initiate = d.putCreatureOnBattlefield(d.player1, "Initiate of Blood")
        d.removeSummoningSickness(initiate)
        val first = d.putCreatureOnBattlefield(d.player2, "Force of Nature")
        val victim = d.putCreatureOnBattlefield(d.player2, "Force of Nature")

        // Bolt (3) + ping (1) + ping (1) = 5: the second ping is the lethal one.
        d.bolt(first)
        d.activate(initiate, pingAbility, first).error shouldBe null
        d.bothPass()
        d.damage(first) shouldBe 4
        d.untapPermanent(initiate)
        d.activate(initiate, pingAbility, first).error shouldBe null
        d.bothPass()
        if (d.state.stack.isNotEmpty()) d.bothPass()

        d.getGraveyard(d.player2) shouldContain first
        d.name(initiate) shouldBe "Goka the Unjust"

        d.bolt(victim)
        d.untapPermanent(initiate)
        d.activate(initiate, gokaAbility, victim).error shouldBe null
        d.bothPass()
        d.getGraveyard(d.player2) shouldContain victim
    }
})
