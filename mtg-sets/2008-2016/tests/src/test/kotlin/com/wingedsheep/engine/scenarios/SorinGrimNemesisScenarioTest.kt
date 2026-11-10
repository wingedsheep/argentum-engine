package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.soi.cards.SorinGrimNemesis
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Sorin, Grim Nemesis: the +1 drains each opponent by the revealed card's mana value, the −X
 * spends one chosen X on both the damage and the life gain, and the −9 counts tokens off the
 * highest life total among all players — not just its controller's.
 */
class SorinGrimNemesisScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(SorinGrimNemesis))
        it.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun putSorin(d: GameTestDriver, player: EntityId, loyalty: Int = 6): EntityId =
        d.putPermanentOnBattlefield(player, "Sorin, Grim Nemesis").also {
            d.addComponent(it, CountersComponent(mapOf(CounterType.LOYALTY to loyalty)))
        }
    fun loyalty(d: GameTestDriver, id: EntityId) =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0
    val abilities = SorinGrimNemesis.script.activatedAbilities

    test("+1 puts the top card into hand and each opponent loses life equal to its mana value") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val sorin = putSorin(d, me)
        val bears = d.putCardOnTopOfLibrary(me, "Grizzly Bears")

        d.submitSuccess(ActivateAbility(me, sorin, abilities[0].id))
        loyalty(d, sorin) shouldBe 7
        d.bothPass()

        d.getHand(me) shouldContain bears
        d.getLifeTotal(opp) shouldBe 18
        d.getLifeTotal(me) shouldBe 20
    }

    test("−X deals X damage to the target creature and gains X life") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val sorin = putSorin(d, me)
        d.setLifeTotal(me, 10)
        val bears = d.putPermanentOnBattlefield(opp, "Grizzly Bears")

        d.submitSuccess(ActivateAbility(me, sorin, abilities[1].id,
            targets = listOf(ChosenTarget.Permanent(bears)), xValue = 3))
        loyalty(d, sorin) shouldBe 3
        d.bothPass()

        d.getGraveyard(opp) shouldContain bears
        d.getLifeTotal(me) shouldBe 13
    }

    test("−X gains no life when its target is gone on resolution") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val sorin = putSorin(d, me)
        val bears = d.putPermanentOnBattlefield(opp, "Grizzly Bears")

        d.submitSuccess(ActivateAbility(me, sorin, abilities[1].id,
            targets = listOf(ChosenTarget.Permanent(bears)), xValue = 2))
        d.moveToGraveyard(bears)
        d.bothPass()

        loyalty(d, sorin) shouldBe 4
        d.getLifeTotal(me) shouldBe 20
    }

    test("−9 creates lifelink Vampire Knights equal to the highest life total among all players") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val sorin = putSorin(d, me, loyalty = 9)
        d.setLifeTotal(me, 3)
        d.setLifeTotal(opp, 5)

        d.submitSuccess(ActivateAbility(me, sorin, abilities[2].id))
        d.bothPass()

        val knights = d.getCreatures(me)
        knights.size shouldBe 5
        knights.forEach { d.state.projectedState.hasKeyword(it, Keyword.LIFELINK) shouldBe true }
        d.getCreatures(opp).size shouldBe 0
    }
})
