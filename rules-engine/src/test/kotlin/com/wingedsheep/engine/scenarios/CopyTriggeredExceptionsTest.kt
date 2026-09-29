package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CopyTriggeredExceptionsTest : FunSpec({
    val copier = card("Test Trigger Copier") {
        manaCost = "{U}"
        typeLine = "Creature — Shapeshifter"
        power = 1; toughness = 1
        replacementEffect(EntersAsCopy(exceptions = CopyExceptions(addedTriggeredAbilities = listOf(
            grantedTriggeredAbility { trigger = Triggers.self.enters(); effect = Effects.GainLife(2) },
            grantedTriggeredAbility { trigger = Triggers.self.dies(); effect = Effects.GainLife(3) },
            grantedTriggeredAbility { trigger = Triggers.you.beginningOf(Step.UPKEEP); effect = Effects.GainLife(4) }
        ))))
    }
    val landCopier = card("Test Trigger Copy Land") {
        typeLine = "Land"
        replacementEffect(EntersAsCopy(
            copyFilter = Filters.Land,
            exceptions = CopyExceptions(addedTriggeredAbilities = listOf(
                grantedTriggeredAbility { trigger = Triggers.self.enters(); effect = Effects.GainLife(2) }
            ))
        ))
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(copier, landCopier))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.copy(name: String = copier.name): com.wingedsheep.sdk.model.EntityId {
        val bear = putPermanentOnBattlefield(player2, "Grizzly Bears")
        val id = putCardInHand(player1, name)
        giveMana(player1, Color.BLUE, 1)
        castSpell(player1, id).error shouldBe null
        bothPass()
        submitCardSelection(player1, listOf(bear)).error shouldBe null
        return id
    }
    test("entry exception triggers immediately and copied card characteristics remain unchanged") {
        val d = driver()
        val id = d.copy()
        d.state.stack.size shouldBe 1
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 22
        d.state.getEntity(id)!!.get<CardComponent>()!!.name shouldBe "Grizzly Bears"
        val view = com.wingedsheep.engine.view.ClientStateTransformer(
            cardRegistry = d.cardRegistry,
            predicateEvaluator = com.wingedsheep.engine.handlers.PredicateEvaluator(cardRegistry = d.cardRegistry)
        ).transform(d.state, viewingPlayerId = d.player2)
        view.cards[id]!!.activeEffects.count { it.effectId.startsWith("copy_trig_") } shouldBe 3
        d.replaceState(d.state.updateEntity(id) {
            it.with(com.wingedsheep.engine.state.components.identity.FaceDownComponent)
        })
        val hiddenView = com.wingedsheep.engine.view.ClientStateTransformer(
            cardRegistry = d.cardRegistry,
            predicateEvaluator = com.wingedsheep.engine.handlers.PredicateEvaluator(cardRegistry = d.cardRegistry)
        ).transform(d.state, viewingPlayerId = d.player2)
        hiddenView.cards[id]!!.activeEffects.none { it.effectId.startsWith("copy_trig_") } shouldBe true
    }
    test("death exception is read from the departed identity after zone cleanup") {
        val d = driver()
        val id = d.copy()
        d.bothPass()
        // Use an inline spell so the normal settle boundary detects the zone-change event.
        val removal = card("Test Kill Copy") {
            manaCost = "{B}"; typeLine = "Sorcery"
            spell { val t = target(com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.Creature); effect = Effects.Destroy(t) }
        }
        d.registerCards(listOf(removal))
        val spell = d.putCardInHand(d.player1, removal.name)
        d.giveMana(d.player1, Color.BLACK, 1)
        d.castSpellWithTargets(d.player1, spell, listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(id))).error shouldBe null
        d.bothPass()
        d.state.getEntity(id)!!.get<CardComponent>()!!.copyTriggeredAbilities shouldBe emptyList()
        d.state.stack.size shouldBe 1
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 25
    }
    test("land entry carries exceptions through the non-spell continuation") {
        val d = driver()
        val island = d.putPermanentOnBattlefield(d.player2, "Island")
        val id = d.putCardInHand(d.player1, landCopier.name)
        d.playLand(d.player1, id).error shouldBe null
        d.submitCardSelection(d.player1, listOf(island)).error shouldBe null
        d.state.getEntity(id)!!.get<CardComponent>()!!.copyTriggeredAbilities.size shouldBe 1
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 22
    }
    test("declining applies neither entry nor phase trigger exceptions") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = d.putCardInHand(d.player1, copier.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpell(d.player1, id).error shouldBe null
        d.bothPass()
        d.submitCardSelection(d.player1, emptyList()).error shouldBe null
        d.state.stack.size shouldBe 0
        d.state.getEntity(id)!!.get<CardComponent>()!!.copyTriggeredAbilities shouldBe emptyList()
        d.getLifeTotal(d.player1) shouldBe 20
    }
    test("phase trigger is intrinsic and ability removal suppresses it") {
        val d = driver()
        val id = d.copy()
        d.bothPass()
        // Advance around the opponent's turn to the copy controller's upkeep.
        d.passPriorityUntil(Step.UPKEEP)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.UPKEEP)
        d.state.stack.size shouldBe 1
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 26
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val suppress = card("Test Suppress Copy") {
            manaCost = "{U}"; typeLine = "Sorcery"
            spell {
                val t = target(com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.Creature)
                effect = Effects.RemoveAllAbilities(t, com.wingedsheep.sdk.scripting.Duration.Permanent)
            }
        }
        d.registerCards(listOf(suppress))
        val spell = d.putCardInHand(d.player1, suppress.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpellWithTargets(d.player1, spell, listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(id))).error shouldBe null
        d.bothPass()
        d.passPriorityUntil(Step.UPKEEP)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.UPKEEP)
        d.state.stack.size shouldBe 0
        d.getLifeTotal(d.player1) shouldBe 26
    }
    test("a token copy inherits entry exceptions and triggers them from the new identity") {
        val d = driver()
        val id = d.copy()
        d.bothPass()
        val tokenSpell = card("Test Token Copy") {
            manaCost = "{U}"; typeLine = "Sorcery"
            spell {
                val t = target(com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.Creature)
                effect = Effects.CreateTokenCopyOfTarget(t)
            }
        }
        d.registerCards(listOf(tokenSpell))
        val spell = d.putCardInHand(d.player1, tokenSpell.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpellWithTargets(d.player1, spell, listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(id))).error shouldBe null
        d.bothPass()
        d.state.stack.size shouldBe 1
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 24
    }

})
