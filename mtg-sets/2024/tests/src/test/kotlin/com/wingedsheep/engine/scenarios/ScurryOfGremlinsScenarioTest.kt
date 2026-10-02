package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.ScurryOfGremlins
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scurry of Gremlins (MH3) — two 1/1 red Gremlins on entry, then energy equal to the creatures
 * you control (counted after the tokens exist); {E}{E}{E}{E} pumps your team +1/+0 and haste.
 */
class ScurryOfGremlinsScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + ScurryOfGremlins)
        it.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.energy(): Int =
        state.getEntity(player1)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0
    fun GameTestDriver.giveEnergy(n: Int) = replaceState(state.updateEntity(player1) {
        it.with(CountersComponent(mapOf(CounterType.ENERGY to n)))
    })
    fun GameTestDriver.gremlins(): List<EntityId> = getPermanents(player1).filter {
        state.getEntity(it)?.get<CardComponent>()?.typeLine?.subtypes?.any { s -> s.value == "Gremlin" } == true
    }
    val ability = ScurryOfGremlins.activatedAbilities.single().id

    test("entering creates two red 1/1 Gremlins, then energy counts every creature you control") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val card = d.putCardInHand(d.player1, "Scurry of Gremlins")
        d.giveMana(d.player1, Color.RED, 2)
        d.giveMana(d.player1, Color.WHITE, 2)
        d.castSpell(d.player1, card).error shouldBe null
        d.bothPass() // resolve the enchantment
        d.bothPass() // resolve the ETB trigger

        val tokens = d.gremlins()
        tokens.size shouldBe 2
        tokens.forEach { t ->
            d.state.projectedState.getPower(t) shouldBe 1
            d.state.projectedState.getToughness(t) shouldBe 1
            d.state.projectedState.getColors(t) shouldBe setOf(Color.RED.name)
        }
        withClue("Bears + two Gremlins = 3; the opponent's creature doesn't count") { d.energy() shouldBe 3 }
    }

    test("paying four energy gives creatures you control +1/+0 and haste until end of turn") {
        val d = driver()
        val scurry = d.putPermanentOnBattlefield(d.player1, "Scurry of Gremlins")
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val theirs = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.giveEnergy(5)

        d.submit(ActivateAbility(d.player1, scurry, ability)).error shouldBe null
        d.energy() shouldBe 1
        d.bothPass()

        d.state.projectedState.getPower(bears) shouldBe 3
        d.state.projectedState.getToughness(bears) shouldBe 2
        d.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
        withClue("an opponent's creature is unaffected") {
            d.state.projectedState.getPower(theirs) shouldBe 2
            d.state.projectedState.hasKeyword(theirs, Keyword.HASTE) shouldBe false
        }

        d.passPriorityUntil(Step.END)
        d.bothPass()
        d.passPriorityUntil(Step.UPKEEP)
        d.state.projectedState.getPower(bears) shouldBe 2
        d.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe false
    }

    test("can't activate with fewer than four energy") {
        val d = driver()
        val scurry = d.putPermanentOnBattlefield(d.player1, "Scurry of Gremlins")
        d.giveEnergy(3)
        d.submit(ActivateAbility(d.player1, scurry, ability)).error shouldNotBe null
        d.energy() shouldBe 3
    }
})
