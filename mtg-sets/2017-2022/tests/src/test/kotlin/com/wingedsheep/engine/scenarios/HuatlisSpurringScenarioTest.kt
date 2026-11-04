package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DominariaUnitedForest274
import com.wingedsheep.mtg.sets.definitions.war.cards.HuatliTheSunsHeart
import com.wingedsheep.mtg.sets.definitions.xln.cards.HuatlisSpurring
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class HuatlisSpurringScenarioTest : FunSpec({
    val creature = card("Spurring Test Creature") {
        manaCost = "{2}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    fun setup() = GameTestDriver().apply {
        registerCards(listOf(HuatlisSpurring, HuatliTheSunsHeart, DominariaUnitedForest274, creature))
        initMirrorMatch(deck = Deck.of("Forest" to 30), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.putHuatli(controller: EntityId): EntityId =
        putPermanentOnBattlefield(controller, HuatliTheSunsHeart.name).also {
            addComponent(it, CountersComponent(mapOf(CounterType.LOYALTY to 7)))
        }

    for (huatliController in listOf("none", "caster", "opponent")) {
        test("boost with Huatli controlled by $huatliController lasts only until end of turn") {
            val d = setup()
            val p = d.activePlayer!!
            val opponent = if (p == d.player1) d.player2 else d.player1
            if (huatliController != "none") {
                d.putHuatli(if (huatliController == "caster") p else opponent)
            }
            // Any creature is legal, including an opponent's.
            val target = d.putCreatureOnBattlefield(opponent, creature.name)
            val spell = d.putCardInHand(p, HuatlisSpurring.name)
            d.giveMana(p, Color.RED)
            d.castSpell(p, spell, listOf(target)).error shouldBe null
            d.bothPass().error shouldBe null
            d.state.projectedState.getPower(target) shouldBe if (huatliController == "caster") 6 else 4
            d.state.projectedState.getToughness(target) shouldBe 2
            d.passPriorityUntil(Step.UPKEEP)
            d.state.projectedState.getPower(target) shouldBe 2
        }
    }

    test("Huatli arriving after casting counts but leaving after resolution does not remove the boost") {
        val d = setup()
        val p = d.activePlayer!!
        val target = d.putCreatureOnBattlefield(p, creature.name)
        val spell = d.putCardInHand(p, HuatlisSpurring.name)
        d.giveMana(p, Color.RED)
        d.castSpell(p, spell, listOf(target)).error shouldBe null
        val huatli = d.putHuatli(p)
        d.bothPass().error shouldBe null
        d.state.projectedState.getPower(target) shouldBe 6
        d.moveToGraveyard(huatli)
        d.state.projectedState.getPower(target) shouldBe 6
    }

    test("Huatli leaving before resolution gives the smaller boost and arriving later does not increase it") {
        val d = setup()
        val p = d.activePlayer!!
        val target = d.putCreatureOnBattlefield(p, creature.name)
        val huatli = d.putHuatli(p)
        val spell = d.putCardInHand(p, HuatlisSpurring.name)
        d.giveMana(p, Color.RED)
        d.castSpell(p, spell, listOf(target)).error shouldBe null
        d.moveToGraveyard(huatli)
        d.bothPass().error shouldBe null
        d.state.projectedState.getPower(target) shouldBe 4
        d.putHuatli(p)
        d.state.projectedState.getPower(target) shouldBe 4
    }
})
