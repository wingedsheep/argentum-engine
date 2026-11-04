package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.rix.cards.AngrathMinotaurPirate
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AngrathMinotaurPirateScenarioTest : FunSpec({
    val indestructibleBear = card("Angrath Test Indestructible Bear") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.INDESTRUCTIBLE)
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(AngrathMinotaurPirate, indestructibleBear))
        it.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun putAngrath(d: GameTestDriver, player: EntityId, loyalty: Int): EntityId =
        d.putPermanentOnBattlefield(player, "Angrath, Minotaur Pirate").also {
            d.addComponent(it, CountersComponent(mapOf(CounterType.LOYALTY to loyalty)))
        }

    val abilities = AngrathMinotaurPirate.script.activatedAbilities

    test("plus two deals 1 damage to the target opponent and each creature they control, not yours") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val angrath = putAngrath(d, me, 5)
        val mine = d.putPermanentOnBattlefield(me, "Grizzly Bears")
        val theirs = d.putPermanentOnBattlefield(opp, "Grizzly Bears")

        d.submitSuccess(ActivateAbility(me, angrath, abilities[0].id, targets = listOf(ChosenTarget.Player(opp))))
        d.bothPass()

        d.getLifeTotal(opp) shouldBe 19
        d.getLifeTotal(me) shouldBe 20
        d.state.getEntity(theirs)?.get<DamageComponent>()
            ?.amount shouldBe 1
        d.state.getEntity(mine)?.get<DamageComponent>()
            ?.amount shouldBe null
        d.state.getEntity(angrath)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 7
    }

    test("minus eleven counts indestructible survivors and pumped power before destroying") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val angrath = putAngrath(d, me, 11)
        val survivor = d.putPermanentOnBattlefield(opp, indestructibleBear.name)
        val pumped = d.putPermanentOnBattlefield(opp, "Grizzly Bears").also {
            d.addComponent(it, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
        }
        val mine = d.putPermanentOnBattlefield(me, "Grizzly Bears")

        d.submitSuccess(ActivateAbility(me, angrath, abilities[2].id, targets = listOf(ChosenTarget.Player(opp))))
        d.bothPass()

        // 2 (indestructible) + 4 (2/2 with two +1/+1 counters) = 6
        d.getLifeTotal(opp) shouldBe 14
        d.getLifeTotal(me) shouldBe 20
        d.getGraveyard(opp).contains(pumped) shouldBe true
        d.state.getBattlefield().contains(survivor) shouldBe true
        d.state.getBattlefield().contains(mine) shouldBe true
    }
})
