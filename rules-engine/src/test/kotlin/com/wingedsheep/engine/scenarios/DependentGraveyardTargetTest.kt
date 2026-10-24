package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.legalactions.utils.TargetEnumerationUtils
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "Target player shuffles up to three target cards from their graveyard" (Gaea's Blessing, Quandrix
 * Command): a graveyard-card slot whose owner is an earlier player target is offered per chosen
 * player, through the same prefix tables as "target land that player controls".
 */
class DependentGraveyardTargetTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
    }
    val predicates = PredicateEvaluator(cardRegistry = null)
    val utils = TargetEnumerationUtils(predicates)

    fun cardsOf(filter: GameObjectFilter) = listOf(
        TargetPlayer(id = "player"),
        TargetObject(count = 3, optional = true, filter = TargetFilter(filter, zone = Zone.GRAVEYARD)),
    )

    test("cards from the targeted player's graveyard are offered per chosen player") {
        val d = setup()
        val mine = d.putCardInGraveyard(d.player1, "Forest")
        val theirs = listOf(d.putCardInGraveyard(d.player2, "Forest"), d.putCardInGraveyard(d.player2, "Mountain"))
        val infos = utils.buildTargetInfos(d.state, d.player1, cardsOf(GameObjectFilter.Any.ownedByTargetPlayer()),
            targetingSourceType = TargetingSourceType.SPELL)
        infos[0].validTargets shouldContainExactlyInAnyOrder listOf(d.player1, d.player2)
        infos[1].validTargetsByPrefix!![d.player1.toString()] shouldBe listOf(mine)
        infos[1].validTargetsByPrefix!![d.player2.toString()]!! shouldContainExactlyInAnyOrder theirs
        infos[1].minTargets shouldBe 0
        infos[1].maxTargets shouldBe 3
    }

    test("a named player reference scopes the graveyard the same way") {
        val d = setup()
        val mine = d.putCardInGraveyard(d.player1, "Forest")
        val theirs = d.putCardInGraveyard(d.player2, "Mountain")
        val filter = GameObjectFilter.Any.ownedBy(EffectTarget.BoundVariable("player"))
        val table = utils.buildTargetInfos(d.state, d.player1, cardsOf(filter),
            targetingSourceType = TargetingSourceType.SPELL)[1].validTargetsByPrefix!!
        table shouldBe mapOf(d.player1.toString() to listOf(mine), d.player2.toString() to listOf(theirs))
    }

    test("an empty graveyard keeps its player for an up-to slot but not for a required one") {
        val d = setup()
        d.putCardInGraveyard(d.player2, "Forest")
        val filter = GameObjectFilter.Any.ownedByTargetPlayer()
        utils.buildTargetInfos(d.state, d.player1, cardsOf(filter),
            targetingSourceType = TargetingSourceType.SPELL)[0].validTargets shouldContainExactlyInAnyOrder
            listOf(d.player1, d.player2)
        val required = listOf(
            TargetPlayer(id = "player"),
            TargetObject(filter = TargetFilter(filter, zone = Zone.GRAVEYARD)),
        )
        utils.buildTargetInfos(d.state, d.player1, required,
            targetingSourceType = TargetingSourceType.SPELL)[0].validTargets shouldBe listOf(d.player2)
    }

    test("the cast rejects a card from a graveyard other than the targeted player's") {
        val d = setup()
        val mine = d.putCardInGraveyard(d.player1, "Forest")
        val theirs = d.putCardInGraveyard(d.player2, "Mountain")
        val requirements = cardsOf(GameObjectFilter.Any.ownedByTargetPlayer())
        val validator = TargetValidator(predicates)
        validator.validateTargets(d.state, listOf(ChosenTarget.Player(d.player2),
            ChosenTarget.Card(theirs, d.player2, Zone.GRAVEYARD)), requirements, d.player1,
            targetingSourceType = TargetingSourceType.SPELL) shouldBe null
        validator.validateTargets(d.state, listOf(ChosenTarget.Player(d.player2),
            ChosenTarget.Card(mine, d.player1, Zone.GRAVEYARD)), requirements, d.player1,
            targetingSourceType = TargetingSourceType.SPELL) shouldNotBe null
    }

    test("the cast accepts a named-player-owned card it offered and rejects another graveyard's") {
        val d = setup()
        val mine = d.putCardInGraveyard(d.player1, "Forest")
        val theirs = d.putCardInGraveyard(d.player2, "Mountain")
        val requirements = cardsOf(GameObjectFilter.Any.ownedBy(EffectTarget.BoundVariable("player")))
        val validator = TargetValidator(predicates)
        validator.validateTargets(d.state, listOf(ChosenTarget.Player(d.player2),
            ChosenTarget.Card(theirs, d.player2, Zone.GRAVEYARD)), requirements, d.player1,
            targetingSourceType = TargetingSourceType.SPELL) shouldBe null
        validator.validateTargets(d.state, listOf(ChosenTarget.Player(d.player2),
            ChosenTarget.Card(mine, d.player1, Zone.GRAVEYARD)), requirements, d.player1,
            targetingSourceType = TargetingSourceType.SPELL) shouldNotBe null
    }
})
