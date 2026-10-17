package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.player.CantCastSpellsExecutor
import com.wingedsheep.engine.state.components.player.CantCastSpellsComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CantCastSpellsEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class FilteredCastRestrictionTest : FunSpec({
    val creature = card("Cast Ban Creature") {
        manaCost = "{0}"; typeLine = "Artifact Creature — Golem"; power = 2; toughness = 2
    }
    val instant = card("Cast Ban Instant") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }
    val adventure = creature.copy(
        name = "Cast Ban Adventure", layout = CardLayout.ADVENTURE,
        cardFaces = listOf(CardFace("Ban Adventure Spell", ManaCost.ZERO, TypeLine.parse("Instant"),
            script = instant.script))
    )
    val xInstant = card("Cast Ban X Instant") {
        manaCost = "{X}"; typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }
    val morph = card("Cast Ban Morph") {
        manaCost = "{0}"; typeLine = "Enchantment"; morph = "{0}"
    }
    val modal = morph.copy(name = "Cast Ban Modal", layout = CardLayout.MODAL_DFC,
        keywordAbilities = emptyList(), backFace = creature.copy(name = "Cast Ban Modal Back"))
    val modalCreatureFront = creature.copy(name = "Cast Ban Creature Modal", layout = CardLayout.MODAL_DFC,
        backFace = morph.copy(name = "Cast Ban Enchantment Back", keywordAbilities = emptyList()))
    val disturb = creature.copy(name = "Cast Ban Disturb",
        keywordAbilities = listOf(KeywordAbility.disturb("{0}")),
        backFace = morph.copy(name = "Cast Ban Disturb Back", keywordAbilities = emptyList()))
    val suspendCreature = creature.copy(name = "Cast Ban Suspend Creature",
        keywordAbilities = listOf(KeywordAbility.suspend("{0}", 2)))
    val suspendInstant = instant.copy(name = "Cast Ban Suspend Instant",
        keywordAbilities = listOf(KeywordAbility.suspend("{0}", 2)))
    val bestowed = card("Cast Ban Bestow") {
        manaCost = "{0}"; typeLine = "Enchantment Creature — Spirit"; power = 1; toughness = 1
        keywordAbility(KeywordAbility.bestow("{0}"))
    }
    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(creature, instant, adventure, morph, modal, modalCreatureFront, disturb,
            suspendCreature, suspendInstant, bestowed, xInstant))
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun ban(d: GameTestDriver, filter: GameObjectFilter, duration: Duration = Duration.EndOfTurn) {
        val result = CantCastSpellsExecutor(d.services.predicateEvaluator.amounts).execute(d.state,
            CantCastSpellsEffect(EffectTarget.PlayerRef(Player.You), duration, filter),
            EffectContext(sourceId = null, controllerId = d.activePlayer!!))
        result.outcome shouldBe Outcome.Done
        result.events shouldBe listOf(PlayerActionPermissionsChangedEvent(d.activePlayer!!))
        d.replaceState(result.state)
    }
    fun offers(d: GameTestDriver, id: EntityId): List<CastSpell> =
        d.legalActions(d.activePlayer!!).mapNotNull { it.action as? CastSpell }.filter { it.cardId == id }

    test("a filtered ban rejects noncreatures in both enumeration and authoritative validation") {
        val d = setup(); val me = d.activePlayer!!
        ban(d, GameObjectFilter.Noncreature)
        // The cards arrive after resolution: the ban changes the rules, not a fixed set of cards.
        val spell = d.putCardInHand(me, instant.name)
        val body = d.putCardInHand(me, creature.name)
        offers(d, spell).isEmpty() shouldBe true
        d.submitExpectFailure(CastSpell(me, spell))
        offers(d, body).isNotEmpty() shouldBe true
        d.submitSuccess(CastSpell(me, body))
    }
    test("the filter is honored for casts granted from exile and graveyard") {
        for (fromGraveyard in listOf(false, true)) {
            val d = setup(); val me = d.activePlayer!!
            val spell = if (fromGraveyard) d.putCardInGraveyard(me, instant.name) else d.putCardInExile(me, instant.name)
            val body = if (fromGraveyard) d.putCardInGraveyard(me, creature.name) else d.putCardInExile(me, creature.name)
            d.replaceState(d.state.copy(mayPlayPermissions = listOf(
                MayPlayPermission(EntityId.generate(), setOf(spell, body), me, timestamp = d.state.timestamp)
            )))
            ban(d, GameObjectFilter.Noncreature)
            offers(d, spell).isEmpty() shouldBe true
            d.submitExpectFailure(CastSpell(me, spell))
            offers(d, body).isNotEmpty() shouldBe true
            d.submitSuccess(CastSpell(me, body))
        }
    }
    test("Adventure is judged by the chosen face") {
        val d = setup(); val me = d.activePlayer!!
        val id = d.putCardInHand(me, adventure.name)
        ban(d, GameObjectFilter.Noncreature)
        offers(d, id).any { it.faceIndex == null } shouldBe true
        offers(d, id).any { it.faceIndex == 0 } shouldBe false
        d.submitExpectFailure(CastSpell(me, id, faceIndex = 0))
        d.submitSuccess(CastSpell(me, id))
    }
    test("a banned creature front does not hide its legal Adventure") {
        val d = setup(); val me = d.activePlayer!!
        val id = d.putCardInHand(me, adventure.name)
        ban(d, GameObjectFilter.Creature)
        offers(d, id).any { it.faceIndex == null } shouldBe false
        val action = offers(d, id).first { it.faceIndex == 0 }
        d.submitExpectFailure(CastSpell(me, id))
        d.submitSuccess(action)
    }
    test("a noncreature card can still be cast face down as a creature") {
        val d = setup(); val me = d.activePlayer!!
        repeat(3) { d.putLandOnBattlefield(me, "Plains") }
        val id = d.putCardInHand(me, morph.name)
        ban(d, GameObjectFilter.Noncreature)
        offers(d, id).any { !it.castFaceDown } shouldBe false
        val action = offers(d, id).first { it.castFaceDown }
        d.submitExpectFailure(CastSpell(me, id))
        d.submitSuccess(action.copy(paymentStrategy = PaymentStrategy.AutoPay))
    }
    test("a modal creature back remains legal when its enchantment front is banned") {
        val d = setup(); val me = d.activePlayer!!
        val id = d.putCardInHand(me, modal.name)
        ban(d, GameObjectFilter.Noncreature)
        offers(d, id).any { !it.useAlternativeCost } shouldBe false
        val action = offers(d, id).first { it.alternativeCostType == AlternativeCostType.MODAL_BACK_FACE }
        d.submitExpectFailure(CastSpell(me, id))
        d.submitSuccess(action)
    }
    test("legacy alternative-cost declarations judge each modal back rather than its front") {
        for (definition in listOf(modal, modalCreatureFront)) {
            for (filter in listOf(GameObjectFilter.Creature, GameObjectFilter.Noncreature)) {
                val d = setup(); val me = d.activePlayer!!
                val id = d.putCardInHand(me, definition.name)
                ban(d, filter)
                val banned = definition.backFace!!.typeLine.isCreature == (filter == GameObjectFilter.Creature)
                offers(d, id).any { it.alternativeCostType == AlternativeCostType.MODAL_BACK_FACE } shouldBe !banned
                val action = CastSpell(me, id, useAlternativeCost = true)
                if (banned) d.submitExpectFailure(action) else d.submitSuccess(action)
            }
        }
    }
    test("legacy disturb declarations judge the enchantment back rather than its creature front") {
        for (filter in listOf(GameObjectFilter.Creature, GameObjectFilter.Noncreature)) {
            val d = setup(); val me = d.activePlayer!!
            val id = d.putCardInGraveyard(me, disturb.name)
            ban(d, filter)
            val banned = filter == GameObjectFilter.Noncreature
            offers(d, id).any { it.alternativeCostType == AlternativeCostType.DISTURB } shouldBe !banned
            val action = CastSpell(me, id, useAlternativeCost = true)
            if (banned) d.submitExpectFailure(action) else d.submitSuccess(action)
        }
    }
    test("suspend offers honor filtered bans just like authoritative validation") {
        val d = setup(); val me = d.activePlayer!!
        val body = d.putCardInHand(me, suspendCreature.name)
        val spell = d.putCardInHand(me, suspendInstant.name)
        ban(d, GameObjectFilter.Noncreature)
        val suspendOffers = d.legalActions(me).mapNotNull { it.action as? SuspendCardFromHand }
        suspendOffers.any { it.cardId == spell } shouldBe false
        suspendOffers.any { it.cardId == body } shouldBe true
        d.submitExpectFailure(SuspendCardFromHand(me, spell))
        d.submitSuccess(SuspendCardFromHand(me, body))
    }
    test("bestow is a noncreature spell while the ordinary enchantment creature remains legal") {
        val d = setup(); val me = d.activePlayer!!
        val host = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val id = d.putCardInHand(me, bestowed.name)
        ban(d, GameObjectFilter.Noncreature)
        offers(d, id).any { !it.useAlternativeCost } shouldBe true
        offers(d, id).any { it.alternativeCostType == AlternativeCostType.BESTOW } shouldBe false
        d.submitExpectFailure(CastSpell(me, id, useAlternativeCost = true,
            alternativeCostType = AlternativeCostType.BESTOW, targets = listOf(ChosenTarget.Permanent(host))))
        d.submitSuccess(CastSpell(me, id))
    }
    test("blanket and filtered bans accumulate in either order") {
        for (filters in listOf(listOf(GameObjectFilter.Any, GameObjectFilter.Noncreature),
            listOf(GameObjectFilter.Noncreature, GameObjectFilter.Any))) {
            val d = setup(); val me = d.activePlayer!!
            filters.forEach { ban(d, it) }
            val id = d.putCardInHand(me, creature.name)
            offers(d, id).isEmpty() shouldBe true
            d.submitExpectFailure(CastSpell(me, id))
            d.state.getEntity(me)!!.get<CantCastSpellsComponent>()!!.restrictions.size shouldBe 2
        }
    }
    test("cleanup expires each ban independently in either application order") {
        for (permanentFirst in listOf(false, true)) {
            val d = setup(); val me = d.activePlayer!!
            if (permanentFirst) ban(d, GameObjectFilter.Creature, Duration.Permanent)
            ban(d, GameObjectFilter.Noncreature)
            if (!permanentFirst) ban(d, GameObjectFilter.Creature, Duration.Permanent)
            val cleanup = CleanupPhaseManager(d.cardRegistry, DecisionHandler(), conditionEvaluator = d.services.conditionEvaluator)
            d.replaceState(cleanup.cleanupEndOfTurn(d.state))
            val spell = d.putCardInHand(me, instant.name)
            val body = d.putCardInHand(me, creature.name)
            offers(d, body).isEmpty() shouldBe true
            d.submitExpectFailure(CastSpell(me, body))
            offers(d, spell).isNotEmpty() shouldBe true
            d.submitSuccess(CastSpell(me, spell))
        }
    }
    test("an end-of-your-next-turn ban outlives the opponent's turn and lifts after the controller's next one") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val result = CantCastSpellsExecutor(d.services.predicateEvaluator.amounts).execute(d.state,
            CantCastSpellsEffect(EffectTarget.PlayerRef(Player.Each), Duration.EndOfYourNextTurn,
                GameObjectFilter.Creature),
            EffectContext(sourceId = null, controllerId = me))
        d.replaceState(result.state)
        fun banned(p: EntityId) = d.state.getEntity(p)?.get<CantCastSpellsComponent>() != null

        // The opponent's turn: still banned for both players, keyed to the caster's turn.
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        banned(me) shouldBe true
        banned(opp) shouldBe true
        // The caster's next turn: still in force until its cleanup.
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, me)
        banned(me) shouldBe true
        val body = d.putCardInHand(me, creature.name)
        offers(d, body).isEmpty() shouldBe true
        // Past that cleanup, both bans are gone.
        d.passPriorityUntil(Step.UPKEEP, opp)
        banned(me) shouldBe false
        banned(opp) shouldBe false
    }
    test("an until-your-next-turn ban locks a dynamic mana value at resolution and lifts on the controller's next turn") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val reference = d.putCardInHand(me, "Grizzly Bears")
        val result = CantCastSpellsExecutor(d.services.predicateEvaluator.amounts).execute(d.state,
            CantCastSpellsEffect(EffectTarget.PlayerRef(Player.EachOpponent), Duration.UntilYourNextTurn,
                GameObjectFilter.Any.manaValueEqualsDynamic(
                    com.wingedsheep.sdk.dsl.DynamicAmounts.manaValueOf(EffectTarget.SpecificEntity(reference)))),
            EffectContext(sourceId = null, controllerId = me))
        d.replaceState(result.state)
        val ban = d.state.getEntity(opp)!!.get<CantCastSpellsComponent>()!!.restrictions.single()
        // Locked in as a literal: later casts read no resolution context.
        ban.spellFilter shouldBe GameObjectFilter.Any.manaValue(2)
        d.state.getEntity(me)?.get<CantCastSpellsComponent>() shouldBe null

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        d.state.getEntity(opp)?.get<CantCastSpellsComponent>() shouldNotBe null
        // Only mana value 2 is banned: their {0} instant is still castable.
        val free = d.putCardInHand(opp, instant.name)
        d.legalActions(opp).mapNotNull { it.action as? CastSpell }.any { it.cardId == free } shouldBe true
        // The controller's next turn lifts it right after untap.
        d.passPriorityUntil(Step.UPKEEP, me)
        d.state.getEntity(opp)?.get<CantCastSpellsComponent>() shouldBe null
    }
    test("a mana-value ban reads the chosen X: the offer stays up and only the banned X is rejected") {
        val d = setup(); val me = d.activePlayer!!
        ban(d, GameObjectFilter.Any.manaValue(2))
        repeat(2) { d.putLandOnBattlefield(me, "Plains") }
        val spell = d.putCardInHand(me, xInstant.name)
        offers(d, spell).isNotEmpty() shouldBe true
        d.submitExpectFailure(CastSpell(me, spell, xValue = 2))
        d.submitSuccess(CastSpell(me, spell, xValue = 1))
    }
})
