package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A "create a token that's a copy of that permanent" payoff iterating a cast trigger's captured
 * target copies it as it last existed on the battlefield (CR 608.2h) when it left before the
 * ability resolved: a departed Clone as what it was copying, and a departed token from the values
 * frozen when the ability triggered.
 */
class CapturedTargetTokenCopyLkiTest : FunSpec({
    val duplicator = card("Lki Test Duplicator") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.you.casts(
                requires = setOf(SpellCastPredicate.TargetsMatching(GameObjectFilter.Creature.youControl(), only = true))
            )
            effect = Effects.ForEachInCollection(
                CollectionSlot.TriggerCaptured,
                Effects.CreateTokenCopyOfTarget(EffectTarget.IterationEntity)
            )
        }
    }
    val copier = card("Lki Test Copier") {
        manaCost = "{U}"; typeLine = "Creature — Shapeshifter"; power = 0; toughness = 1
        replacementEffect(EntersAsCopy())
    }
    val bear = card("Lki Test Bear") {
        manaCost = "{G}"; typeLine = "Creature — Bear"; power = 2; toughness = 2
    }
    val pump = card("Lki Test Pump") {
        manaCost = "{G}"; typeLine = "Instant"
        spell { effect = Effects.ModifyStats(1, 1, target(TargetFilter.Creature)) }
    }
    val kill = card("Lki Test Kill") {
        manaCost = "{B}"; typeLine = "Instant"
        spell { effect = Effects.Destroy(target(TargetFilter.Creature)) }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(duplicator, copier, bear, pump, kill))
        initMirrorMatch(deck = Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        putPermanentOnBattlefield(player1, duplicator.name)
    }

    fun GameTestDriver.tokens(): List<EntityId> =
        getPermanents(player1).filter { state.getEntity(it)?.has<TokenComponent>() == true }

    fun GameTestDriver.pumpThenOpponentKills(targetId: EntityId) {
        giveMana(player1, Color.GREEN, 1)
        castSpell(player1, putCardInHand(player1, pump.name), listOf(targetId)).error shouldBe null
        stackSize shouldBe 2
        passPriority(player1)
        giveMana(player2, Color.BLACK, 1)
        castSpell(player2, putCardInHand(player2, kill.name), listOf(targetId)).error shouldBe null
        while (stackSize > 0) bothPass()
    }

    test("a departed Clone is copied as what it was copying, not as the card in the graveyard") {
        val d = driver()
        val bearId = d.putCreatureOnBattlefield(d.player2, bear.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        val cloneId = d.putCardInHand(d.player1, copier.name)
        d.castSpell(d.player1, cloneId).error shouldBe null
        d.bothPass()
        d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(d.player1, listOf(bearId)).error shouldBe null
        d.getCardName(cloneId) shouldBe bear.name

        d.pumpThenOpponentKills(cloneId)

        withClue("the Clone died") { d.getGraveyard(d.player1).contains(cloneId) shouldBe true }
        val token = d.tokens().single()
        d.getCardName(token) shouldBe bear.name
        d.state.projectedState.getPower(token) shouldBe 2
    }

    test("a token that ceased to exist is copied from the values frozen at trigger time") {
        val d = driver()
        val bearId = d.putCreatureOnBattlefield(d.player1, bear.name)
        d.giveMana(d.player1, Color.GREEN, 1)
        d.castSpell(d.player1, d.putCardInHand(d.player1, pump.name), listOf(bearId)).error shouldBe null
        while (d.stackSize > 0) d.bothPass()
        val first = d.tokens().single()

        d.pumpThenOpponentKills(first)

        d.state.getEntity(first) shouldBe null
        val second = d.tokens().single()
        d.getCardName(second) shouldBe bear.name
    }
})
