package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Sacrifice
import com.wingedsheep.mtg.sets.definitions.lea.cards.Clone
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class SacrificeScenarioTest : FunSpec({
    fun setup() = GameTestDriver().also {
        it.registerCards(TestCards.all + Sacrifice + Clone)
        it.initMirrorMatch(Deck.of("Forest" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
        it.giveMana(it.activePlayer!!, Color.BLACK, 1)
    }
    fun cast(d: GameTestDriver, ids: List<EntityId>) = d.submit(CastSpell(
        d.activePlayer!!, d.putCardInHand(d.activePlayer!!, "Sacrifice"),
        paymentStrategy = PaymentStrategy.FromPool,
        additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = ids)))
    fun black(d: GameTestDriver) = d.state.getEntity(d.activePlayer!!)!!.get<ManaPoolComponent>()!!.black

    test("sacrifice is paid before responses and mana is added only on resolution") {
        val d = setup(); val p = d.activePlayer!!
        val id = d.putCreatureOnBattlefield(p, "Hill Giant")
        cast(d, listOf(id)).error shouldBe null
        d.state.getGraveyard(p).contains(id) shouldBe true
        d.state.stack.size shouldBe 1
        black(d) shouldBe 0
        d.bothPass()
        black(d) shouldBe 4
    }
    for (invalid in listOf("none", "two", "opponent", "noncreature")) {
        test("rejects $invalid sacrifice without consuming mana or permanents") {
            val d = setup(); val p = d.activePlayer!!
            val ids = when (invalid) {
                "none" -> emptyList()
                "two" -> listOf(d.putCreatureOnBattlefield(p, "Grizzly Bears"), d.putCreatureOnBattlefield(p, "Hill Giant"))
                "opponent" -> listOf(d.putCreatureOnBattlefield(d.getOpponent(p), "Grizzly Bears"))
                else -> listOf(d.putLandOnBattlefield(p, "Forest"))
            }
            cast(d, ids).error shouldNotBe null
            black(d) shouldBe 1
            ids.all { it in d.state.getBattlefield() } shouldBe true
        }
    }
    test("face-down sacrifice adds zero despite the revealed card's cost") {
        val d = setup(); val p = d.activePlayer!!
        val id = d.putCreatureOnBattlefield(p, "Hill Giant")
        d.addComponent(id, FaceDownComponent)
        cast(d, listOf(id)).error shouldBe null
        d.bothPass()
        black(d) shouldBe 0
    }
    test("a token with a copied mana cost supplies mana after ceasing to exist") {
        val d = setup(); val p = d.activePlayer!!
        val id = d.putCreatureOnBattlefield(p, "Hill Giant")
        d.addComponent(id, TokenComponent)
        cast(d, listOf(id)).error shouldBe null
        d.state.getEntity(id) shouldBe null
        d.bothPass()
        black(d) shouldBe 4
    }

    test("sacrificing Clone uses its copied cost before the graveyard resets it") {
        val d = setup(); val p = d.activePlayer!!
        val bear = d.putCreatureOnBattlefield(p, "Grizzly Bears")
        val clone = d.putCardInHand(p, "Clone")
        d.giveMana(p, Color.BLUE, 4)
        d.castSpell(p, clone).error shouldBe null
        d.bothPass()
        d.submitCardSelection(p, listOf(bear)).error shouldBe null
        d.state.getEntity(clone)!!.get<CardComponent>()!!.manaValue shouldBe 2
        cast(d, listOf(clone)).error shouldBe null
        d.state.getEntity(clone)!!.get<CardComponent>()!!.manaValue shouldBe 4
        d.bothPass()
        black(d) shouldBe 2
    }
    test("countering the spell adds no mana and does not refund the sacrificed creature") {
        val d = setup(); val p = d.activePlayer!!; val opponent = d.getOpponent(p)
        val bear = d.putCreatureOnBattlefield(p, "Grizzly Bears")
        cast(d, listOf(bear)).error shouldBe null
        val spell = d.state.stack.single()
        d.passPriority(p)
        d.giveMana(opponent, Color.BLUE, 2)
        val counter = d.putCardInHand(opponent, "Counterspell")
        d.castSpellWithTargets(opponent, counter, listOf(ChosenTarget.Spell(spell))).error shouldBe null
        d.bothPass()
        black(d) shouldBe 0
        d.state.getGraveyard(p).contains(bear) shouldBe true
        d.state.stack.size shouldBe 0
    }
    test("a costless creature such as an animated land adds no mana") {
        val d = setup(); val p = d.activePlayer!!
        val id = d.putCreatureOnBattlefield(p, "Grizzly Bears")
        d.replaceState(d.state.updateEntity(id) { c -> c.with(c.get<CardComponent>()!!.copy(manaCost = ManaCost.ZERO)) })
        cast(d, listOf(id)).error shouldBe null
        d.bothPass()
        black(d) shouldBe 0
    }
})
