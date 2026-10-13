package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.dmu.cards.CitizensArrest
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DominariaUnitedForest274
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class CitizensArrestScenarioTest : FunSpec({
    val bear = card("Arrest Suspect") { typeLine = "Creature — Bear"; power = 2; toughness = 2 }
    val own = card("Arrest Deputy") { typeLine = "Creature — Human"; power = 1; toughness = 1 }
    val doom = card("Arrest Doom") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.Destroy(target(TargetFilter.Permanent)) }
    }
    fun setup() = GameTestDriver().apply {
        registerCards(listOf(CitizensArrest, DominariaUnitedForest274, bear, own, doom))
        initMirrorMatch(deck = Deck.of("Forest" to 30), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.arrest(): com.wingedsheep.sdk.model.EntityId {
        val p = activePlayer!!
        val id = putCardInHand(p, CitizensArrest.name)
        giveMana(p, Color.WHITE, 2); giveColorlessMana(p, 1)
        castSpell(p, id).error shouldBe null
        bothPass().error shouldBe null
        return id
    }
    fun GameTestDriver.destroy(victim: com.wingedsheep.sdk.model.EntityId) {
        val p = state.priorityPlayerId!!
        castSpell(p, putCardInHand(p, doom.name), listOf(victim)).error shouldBe null
        bothPass().error shouldBe null
    }
    test("exiles an opponent's creature until Citizen's Arrest leaves") {
        val d = setup(); val p = d.activePlayer!!; val opp = d.getOpponent(p)
        val suspect = d.putCreatureOnBattlefield(opp, bear.name)
        d.putCreatureOnBattlefield(p, own.name)
        val arrest = d.arrest()
        val q = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        q.legalTargets.values.flatten() shouldBe listOf(suspect)
        d.submitTargetSelection(p, listOf(suspect)).error shouldBe null
        d.bothPass().error shouldBe null
        d.getExile(opp) shouldContain suspect
        d.destroy(arrest)
        d.state.stack.size shouldBe 0
        d.getExile(opp) shouldNotContain suspect
        d.findPermanent(opp, bear.name) shouldBe d.getCreatures(opp).single()
    }
    test("if it leaves before its trigger resolves, nothing is exiled") {
        val d = setup(); val p = d.activePlayer!!; val opp = d.getOpponent(p)
        val suspect = d.putCreatureOnBattlefield(opp, bear.name)
        val arrest = d.arrest()
        d.submitTargetSelection(p, listOf(suspect)).error shouldBe null
        d.destroy(arrest)
        d.bothPass().error shouldBe null
        d.state.getBattlefield() shouldContain suspect
        d.getExile(opp) shouldNotContain suspect
    }
})
