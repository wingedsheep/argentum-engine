package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.LuckBobblehead
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.GameRng
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LuckBobbleheadScenarioTest : FunSpec({
    fun setup()=GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(LuckBobblehead, PredefinedTokens.Treasure))
        it.initMirrorMatch(Deck.of("Plains" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun seedForSixes(count:Int,sixes:Int):Long = (0L..10000L).first { seed ->
        var rng=GameRng.seeded(seed);var hits=0
        repeat(count) { val (face,next)=rng.nextInt(6);rng=next;if(face==5)hits++ }
        hits==sixes
    }
    test("counts only controlled Bobbleheads at resolution, including arrivals after activation") {
        val d=setup();val player=d.activePlayer!!;val opponent=d.getOpponent(player)
        val source=d.putPermanentOnBattlefield(player,"Luck Bobblehead")
        d.putPermanentOnBattlefield(opponent,"Luck Bobblehead")
        d.giveMana(player,Color.WHITE)
        d.submit(ActivateAbility(player,source,LuckBobblehead.script.activatedAbilities[1].id)).outcome shouldBe Outcome.Done
        d.putPermanentOnBattlefield(player,"Luck Bobblehead")
        d.replaceState(d.state.copy(rng=GameRng.seeded(seedForSixes(2, 1))))
        val result=d.bothPass()
        val rolls=result.events.filterIsInstance<DieRolledEvent>()
        rolls.size shouldBe 2
        val treasures=d.getPermanents(player).filter { d.getCardName(it)=="Treasure" }
        treasures.size shouldBe rolls.count { it.result % 2==0 }
        treasures.all { d.isTapped(it) } shouldBe true
        d.state.gameOver shouldBe false
    }
    test("removing the only Bobblehead before resolution rolls zero dice and creates no tokens") {
        val d=setup();val player=d.activePlayer!!;val opponent=d.getOpponent(player)
        val source=d.putPermanentOnBattlefield(player,"Luck Bobblehead")
        d.giveMana(player,Color.WHITE)
        d.submit(ActivateAbility(player,source,LuckBobblehead.script.activatedAbilities[1].id)).outcome shouldBe Outcome.Done
        d.passPriority(player)
        d.giveMana(opponent,Color.WHITE,2)
        val removal=d.putCardInHand(opponent,"Disenchant")
        d.castSpell(opponent,removal,listOf(source)).outcome shouldBe Outcome.Done
        d.bothPass()
        val before=d.state.rng
        val result=d.bothPass()
        result.events.filterIsInstance<DieRolledEvent>() shouldBe emptyList()
        d.state.rng shouldBe before
        d.getPermanents(player).count { d.getCardName(it)=="Treasure" } shouldBe 0
        d.state.gameOver shouldBe false
    }
    for (sixes in listOf(6,7,8)) test("$sixes sixes in a single batch ${if(sixes==7) "wins" else "does not win"}") {
        val d=setup();val player=d.activePlayer!!
        val bobbles=List(42) { d.putPermanentOnBattlefield(player,"Luck Bobblehead") }
        d.giveMana(player,Color.WHITE)
        d.submit(ActivateAbility(player,bobbles.first(),LuckBobblehead.script.activatedAbilities[1].id)).outcome shouldBe Outcome.Done
        d.replaceState(d.state.copy(rng=GameRng.seeded(seedForSixes(42,sixes))))
        val result=d.bothPass()
        val rolls=result.events.filterIsInstance<DieRolledEvent>()
        rolls.size shouldBe 42
        rolls.count { it.result==6 } shouldBe sixes
        d.getPermanents(player).count { d.getCardName(it)=="Treasure" } shouldBe rolls.count { it.result % 2==0 }
        d.state.gameOver shouldBe (sixes==7)
        if(sixes==7)d.state.winnerId shouldBe player
    }
    test("prior resolutions never contribute to the exact-seven condition") {
        val d=setup();val player=d.activePlayer!!
        val bobbles=List(42) { d.putPermanentOnBattlefield(player,"Luck Bobblehead") }
        repeat(2) { index ->
            d.giveMana(player,Color.WHITE)
            d.submit(ActivateAbility(player,bobbles[index],LuckBobblehead.script.activatedAbilities[1].id)).outcome shouldBe Outcome.Done
            d.replaceState(d.state.copy(rng=GameRng.seeded(seedForSixes(42,if(index==0)3 else 4))))
            d.bothPass();d.state.gameOver shouldBe false
        }
    }
    test("mana ability adds the selected color immediately without rolling dice") {
        val d=setup();val player=d.activePlayer!!
        val source=d.putPermanentOnBattlefield(player,"Luck Bobblehead")
        val result=d.submit(ActivateAbility(player,source,LuckBobblehead.script.activatedAbilities[0].id,manaColorChoice=Color.RED))
        result.outcome shouldBe Outcome.Done
        result.events.filterIsInstance<DieRolledEvent>() shouldBe emptyList()
        d.state.stack.size shouldBe 0
        d.isTapped(source) shouldBe true
    }
})
