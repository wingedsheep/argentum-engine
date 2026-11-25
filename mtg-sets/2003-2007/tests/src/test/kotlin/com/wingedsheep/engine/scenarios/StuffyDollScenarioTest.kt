package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.chosenPlayer
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tsp.cards.StuffyDoll
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Stuffy Doll (TSP #264) — "Indestructible. As this creature enters, choose a player. Whenever
 * this creature is dealt damage, it deals that much damage to the chosen player. {T}: This
 * creature deals 1 damage to itself."
 *
 * The chosen player — any player, its controller included — takes exactly the damage the Doll
 * was dealt, even past its toughness (the 2021-03-19 ruling), and the Doll survives it all.
 */
class StuffyDollScenarioTest : FunSpec({

    val pinch = StuffyDoll.activatedAbilities.single().id

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + StuffyDoll)
        it.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Cast Stuffy Doll for player 1, naming [victim]; returns the Doll, ready to tap. */
    fun GameTestDriver.castDoll(victim: EntityId): EntityId {
        val doll = putCardInHand(player1, "Stuffy Doll")
        giveColorlessMana(player1, 5)
        castSpell(player1, doll).error shouldBe null
        bothPass()
        val decision = state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.prompt shouldBe "Choose a player"
        decision.options.size shouldBe 2
        submitDecision(player1, OptionChosenResponse(decision.id, state.turnOrder.indexOf(victim))).error shouldBe null
        state.getEntity(doll)!!.chosenPlayer() shouldBe victim
        removeSummoningSickness(doll)
        return doll
    }

    test("tapping it deals 1 to itself, and the chosen opponent takes 1") {
        val d = driver()
        val opponent = d.player2
        val doll = d.castDoll(opponent)
        val life = d.getLifeTotal(opponent)

        d.submit(ActivateAbility(d.player1, doll, pinch)).outcome shouldBe Outcome.Done
        d.bothPass() // the tap ability resolves
        d.bothPass() // the dealt-damage trigger resolves

        d.getLifeTotal(opponent) shouldBe life - 1
        d.getLifeTotal(d.player1) shouldBe 20
        d.state.getBattlefield().contains(doll) shouldBe true
    }

    test("lethal damage passes through in full, and indestructible keeps the Doll") {
        val d = driver()
        val opponent = d.player2
        val doll = d.castDoll(opponent)
        val life = d.getLifeTotal(opponent)

        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpellWithTargets(d.player1, bolt, listOf(ChosenTarget.Permanent(doll))).error shouldBe null
        d.bothPass() // Bolt resolves: 3 damage to a 0/1
        d.bothPass() // the trigger resolves

        d.getLifeTotal(opponent) shouldBe life - 3
        d.state.getBattlefield().contains(doll) shouldBe true
    }

    test("naming yourself turns the damage on its controller") {
        val d = driver()
        val doll = d.castDoll(d.player1)
        val life = d.getLifeTotal(d.player1)

        d.submit(ActivateAbility(d.player1, doll, pinch)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.bothPass()

        d.getLifeTotal(d.player1) shouldBe life - 1
        d.getLifeTotal(d.player2) shouldBe 20
    }
})
