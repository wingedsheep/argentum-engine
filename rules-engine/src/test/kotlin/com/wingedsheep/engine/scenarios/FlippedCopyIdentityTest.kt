package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.permanent.types.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.effects.EachPermanentBecomesCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class FlippedCopyIdentityTest : FunSpec({
    fun flipCard(front: String, back: String, n: Int) = CardDefinition.flipCard(
        card(front) {
            manaCost = "{1}{G}"; typeLine = "Creature — Human"; power = 1; toughness = 2
            activatedAbility { cost = Costs.Free; effect = Effects.Flip() }
        },
        card(back) {
            typeLine = "Legendary Creature — Wizard"; power = n; toughness = n
            keywords(Keyword.FLYING)
            metadata { collectorNumber = "42" }
            if (n == 4) staticAbility {
                ability = ModifyStats(1, 1, GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true))
            }
            activatedAbility { cost = Costs.Free; effect = Effects.GainLife(n) }
        },
    )
    val first = flipCard("Copy Student", "Copy Teacher", 3)
    val second = flipCard("Copy Pupil", "Copy Professor", 4)
    val copier = card("Blue Flip Copier") {
        manaCost = "{U}"; typeLine = "Creature — Shapeshifter"; power = 2; toughness = 2
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(first, second, copier))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun card(d: GameTestDriver, id: EntityId) = d.state.getEntity(id)!!.get<CardComponent>()!!
    fun copy(d: GameTestDriver, who: EntityId, source: EntityId,
             duration: Duration = Duration.Permanent, exceptions: CopyExceptions = CopyExceptions.None) {
        val result = EachPermanentBecomesCopyOfTargetExecutor(PredicateEvaluator(cardRegistry = d.cardRegistry), d.cardRegistry)
            .execute(d.state, EachPermanentBecomesCopyOfTargetEffect(
                target = EffectTarget.ContextTarget(0), affected = EffectTarget.Self,
                duration = duration, exceptions = exceptions),
                EffectContext(sourceId = who, controllerId = d.player1, targets = listOf(ChosenTarget.Permanent(source))))
        result.error shouldBe null
        result.events.filterIsInstance<CopiableCharacteristicsChangedEvent>().size shouldBe 1
        d.replaceState(result.state)
    }
    fun flip(d: GameTestDriver, id: EntityId) {
        val result = FlipEffectExecutor(d.cardRegistry).execute(d.state, com.wingedsheep.sdk.scripting.effects.FlipEffect(),
            EffectContext(sourceId = id, controllerId = d.player1))
        d.replaceState(result.state)
    }
    fun roundTrip(d: GameTestDriver) {
        val codec = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
        d.replaceState(codec.decodeFromString<GameState>(codec.encodeToString(d.state)))
    }
    test("unflipped copies of either source status acquire both halves and can flip independently") {
        for (sourceFlipped in listOf(false, true)) {
            val d = driver()
            val source = d.putPermanentOnBattlefield(d.player2, first.name)
            if (sourceFlipped) flip(d, source)
            val who = d.putPermanentOnBattlefield(d.player1, copier.name)
            copy(d, who, source, exceptions = CopyExceptions(retainColors = true))
            card(d, who).name shouldBe first.name
            card(d, who).flipSide!!.name shouldBe first.flipSide!!.name
            roundTrip(d)
            flip(d, who)
            card(d, who).name shouldBe first.flipSide!!.name
            card(d, who).colors shouldBe setOf(Color.BLUE)
            card(d, who).manaCost shouldBe first.manaCost
            card(d, who).ownerId shouldBe d.player1
            d.state.projectedState.getPower(who) shouldBe 3
        }
    }
    test("flipped status survives ordinary then alternative recopy and both snapshots stay current") {
        for (sourceFlipped in listOf(false, true)) {
            val d = driver()
            val who = d.putPermanentOnBattlefield(d.player1, first.name)
            flip(d, who)
            val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
            copy(d, who, bear)
            card(d, who).name shouldBe "Grizzly Bears"
            d.state.getEntity(who)!!.copiableCardComponent()!!.name shouldBe "Grizzly Bears"
            val source = d.putPermanentOnBattlefield(d.player2, second.name)
            if (sourceFlipped) flip(d, source)
            roundTrip(d)
            copy(d, who, source)
            card(d, who).name shouldBe second.flipSide!!.name
            d.state.projectedState.getPower(who) shouldBe 4
            d.state.getEntity(who)!!.copiableCardComponent()!!.name shouldBe second.name
            d.state.getEntity(who)!!.has<FlippedComponent>() shouldBe true
            val fresh = d.putPermanentOnBattlefield(d.player1, copier.name)
            copy(d, fresh, who)
            card(d, fresh).name shouldBe second.name
            flip(d, fresh)
            card(d, fresh).name shouldBe second.flipSide!!.name
        }
    }
    test("a flipped copier of a face-down source has only public values and forgets hidden flip halves") {
        val d = driver()
        val who = d.putPermanentOnBattlefield(d.player1, first.name)
        flip(d, who)
        val source = d.putPermanentOnBattlefield(d.player2, second.name)
        d.replaceState(d.state.updateEntity(source) { it.with(FaceDownComponent) })
        copy(d, who, source)
        card(d, who).name shouldBe ""
        card(d, who).flipSide shouldBe null
        d.state.getEntity(who)!!.copiableCardComponent()!!.name shouldBe ""
        d.state.projectedState.getPower(who) shouldBe 2
        copy(d, who, source, exceptions = CopyExceptions(addedNumericKeywords = listOf(KeywordAbility.Numeric(Keyword.TOXIC, 1))))
        d.state.getEntity(who)!!.get<ToxicComponent>()!!.amount shouldBe 1
    }
    test("copy exceptions apply to both halves and survive copies of copies") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player2, first.name)
        val who = d.putPermanentOnBattlefield(d.player1, copier.name)
        val ability = card("Flip Copy Trigger Scope") {
            triggeredAbility { trigger = Triggers.you.beginningOf(Step.UPKEEP); effect = Effects.GainLife(1) }
        }.triggeredAbilities.single()
        copy(d, who, source, exceptions = CopyExceptions(retainColors = true,
            addedTriggeredAbilities = listOf(ability), powerOverride = 5, toughnessOverride = 6))
        flip(d, who)
        card(d, who).copyTriggeredAbilities.size shouldBe 1
        card(d, who).baseStats!!.basePower shouldBe 5
        card(d, who).baseStats!!.baseToughness shouldBe 6
        val next = d.putPermanentOnBattlefield(d.player1, copier.name)
        copy(d, next, who)
        flip(d, next)
        card(d, next).colors shouldBe setOf(Color.BLUE)
        card(d, next).copyTriggeredAbilities.size shouldBe 1
        card(d, next).baseStats!!.basePower shouldBe 5
    }
    test("a copied flip half exposes its own activated ability") {
        val d = driver()
        val who = d.putPermanentOnBattlefield(d.player1, first.name)
        flip(d, who)
        val source = d.putPermanentOnBattlefield(d.player2, second.name)
        copy(d, who, source)
        d.submit(ActivateAbility(d.player1, who, second.flipSide!!.activatedAbilities.single().id)).error shouldBe null
        d.bothPass().error shouldBe null
        d.getLifeTotal(d.player1) shouldBe 24
    }
    test("cleanup restores a flipped original and retains flipped status") {
        val d = driver()
        val who = d.putPermanentOnBattlefield(d.player1, first.name)
        flip(d, who)
        val source = d.putPermanentOnBattlefield(d.player2, second.name)
        copy(d, who, source, Duration.EndOfTurn)
        roundTrip(d)
        d.passPriorityUntil(Step.UPKEEP)
        card(d, who).name shouldBe first.flipSide!!.name
        d.state.getEntity(who)!!.copiableCardComponent()!!.name shouldBe first.name
        d.state.getEntity(who)!!.has<FlippedComponent>() shouldBe true
    }
    test("temporary copy expiry restores a permanent copy instead of the printed copier") {
        val d = driver()
        val who = d.putPermanentOnBattlefield(d.player1, copier.name)
        val source = d.putPermanentOnBattlefield(d.player2, first.name)
        val other = d.putPermanentOnBattlefield(d.player2, second.name)
        copy(d, who, source)
        flip(d, who)
        copy(d, who, other, Duration.EndOfTurn)
        d.passPriorityUntil(Step.UPKEEP)
        card(d, who).name shouldBe first.flipSide!!.name
        d.state.getEntity(who)!!.get<CopyOfComponent>()!!.originalCardComponent!!.name shouldBe copier.name
    }
    test("flipping during a temporary copy retains that status after expiry") {
        val d = driver()
        val who = d.putPermanentOnBattlefield(d.player1, first.name)
        val source = d.putPermanentOnBattlefield(d.player2, second.name)
        copy(d, who, source, Duration.EndOfTurn)
        flip(d, who)
        d.passPriorityUntil(Step.UPKEEP)
        card(d, who).name shouldBe first.flipSide!!.name
    }
    test("an expired lower layer never erases a later permanent recopy") {
        val d = driver()
        val who = d.putPermanentOnBattlefield(d.player1, first.name)
        flip(d, who)
        val source = d.putPermanentOnBattlefield(d.player2, second.name)
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        copy(d, who, source, Duration.EndOfTurn)
        copy(d, who, bear)
        d.passPriorityUntil(Step.UPKEEP)
        card(d, who).name shouldBe "Grizzly Bears"
        d.state.getEntity(who)!!.copiableCardComponent()!!.name shouldBe "Grizzly Bears"
    }
    test("leaving the battlefield restores the printed card rather than the last copied upright snapshot") {
        val d = driver()
        val who = d.putPermanentOnBattlefield(d.player1, copier.name)
        val source = d.putPermanentOnBattlefield(d.player2, first.name)
        copy(d, who, source)
        flip(d, who)
        val moved = com.wingedsheep.engine.handlers.effects.ZoneTransitionService(d.cardRegistry,
            PredicateEvaluator(cardRegistry = d.cardRegistry)).moveToZone(d.state, who, Zone.GRAVEYARD)
        d.replaceState(moved.state)
        card(d, who).name shouldBe copier.name
        card(d, who).flipSide shouldBe null
        d.state.getEntity(who)!!.has<FlippedComponent>() shouldBe false
    }
    test("client projection shows copied alternative name and keeps rotation") {
        val d = driver()
        val who = d.putPermanentOnBattlefield(d.player1, first.name)
        flip(d, who)
        val source = d.putPermanentOnBattlefield(d.player2, second.name)
        copy(d, who, source)
        val view = ClientStateTransformer(d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = d.cardRegistry))
            .transform(d.state, d.player1).cards.getValue(who)
        view.name shouldBe second.flipSide!!.name
        view.imageRotation shouldBe 180
        view.power shouldBe 4
    }

    test("a lower next-end-step copy expires without erasing a later end-of-turn layer") {
        val d = driver()
        val who = d.putPermanentOnBattlefield(d.player1, first.name)
        flip(d, who)
        val source = d.putPermanentOnBattlefield(d.player2, second.name)
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        copy(d, who, source, Duration.UntilNextEndStep)
        copy(d, who, bear, Duration.EndOfTurn)
        roundTrip(d)
        d.passPriorityUntil(Step.END)
        card(d, who).name shouldBe "Grizzly Bears"
        d.passPriorityUntil(Step.UPKEEP)
        card(d, who).name shouldBe first.flipSide!!.name
    }
    test("next-turn expiry restores the flipped identity at its controller's next turn") {
        val d = driver()
        val who = d.putPermanentOnBattlefield(d.player1, first.name)
        flip(d, who)
        val source = d.putPermanentOnBattlefield(d.player2, second.name)
        copy(d, who, source, Duration.UntilYourNextTurn)
        d.passPriorityUntil(Step.UPKEEP)
        card(d, who).name shouldBe second.flipSide!!.name
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.UPKEEP)
        card(d, who).name shouldBe first.flipSide!!.name
    }
    test("recopy and expiry refresh the selected half's static abilities") {
        val d = driver()
        val who = d.putPermanentOnBattlefield(d.player1, first.name)
        flip(d, who)
        val buddy = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val source = d.putPermanentOnBattlefield(d.player2, second.name)
        d.state.projectedState.getPower(buddy) shouldBe 2
        copy(d, who, source, Duration.EndOfTurn)
        d.state.projectedState.getPower(buddy) shouldBe 3
        d.passPriorityUntil(Step.UPKEEP)
        d.state.projectedState.getPower(buddy) shouldBe 2
        d.events.filterIsInstance<CopiableCharacteristicsChangedEvent>().any { it.entityId == who } shouldBe true
    }
    test("attachment expiry restores flipped status and clears the copied identity") {
        val d = driver()
        val equipmentDef = card("Flip Copy Equipment") { typeLine = "Artifact — Equipment" }
        d.registerCard(equipmentDef)
        val equipment = d.putPermanentOnBattlefield(d.player1, equipmentDef.name)
        val who = d.putPermanentOnBattlefield(d.player1, first.name)
        flip(d, who)
        val source = d.putPermanentOnBattlefield(d.player2, second.name)
        d.replaceState(d.state.updateEntity(equipment) { it.with(AttachedToComponent(who)) })
        val result = EachPermanentBecomesCopyOfTargetExecutor(PredicateEvaluator(cardRegistry = d.cardRegistry), d.cardRegistry)
            .execute(d.state, EachPermanentBecomesCopyOfTargetEffect(
                target = EffectTarget.ContextTarget(0), affected = EffectTarget.ContextTarget(1),
                duration = Duration.WhileSourceAttachedToAffected),
                EffectContext(sourceId = equipment, controllerId = d.player1,
                    targets = listOf(ChosenTarget.Permanent(source), ChosenTarget.Permanent(who))))
        d.replaceState(result.state)
        card(d, who).name shouldBe second.flipSide!!.name
        d.replaceState(d.state.updateEntity(equipment) { it.without<AttachedToComponent>() })
        val expired = com.wingedsheep.engine.mechanics.sba.permanent.AttachedCopyExpiryCheck(d.cardRegistry).check(d.state)
        d.replaceState(expired.newState)
        card(d, who).name shouldBe first.flipSide!!.name
        expired.events.filterIsInstance<CopiableCharacteristicsChangedEvent>().size shouldBe 1
        d.state.getEntity(who)!!.has<CopyWhileAttachedComponent>() shouldBe false
    }

    test("a copied intrinsic morph marker ends on departure and the printed one returns after a temporary copy") {
        val d = driver()
        val morphCard = card("Flip Copy Morph") {
            typeLine = "Creature — Beast"; power = 2; toughness = 2; morph = "{G}"
        }
        d.registerCard(morphCard)
        val source = d.putPermanentOnBattlefield(d.player2, morphCard.name)
        val who = d.putPermanentOnBattlefield(d.player1, copier.name)
        copy(d, who, source)
        d.state.getEntity(who)!!.has<HasMorphAbilityComponent>() shouldBe true
        val moved = com.wingedsheep.engine.handlers.effects.ZoneTransitionService(d.cardRegistry,
            PredicateEvaluator(cardRegistry = d.cardRegistry)).moveToZone(d.state, who, Zone.GRAVEYARD)
        d.replaceState(moved.state)
        d.state.getEntity(who)!!.has<HasMorphAbilityComponent>() shouldBe false
        val original = d.putPermanentOnBattlefield(d.player1, morphCard.name)
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        copy(d, original, bear, Duration.EndOfTurn)
        d.state.getEntity(original)!!.has<HasMorphAbilityComponent>() shouldBe false
        d.passPriorityUntil(Step.UPKEEP)
        d.state.getEntity(original)!!.has<HasMorphAbilityComponent>() shouldBe true
    }

    test("numeric copy exceptions survive flipping and temporary identity changes") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player2, first.name)
        val who = d.putPermanentOnBattlefield(d.player1, copier.name)
        copy(d, who, source, exceptions = CopyExceptions(addedNumericKeywords = listOf(KeywordAbility.Numeric(Keyword.TOXIC, 1))))
        d.state.getEntity(who)!!.get<ToxicComponent>()!!.amount shouldBe 1
        flip(d, who)
        d.state.getEntity(who)!!.get<ToxicComponent>()!!.amount shouldBe 1
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        copy(d, who, bear, Duration.EndOfTurn)
        d.state.getEntity(who)!!.has<ToxicComponent>() shouldBe false
        roundTrip(d)
        d.passPriorityUntil(Step.UPKEEP)
        card(d, who).name shouldBe first.flipSide!!.name
        d.state.getEntity(who)!!.get<ToxicComponent>()!!.amount shouldBe 1
    }
})
