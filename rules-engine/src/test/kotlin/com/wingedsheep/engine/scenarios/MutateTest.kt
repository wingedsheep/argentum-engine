package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.MutatedEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.mechanics.MergedComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Mutate's casting, merge, abilities and departure (CR 702.140, 730). */
class MutateTest : FunSpec({
    val mutator = card("Mutate Test Beast") {
        manaCost = "{3}{G}"
        typeLine = "Creature — Beast"
        power = 4
        toughness = 4
        keywords(Keyword.REACH, Keyword.TRAMPLE)
        keywordAbility(KeywordAbility.mutate("{1}{G}"))
        triggeredAbility {
            trigger = Triggers.self.mutates()
            effect = Effects.GainLife(3)
        }
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.GainLife(1)
        }
    }
    val tapper = card("Mutate Test Tapper") {
        manaCost = "{G}"
        typeLine = "Creature — Elf"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.GainLife(2)
        }
    }
    val human = card("Mutate Test Human") {
        manaCost = "{G}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
    }
    val removal = card("Mutate Test Removal") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.Destroy(creature)
        }
    }
    val beastToken = card("Mutate Test Token Maker") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.CreateToken(power = 3, toughness = 3, creatureTypes = setOf("Beast")) }
    }

    val timesMutated = DynamicAmounts.propertyOf(EffectTarget.Self, EntityNumericProperty.TimesMutated)
    val counter = card("Mutate Test Counter") {
        manaCost = "{3}{G}"
        typeLine = "Creature — Beast"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.mutate("{G}"))
        triggeredAbility {
            trigger = Triggers.self.mutates()
            effect = Effects.GainLife(timesMutated)
        }
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.GainLife(timesMutated)
        }
    }

    val vanilla = card("Mutate Test Vanilla") {
        manaCost = "{G}"
        typeLine = "Creature — Beast"
        power = 1
        toughness = 1
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(mutator, tapper, human, removal, beastToken, counter, vanilla))
        it.initMirrorMatch(deck = Deck.of("Forest" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.mutate(cardId: EntityId, host: EntityId) = submit(CastSpell(
        playerId = activePlayer!!,
        cardId = cardId,
        targets = listOf(ChosenTarget.Permanent(host)),
        useAlternativeCost = true,
        alternativeCostType = AlternativeCostType.MUTATE,
        paymentStrategy = PaymentStrategy.FromPool,
    ))
    /** Resolve the stack, answering the over/under question with [over] and taking the first legal targets. */
    fun GameTestDriver.resolveAll(over: Boolean = true) {
        repeat(20) {
            val decision = state.pendingDecision
            when {
                decision is YesNoDecision && decision.prompt.startsWith("Mutate") -> submitYesNo(decision.playerId, over)
                decision is ChooseTargetsDecision -> submitDecision(decision.playerId, TargetsResponse(decision.id,
                    decision.legalTargets.mapValues { (_, targets) -> targets.take(1) })).error shouldBe null
                decision != null -> autoResolveDecision()
                stackSize > 0 -> bothPass()
                else -> return
            }
        }
        error("Mutate test did not finish resolving")
    }
    fun GameTestDriver.castFree(name: String, targets: List<EntityId> = emptyList()) {
        val player = activePlayer!!
        castSpell(player, putCardInHand(player, name), targets).outcome shouldBe Outcome.Done
        resolveAll()
    }

    test("mutating over a creature makes the same permanent the top card with every ability, without entering") {
        val game = driver()
        val player = game.activePlayer!!
        val host = game.putCreatureOnBattlefield(player, tapper.name)
        game.removeSummoningSickness(host)
        val beast = game.putCardInHand(player, mutator.name)
        game.giveMana(player, Color.GREEN, 2)
        game.mutate(beast, host).outcome shouldBe Outcome.Done
        game.resolveAll(over = true)

        game.getCreatures(player) shouldBe listOf(host)
        val card = game.state.getEntity(host)!!.get<CardComponent>()!!
        card.name shouldBe mutator.name
        val projected = game.state.projectedState
        projected.getPower(host) shouldBe 4
        projected.hasKeyword(host, Keyword.REACH) shouldBe true
        projected.hasKeyword(host, Keyword.TRAMPLE) shouldBe true
        // The same object: still no summoning sickness, and the mutate trigger — not the ETB — fired.
        game.state.getEntity(host)!!.has<SummoningSicknessComponent>() shouldBe false
        game.getLifeTotal(player) shouldBe 23
        game.state.getEntity(beast)!!.get<CardComponent>()!!.name shouldBe mutator.name
        game.state.zones.values.none { beast in it } shouldBe true
        // The under card's activated ability belongs to the merged permanent.
        game.legalActions(player).any { it.action is com.wingedsheep.engine.core.ActivateAbility &&
            (it.action as com.wingedsheep.engine.core.ActivateAbility).sourceId == host } shouldBe true
    }

    test("mutating under keeps the creature's characteristics and adds the spell's abilities") {
        val game = driver()
        val player = game.activePlayer!!
        val host = game.putCreatureOnBattlefield(player, tapper.name)
        val beast = game.putCardInHand(player, mutator.name)
        game.giveMana(player, Color.GREEN, 2)
        game.mutate(beast, host).outcome shouldBe Outcome.Done
        game.resolveAll(over = false)

        game.state.getEntity(host)!!.get<CardComponent>()!!.name shouldBe tapper.name
        val projected = game.state.projectedState
        projected.getPower(host) shouldBe 1
        projected.hasKeyword(host, Keyword.TRAMPLE) shouldBe true
        game.getLifeTotal(player) shouldBe 23
    }

    test("counters and the permanent's identity survive the merge") {
        val game = driver()
        val player = game.activePlayer!!
        val host = game.putCreatureOnBattlefield(player, tapper.name)
        game.addComponent(host, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
        val beast = game.putCardInHand(player, mutator.name)
        game.giveMana(player, Color.GREEN, 2)
        game.mutate(beast, host)
        game.resolveAll(over = true)
        game.state.projectedState.getPower(host) shouldBe 5
    }

    test("a mutating spell whose target is gone resolves as an ordinary creature spell") {
        val game = driver()
        val player = game.activePlayer!!
        val host = game.putCreatureOnBattlefield(player, tapper.name)
        val beast = game.putCardInHand(player, mutator.name)
        game.giveMana(player, Color.GREEN, 2)
        game.mutate(beast, host).outcome shouldBe Outcome.Done
        // Kill the target in response.
        game.castSpell(player, game.putCardInHand(player, removal.name), listOf(host)).outcome shouldBe Outcome.Done
        game.resolveAll()

        game.findPermanent(player, mutator.name) shouldBe beast
        game.state.getEntity(beast)!!.has<MergedComponent>() shouldBe false
        // Entered (ETB +1), didn't mutate (no +3).
        game.getLifeTotal(player) shouldBe 21
    }

    test("mutate targets only a non-Human creature with the spell's owner") {
        val game = driver()
        val player = game.activePlayer!!
        val opponent = game.getOpponent(player)
        val humanId = game.putCreatureOnBattlefield(player, human.name)
        val theirs = game.putCreatureOnBattlefield(opponent, tapper.name)
        val ours = game.putCreatureOnBattlefield(player, tapper.name)
        val beast = game.putCardInHand(player, mutator.name)
        game.giveMana(player, Color.GREEN, 2)

        val offer = game.legalActions(player).single {
            (it.action as? CastSpell)?.alternativeCostType == AlternativeCostType.MUTATE
        }
        offer.validTargets!!.toSet() shouldBe setOf(ours)
        game.mutate(beast, humanId).error shouldNotBe null
        game.mutate(beast, theirs).error shouldNotBe null
        game.stackSize shouldBe 0
    }

    test("a merged permanent that leaves puts every card into the graveyard") {
        val game = driver()
        val player = game.activePlayer!!
        val host = game.putCreatureOnBattlefield(player, tapper.name)
        val beast = game.putCardInHand(player, mutator.name)
        game.giveMana(player, Color.GREEN, 2)
        game.mutate(beast, host)
        game.resolveAll(over = true)
        game.castFree(removal.name, listOf(host))

        game.getCreatures(player) shouldBe emptyList()
        game.getGraveyardCardNames(player) shouldContainExactlyInAnyOrder listOf(tapper.name, mutator.name, removal.name)
        game.state.getEntity(host)!!.get<CardComponent>()!!.name shouldBe tapper.name
        game.state.getEntity(host)!!.has<MergedComponent>() shouldBe false
    }

    test("a token under a card stops being a token, and only its card survives the departure") {
        val game = driver()
        val player = game.activePlayer!!
        game.castFree(beastToken.name)
        val token = game.getCreatures(player).single()
        val beast = game.putCardInHand(player, mutator.name)
        game.giveMana(player, Color.GREEN, 2)
        game.mutate(beast, token)
        game.resolveAll(over = true)
        game.state.getEntity(token)!!.has<TokenComponent>() shouldBe false

        game.castFree(removal.name, listOf(token))
        game.getGraveyardCardNames(player) shouldContainExactlyInAnyOrder listOf(mutator.name, beastToken.name, removal.name)
    }

    test("a card under a token leaves the permanent a token") {
        val game = driver()
        val player = game.activePlayer!!
        game.castFree(beastToken.name)
        val token = game.getCreatures(player).single()
        val beast = game.putCardInHand(player, mutator.name)
        game.giveMana(player, Color.GREEN, 2)
        game.mutate(beast, token)
        game.resolveAll(over = false)
        game.state.getEntity(token)!!.has<TokenComponent>() shouldBe true
        game.state.projectedState.hasKeyword(token, Keyword.REACH) shouldBe true
        game.state.projectedState.getPower(token) shouldBe 3
    }

    test("mutating onto a mutated permanent adds another component and triggers each mutate ability") {
        val game = driver()
        val player = game.activePlayer!!
        val host = game.putCreatureOnBattlefield(player, tapper.name)
        game.giveMana(player, Color.GREEN, 4)
        game.mutate(game.putCardInHand(player, mutator.name), host)
        game.resolveAll(over = true)
        game.getLifeTotal(player) shouldBe 23
        game.mutate(game.putCardInHand(player, mutator.name), host)
        game.resolveAll(over = false)

        game.state.getEntity(host)!!.get<MergedComponent>()!!.componentIds.size shouldBe 3
        // Two copies of the mutate trigger now — each fires.
        game.getLifeTotal(player) shouldBe 29
        game.getCreatures(player) shouldBe listOf(host)
    }

    test("the merge emits a MutatedEvent naming the permanent") {
        val game = driver()
        val player = game.activePlayer!!
        val host = game.putCreatureOnBattlefield(player, tapper.name)
        val beast = game.putCardInHand(player, mutator.name)
        game.giveMana(player, Color.GREEN, 2)
        game.mutate(beast, host)
        game.bothPass()
        val decision = game.state.pendingDecision as YesNoDecision
        val result = game.submitYesNo(decision.playerId, true)
        val event = result.events.filterIsInstance<MutatedEvent>().single()
        event.permanentId shouldBe host
        event.mutatingCardId shouldBe beast
        event.onTop shouldBe true
        game.state.getEntity(host)!!.get<CardComponent>()!!.cardDefinitionId shouldNotBe mutator.name
    }

    test("the number of times a permanent has mutated counts every merge, over or under, including the one that triggered") {
        val game = driver()
        val player = game.activePlayer!!
        val host = game.putCreatureOnBattlefield(player, vanilla.name)
        game.removeSummoningSickness(host)
        game.giveMana(player, Color.GREEN, 2)

        game.mutate(game.putCardInHand(player, counter.name), host)
        game.resolveAll(over = false)
        // First merge: the mutate trigger already sees 1.
        game.getLifeTotal(player) shouldBe 21

        game.mutate(game.putCardInHand(player, counter.name), host)
        game.resolveAll(over = true)
        // Second merge: two copies of the trigger now, each reading 2.
        game.getLifeTotal(player) shouldBe 25

        // Outside a trigger the count is still read off the permanent.
        val tap = game.legalActions(player).map { it.action }
            .filterIsInstance<com.wingedsheep.engine.core.ActivateAbility>()
            .first { it.sourceId == host }
        game.submit(tap).error shouldBe null
        game.resolveAll()
        game.getLifeTotal(player) shouldBe 27
    }

    test("a creature that never mutated has mutated zero times") {
        val game = driver()
        val player = game.activePlayer!!
        val plain = game.putCreatureOnBattlefield(player, counter.name)
        game.removeSummoningSickness(plain)
        val tap = game.legalActions(player).map { it.action }
            .filterIsInstance<com.wingedsheep.engine.core.ActivateAbility>()
            .first { it.sourceId == plain }
        game.submit(tap).error shouldBe null
        game.resolveAll()
        game.getLifeTotal(player) shouldBe 20
    }

    test("a mutated source that leaves before its ability resolves is read as it last existed") {
        val game = driver()
        val player = game.activePlayer!!
        val host = game.putCreatureOnBattlefield(player, vanilla.name)
        game.removeSummoningSickness(host)
        game.giveMana(player, Color.GREEN, 2)
        game.mutate(game.putCardInHand(player, counter.name), host)
        game.resolveAll(over = true)
        game.mutate(game.putCardInHand(player, counter.name), host)
        game.resolveAll(over = true)
        game.getLifeTotal(player) shouldBe 25

        val tap = game.legalActions(player).map { it.action }
            .filterIsInstance<com.wingedsheep.engine.core.ActivateAbility>()
            .first { it.sourceId == host }
        game.submit(tap).error shouldBe null
        // Destroy the merged permanent in response: the ability still counts its two mutations.
        game.castSpell(player, game.putCardInHand(player, removal.name), listOf(host)).outcome shouldBe Outcome.Done
        game.resolveAll()
        game.state.getEntity(host)!!.has<MergedComponent>() shouldBe false
        game.getLifeTotal(player) shouldBe 27
    }
})
