package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.mtg.sets.definitions.pip.cards.MrHousePresidentAndCEO
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.mtg.sets.definitions.ltr.cards.PeregrinTook
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MrHousePresidentAndCEOScenarioTest : FunSpec({
    val name = "Mr. House, President and CEO"
    val ability = MrHousePresidentAndCEO.script.activatedAbilities.single().id
    fun setup() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(MrHousePresidentAndCEO, PredefinedTokens.Treasure))
        it.initMirrorMatch(Deck.of("Plains" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    for (treasures in listOf(0, 1, 4)) test("$treasures sacrificed Treasures add exactly $treasures dice") {
        val d = setup(); val player = d.activePlayer!!
        val house = d.putPermanentOnBattlefield(player, name)
        repeat(treasures) {
            val t = d.putPermanentOnBattlefield(player, "Treasure")
            d.submit(ActivateAbility(player, t, PredefinedTokens.Treasure.script.activatedAbilities.single().id,
                manaColorChoice = Color.RED)).outcome shouldBe Outcome.Done
        }
        d.giveMana(player, Color.WHITE, 4 - treasures)
        d.submit(ActivateAbility(player, house, ability)).outcome shouldBe Outcome.Done
        val result = d.bothPass()
        result.events.filterIsInstance<DieRolledEvent>().size shouldBe 1 + treasures
    }
    test("a sacrificed Treasure's paused color choice retains its source snapshot") {
        val d = setup(); val player = d.activePlayer!!
        val house = d.putPermanentOnBattlefield(player, name)
        val t = d.putPermanentOnBattlefield(player, "Treasure")
        d.submit(ActivateAbility(player, t, PredefinedTokens.Treasure.script.activatedAbilities.single().id)).outcome::class shouldBe Outcome.Paused::class
        d.submitDecision(player, ColorChosenResponse(d.pendingDecision!!.id, Color.RED)).outcome shouldBe Outcome.Done
        d.giveMana(player, Color.WHITE, 3)
        d.submit(ActivateAbility(player, house, ability)).outcome shouldBe Outcome.Done
        d.bothPass().events.filterIsInstance<DieRolledEvent>().size shouldBe 2
    }
    test("resolution uses the paid snapshot after the source leaves and new Treasure mana is floated") {
        val d = setup(); val player = d.activePlayer!!; val opponent = d.getOpponent(player)
        val house = d.putPermanentOnBattlefield(player, name)
        val t = d.putPermanentOnBattlefield(player, "Treasure")
        d.submit(ActivateAbility(player, t, PredefinedTokens.Treasure.script.activatedAbilities.single().id,
            manaColorChoice = Color.RED)).outcome shouldBe Outcome.Done
        d.giveMana(player, Color.WHITE, 3)
        d.submit(ActivateAbility(player, house, ability)).outcome shouldBe Outcome.Done
        val later = d.putPermanentOnBattlefield(player, "Treasure")
        d.submit(ActivateAbility(player, later, PredefinedTokens.Treasure.script.activatedAbilities.single().id,
            manaColorChoice = Color.RED)).outcome shouldBe Outcome.Done
        d.passPriority(player)
        d.giveMana(opponent, Color.WHITE, 2)
        d.castSpell(opponent, d.putCardInHand(opponent, "Disenchant"), listOf(house)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.bothPass().events.filterIsInstance<DieRolledEvent>().size shouldBe 2
    }
    test("each qualifying roll creates its own Robot, and only six or higher also creates Treasure") {
        for (face in listOf(3, 4, 5, 6, 19)) {
            val d = setup(); val player = d.activePlayer!!
            d.putPermanentOnBattlefield(player, name)
            val roller = card("Test d20") { typeLine = "Artifact"; activatedAbility { effect = Effects.RollDice(sides = 20) } }
            d.registerCards(listOf(roller))
            val source = d.putPermanentOnBattlefield(player, "Test d20")
            val seed = (0L..1000L).first { GameRng.seeded(it).nextInt(20).first + 1 == face }
            d.submit(ActivateAbility(player, source, roller.script.activatedAbilities.single().id)).outcome shouldBe Outcome.Done
            d.replaceState(d.state.copy(rng = GameRng.seeded(seed)))
            d.bothPass()
            while (d.state.stack.isNotEmpty()) d.bothPass()
            val permanents = d.getPermanents(player)
            permanents.count { d.getCardName(it) == "Robot Token" } shouldBe if (face >= 4) 1 else 0
            permanents.count { d.getCardName(it) == "Treasure" } shouldBe if (face >= 6) 1 else 0
        }
    }
    test("Peregrin Took adds one Food to the joint Robot and Treasure event") {
        for (face in listOf(4, 6)) {
            val d = setup(); val player = d.activePlayer!!
            d.registerCards(listOf(PeregrinTook, PredefinedTokens.Food))
            d.putPermanentOnBattlefield(player, name)
            d.putPermanentOnBattlefield(player, "Peregrin Took")
            val roller = card("Test d6") { typeLine = "Artifact"; activatedAbility { effect = Effects.RollDice() } }
            d.registerCards(listOf(roller))
            val source = d.putPermanentOnBattlefield(player, "Test d6")
            val seed = (0L..1000L).first { GameRng.seeded(it).nextInt(6).first + 1 == face }
            d.submit(ActivateAbility(player, source, roller.script.activatedAbilities.single().id)).outcome shouldBe Outcome.Done
            d.replaceState(d.state.copy(rng = GameRng.seeded(seed)))
            d.bothPass()
            while (d.state.stack.isNotEmpty()) d.bothPass()
            val permanents = d.getPermanents(player)
            permanents.count { d.getCardName(it) == "Robot Token" } shouldBe 1
            permanents.count { d.getCardName(it) == "Treasure" } shouldBe if (face == 6) 1 else 0
            permanents.count { d.getCardName(it) == "Food" } shouldBe 1
        }
    }
    test("auto and explicit source payment keep the same count and do not consume excess") {
        for (explicit in listOf(false, true)) {
            val d = setup(); val player = d.activePlayer!!
            val producer = card("Reusable Treasure") {
                typeLine = "Artifact — Treasure"
                activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.WHITE, 3); manaAbility = true }
            }
            d.registerCards(listOf(producer))
            val source = d.putPermanentOnBattlefield(player, "Reusable Treasure")
            val house = d.putPermanentOnBattlefield(player, name)
            d.giveColorlessMana(player, 2)
            val strategy = if (explicit) PaymentStrategy.Explicit(listOf(source)) else PaymentStrategy.AutoPay
            d.submit(ActivateAbility(player, house, ability, paymentStrategy = strategy)).outcome shouldBe Outcome.Done
            d.bothPass().events.filterIsInstance<DieRolledEvent>().size shouldBe 3
            d.state.getEntity(player)!!.get<ManaPoolComponent>()!!.manaUnits.size shouldBe 1
        }
    }
})
