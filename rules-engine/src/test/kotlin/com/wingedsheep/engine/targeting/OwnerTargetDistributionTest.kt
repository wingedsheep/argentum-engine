package com.wingedsheep.engine.targeting

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.utils.TargetEnumerationUtils
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.*
import io.kotest.matchers.shouldBe

class OwnerTargetDistributionTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMultiplayer(List(4) { Deck.of("Mountain" to 40) })
    }
    val req = TargetObject(filter = TargetFilter(GameObjectFilter.InstantOrSorcery, Zone.GRAVEYARD),
        ownerDistribution = OwnerDistribution.EXACTLY_ONE)
    test("enumeration counts owners with matching cards and exposes disjoint pools") {
        val d = setup()
        val a = d.putCardInGraveyard(d.player1, "Lightning Bolt")
        val b = d.putCardInGraveyard(d.player1, "Lightning Bolt")
        val c = d.putCardInGraveyard(d.player2, "Lightning Bolt")
        d.putCardInGraveyard(d.state.turnOrder[2], "Grizzly Bears")
        val info = TargetEnumerationUtils(PredicateEvaluator(null)).buildTargetInfos(d.state, d.player1, listOf(req)).single()
        info.minTargets shouldBe 2
        info.maxTargets shouldBe 2
        info.exclusiveGroups shouldBe listOf(listOf(a, b), listOf(c))
        val v = TargetValidator(PredicateEvaluator(null))
        fun check(ids: List<com.wingedsheep.sdk.model.EntityId>) = v.validateTargets(d.state,
            ids.map { ChosenTarget.Card(it, d.state.getEntity(it)!!.get<com.wingedsheep.engine.state.components.identity.OwnerComponent>()!!.playerId, Zone.GRAVEYARD) }, listOf(req), d.player1, targetingSourceType = com.wingedsheep.engine.handlers.TargetingSourceType.SPELL)
        check(listOf(a, b)).shouldNotBeNull()
        check(listOf(a)).shouldNotBeNull()
        check(listOf(a, c)).shouldBeNull()
        check(emptyList()).shouldNotBeNull()
        val optional = req.copy(ownerDistribution = OwnerDistribution.UP_TO_ONE)
        v.validateTargets(d.state, emptyList(), listOf(optional), d.player1, targetingSourceType = com.wingedsheep.engine.handlers.TargetingSourceType.SPELL).shouldBeNull()
    }
    test("empty eligible scope has zero required targets") {
        val d = setup()
        val info = TargetEnumerationUtils(PredicateEvaluator(null)).buildTargetInfos(d.state, d.player1, listOf(req)).single()
        info.minTargets shouldBe 0
        info.maxTargets shouldBe 0
        TargetValidator(PredicateEvaluator(null)).validateTargets(d.state, emptyList(), listOf(req), d.player1, targetingSourceType = com.wingedsheep.engine.handlers.TargetingSourceType.SPELL).shouldBeNull()
    }
})
