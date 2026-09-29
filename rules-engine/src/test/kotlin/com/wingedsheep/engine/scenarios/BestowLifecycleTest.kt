package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.mechanics.BestowedComponent
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BestowLifecycleTest : FunSpec({
    fun GameTestDriver.answerPendingDecision() {
        val decision = state.pendingDecision
        if (decision is ChooseTargetsDecision) {
            submitDecision(decision.playerId, TargetsResponse(
                decision.id, decision.legalTargets.mapValues { (_, targets) -> targets.take(1) }
            )).error shouldBe null
        } else if (decision != null) {
            autoResolveDecision()
        }
    }

    val spirit = card("Bestow Lifecycle Spirit") {
        manaCost = "{W}"
        typeLine = "Enchantment Creature — Spirit"
        power = 2
        toughness = 3
        keywordAbility(KeywordAbility.bestow("{0}"))
    }

    for (newType in listOf("LAND", "ENCHANTMENT")) {
        test("a bestowed permanent transformed into a plain $newType becomes unattached") {
            val transformation = card("Bestow Lifecycle Transformation") {
                manaCost = "{0}"
                typeLine = "Enchantment — Aura"
                auraTarget = TargetObject(filter = TargetFilter.Permanent)
                staticAbility {
                    ability = TransformPermanent(setCardTypes = setOf(newType), clearSubtypes = true)
                }
            }
            val game = GameTestDriver()
            game.registerCards(TestCards.all + listOf(spirit, transformation))
            game.initMirrorMatch(deck = Deck.of("Plains" to 40))
            game.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val player = game.activePlayer!!
            val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
            val bestowed = game.putCardInHand(player, spirit.name)
            game.submit(CastSpell(
                playerId = player,
                cardId = bestowed,
                targets = listOf(ChosenTarget.Permanent(host)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.BESTOW,
                paymentStrategy = PaymentStrategy.FromPool
            )).error shouldBe null
            game.bothPass()
            game.state.getEntity(bestowed)?.get<AttachedToComponent>()?.targetId shouldBe host

            game.castSpell(player, game.putCardInHand(player, transformation.name), listOf(bestowed)).error shouldBe null
            game.bothPass()

            game.findPermanent(player, spirit.name) shouldBe bestowed
            game.state.projectedState.hasType(bestowed, newType) shouldBe true
            game.state.projectedState.isCreature(bestowed) shouldBe false
            game.state.projectedState.hasSubtype(bestowed, "Aura") shouldBe false
            game.state.getEntity(bestowed)?.has<BestowedComponent>() shouldBe false
            game.state.getEntity(bestowed)?.get<AttachedToComponent>() shouldBe null
            game.state.getEntity(host)?.get<AttachmentsComponent>()?.attachedIds.orEmpty().contains(bestowed) shouldBe false
        }
    }

    test("bestow ends before later instructions after the host leaves") {
        val removal = card("Bestow Lifecycle Removal And Count") {
            manaCost = "{0}"
            typeLine = "Instant"
            spell {
                val host = target(TargetFilter.Creature)
                effect = Effects.Move(host, Zone.GRAVEYARD) then
                    Effects.GainLife(DynamicAmounts.creaturesYouControl())
            }
        }
        val game = GameTestDriver()
        game.registerCards(TestCards.all + listOf(spirit, removal))
        game.initMirrorMatch(deck = Deck.of("Plains" to 40))
        game.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = game.activePlayer!!
        val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
        val bestowed = game.putCardInHand(player, spirit.name)
        game.submit(CastSpell(
            playerId = player,
            cardId = bestowed,
            targets = listOf(ChosenTarget.Permanent(host)),
            useAlternativeCost = true,
            alternativeCostType = AlternativeCostType.BESTOW,
            paymentStrategy = PaymentStrategy.FromPool
        )).error shouldBe null
        game.bothPass()

        game.castSpell(player, game.putCardInHand(player, removal.name), listOf(host)).error shouldBe null
        game.bothPass()

        game.getLifeTotal(player) shouldBe 21
        game.findPermanent(player, spirit.name) shouldBe bestowed
        game.state.projectedState.isCreature(bestowed) shouldBe true
        game.state.getEntity(bestowed)?.has<BestowedComponent>() shouldBe false
        game.state.getEntity(bestowed)?.get<AttachedToComponent>() shouldBe null
    }

    for (removeHost in listOf(false, true)) {
        test("a bestowed spell copy keeps nonlegendary and token keywords when host removal is $removeHost") {
            val legendarySpirit = card("Bestow Lifecycle Legendary Spirit") {
                manaCost = "{W}"
                typeLine = "Legendary Enchantment Creature — Spirit"
                power = 2
                toughness = 3
                keywordAbility(KeywordAbility.bestow("{0}"))
            }
            val copySpell = card("Bestow Lifecycle Copy") {
                manaCost = "{0}"
                typeLine = "Instant"
                spell {
                    val original = target(TargetFilter.SpellOnStack)
                    effect = Effects.CopyTargetSpell(
                        original, removeLegendary = true, addedTokenKeywords = setOf(Keyword.HASTE)
                    )
                }
            }
            val removal = card("Bestow Lifecycle Host Removal") {
                manaCost = "{0}"
                typeLine = "Instant"
                spell {
                    val host = target(TargetFilter.Creature)
                    effect = Effects.Move(host, Zone.GRAVEYARD)
                }
            }
            val game = GameTestDriver()
            game.registerCards(TestCards.all + listOf(legendarySpirit, copySpell, removal))
            game.initMirrorMatch(deck = Deck.of("Plains" to 40))
            game.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val player = game.activePlayer!!
            val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
            val bestowed = game.putCardInHand(player, legendarySpirit.name)
            game.submit(CastSpell(
                playerId = player,
                cardId = bestowed,
                targets = listOf(ChosenTarget.Permanent(host)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.BESTOW,
                paymentStrategy = PaymentStrategy.FromPool
            )).error shouldBe null
            game.castSpellWithTargets(player, game.putCardInHand(player, copySpell.name),
                listOf(ChosenTarget.Spell(game.state.stack.single()))).error shouldBe null
            game.bothPass()
            while (game.state.pendingDecision != null) game.answerPendingDecision()
            if (removeHost) {
                game.castSpell(player, game.putCardInHand(player, removal.name), listOf(host)).error shouldBe null
            }
            repeat(10) {
                if (game.state.pendingDecision != null) game.answerPendingDecision()
                else if (game.stackSize > 0) game.bothPass()
            }
            game.stackSize shouldBe 0
            val copies = game.state.getBattlefield().filter { id ->
                game.state.getEntity(id)?.has<TokenComponent>() == true
            }
            copies.size shouldBe 1
            val copy = copies.single()
            game.state.projectedState.isLegendary(copy) shouldBe false
            game.state.projectedState.hasKeyword(copy, Keyword.HASTE) shouldBe true
            game.state.projectedState.isCreature(copy) shouldBe removeHost
            game.state.getEntity(copy)?.get<AttachedToComponent>()?.targetId shouldBe
                (if (removeHost) null else host)
        }
    }
})
