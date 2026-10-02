package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `MayCastSelfFromZones.castUsing` — "You may cast this card from your graveyard using its bestow
 * ability" (CR 702.103a: bestow functions in any zone the card could be cast from). The permission
 * authorizes only the bestowed cast, for the bestow price; an unknown casting keyword authorizes
 * nothing; the restriction never reaches the card's casts from hand.
 */
class CastUsingPermissionTest : FunSpec({
    val host = card("Cast Using Host") {
        manaCost = "{W}"
        typeLine = "Creature — Soldier"
        power = 1
        toughness = 1
    }
    val spirit = card("Cast Using Spirit") {
        manaCost = "{4}{W}"
        typeLine = "Enchantment Creature — Spirit"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.bestow("{1}{W}"))
        staticAbility { ability = MayCastSelfFromZones(listOf(Zone.GRAVEYARD), castUsing = Keyword.BESTOW) }
    }
    val unknown = card("Cast Using Unknown") {
        manaCost = "{4}{W}"
        typeLine = "Enchantment Creature — Spirit"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.bestow("{1}{W}"))
        staticAbility { ability = MayCastSelfFromZones(listOf(Zone.GRAVEYARD), castUsing = Keyword.FLYING) }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(host, spirit, unknown))
        it.initMirrorMatch(deck = Deck.of("Plains" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castsOf(player: EntityId, card: EntityId) =
        legalActions(player).mapNotNull { it.action as? CastSpell }.filter { it.cardId == card }

    fun bestow(player: EntityId, card: EntityId, target: EntityId) = CastSpell(
        player, card, targets = listOf(ChosenTarget.Permanent(target)),
        useAlternativeCost = true, alternativeCostType = AlternativeCostType.BESTOW,
        paymentStrategy = PaymentStrategy.FromPool
    )

    test("from the graveyard only the bestowed cast is offered, and it costs the bestow price") {
        val game = driver()
        val me = game.activePlayer!!
        val creature = game.putCreatureOnBattlefield(me, host.name)
        val card = game.putCardInGraveyard(me, spirit.name)
        game.giveMana(me, Color.WHITE, 2)

        game.castsOf(me, card).map { it.alternativeCostType } shouldBe listOf(AlternativeCostType.BESTOW)
        game.submit(bestow(me, card, creature)).error shouldBe null
        while (game.state.stack.isNotEmpty()) game.bothPass()

        game.state.getBattlefield() shouldContain card
        game.state.getEntity(card)?.get<AttachedToComponent>()?.targetId shouldBe creature
        game.state.projectedState.isCreature(card) shouldBe false
    }

    test("an ordinary cast from the graveyard is rejected even with its full mana cost available") {
        val game = driver()
        val me = game.activePlayer!!
        val card = game.putCardInGraveyard(me, spirit.name)
        game.giveMana(me, Color.WHITE, 5)

        game.submit(CastSpell(me, card, paymentStrategy = PaymentStrategy.FromPool)).error shouldNotBe null
        game.state.getGraveyard(me) shouldContain card
    }

    test("a casting keyword the engine can't cast with authorizes nothing") {
        val game = driver()
        val me = game.activePlayer!!
        val creature = game.putCreatureOnBattlefield(me, host.name)
        val card = game.putCardInGraveyard(me, unknown.name)
        game.giveMana(me, Color.WHITE, 5)

        game.castsOf(me, card).shouldBeEmpty()
        game.submit(bestow(me, card, creature)).error shouldNotBe null
        game.submit(CastSpell(me, card, paymentStrategy = PaymentStrategy.FromPool)).error shouldNotBe null
    }

    test("the restriction is graveyard-scoped: from hand both casts are offered") {
        val game = driver()
        val me = game.activePlayer!!
        game.putCreatureOnBattlefield(me, host.name)
        val card = game.putCardInHand(me, spirit.name)
        game.giveMana(me, Color.WHITE, 5)

        game.castsOf(me, card).map { it.alternativeCostType }.toSet() shouldBe setOf(null, AlternativeCostType.BESTOW)
    }
})
