package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DominariaUnitedForest274
import com.wingedsheep.mtg.sets.definitions.dmu.cards.ElasIlKorSadisticPilgrim
import com.wingedsheep.mtg.sets.definitions.ice.cards.Pyroclasm
import com.wingedsheep.mtg.sets.definitions.m14.cards.RiseOfTheDarkRealms
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ElasIlKorSadisticPilgrimScenarioTest : FunSpec({
    val creature = card("Elas Test Creature") {
        manaCost = "{1}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
    }
    fun driver() = GameTestDriver().apply {
        registerCards(listOf(ElasIlKorSadisticPilgrim, DominariaUnitedForest274, creature, Pyroclasm, RiseOfTheDarkRealms))
        initMirrorMatch(deck = Deck.of("Forest" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("own entry does not trigger but another controlled creature does, even if Elas leaves") {
        val d = driver()
        d.giveMana(d.player1, Color.WHITE, 1)
        d.giveMana(d.player1, Color.BLACK, 1)
        val elas = d.putCardInHand(d.player1, ElasIlKorSadisticPilgrim.name)
        d.castSpell(d.player1, elas).outcome shouldBe Outcome.Done
        d.bothPass()
        d.stackSize shouldBe 0
        d.getLifeTotal(d.player1) shouldBe 20
        d.giveMana(d.player1, Color.GREEN, 1)
        d.castSpell(d.player1, d.putCardInHand(d.player1, creature.name)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.stackSize shouldBe 1
        d.moveToGraveyard(elas)
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 21
        d.getLifeTotal(d.player2) shouldBe 20
    }

    test("simultaneous deaths see each other controlled creature but not self or opponent") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, ElasIlKorSadisticPilgrim.name)
        repeat(2) { d.putCreatureOnBattlefield(d.player1, creature.name) }
        d.putCreatureOnBattlefield(d.player2, creature.name)
        d.giveMana(d.player1, Color.RED, 2)
        d.castSpell(d.player1, d.putCardInHand(d.player1, "Pyroclasm")).outcome shouldBe Outcome.Done
        d.bothPass()
        d.stackSize shouldBe 2
        repeat(2) { d.bothPass() }
        d.stackSize shouldBe 0
        d.getLifeTotal(d.player1) shouldBe 20
        d.getLifeTotal(d.player2) shouldBe 18
    }

    test("simultaneous entry with Elas triggers for each other creature including an opponent-owned one") {
        val d = driver()
        d.putCardInGraveyard(d.player1, ElasIlKorSadisticPilgrim.name)
        d.putCardInGraveyard(d.player1, creature.name)
        d.putCardInGraveyard(d.player2, creature.name)
        d.giveMana(d.player1, Color.BLACK, 9)
        d.castSpell(d.player1, d.putCardInHand(d.player1, "Rise of the Dark Realms")).outcome shouldBe Outcome.Done
        d.bothPass()
        d.stackSize shouldBe 2
        repeat(2) { d.bothPass() }
        d.stackSize shouldBe 0
        d.getLifeTotal(d.player1) shouldBe 22
        d.getLifeTotal(d.player2) shouldBe 20
    }
})
