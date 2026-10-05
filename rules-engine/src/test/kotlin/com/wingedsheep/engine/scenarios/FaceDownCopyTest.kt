package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.copy.CopyExceptionApplier
import com.wingedsheep.engine.handlers.effects.permanent.types.EachPermanentBecomesCopyOfTargetExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class FaceDownCopyTest : FunSpec({
    val hidden = card("Hidden Copy Subject") {
        manaCost = "{4}{G}"
        typeLine = "Legendary Creature — Elf"
        power = 7; toughness = 8
        keywords(Keyword.FLYING)
        triggeredAbility { trigger = Triggers.self.enters(); effect = Effects.GainLife(9) }
        triggeredAbility { trigger = Triggers.you.beginningOf(Step.UPKEEP); effect = Effects.GainLife(9) }
        activatedAbility { cost = Costs.Tap; effect = Effects.GainLife(9) }
    }
    val copier = card("Anonymous Entry Copier") {
        manaCost = "{U}"; typeLine = "Creature — Shapeshifter"; power = 0; toughness = 0
        replacementEffect(EntersAsCopy())
    }
    val echo = card("Anonymous Token Maker") {
        manaCost = "{U}"; typeLine = "Sorcery"
        spell { val t = target(TargetFilter.Creature); effect = Effects.CreateTokenCopyOfTarget(t) }
    }
    val landCopier = card("Anonymous Land Copier") {
        typeLine = "Land"
        replacementEffect(EntersAsCopy())
    }
    val returnSpell = card("Anonymous Reanimation") {
        manaCost = "{U}"; typeLine = "Sorcery"
        spell { val t = target(TargetFilter.CreatureInYourGraveyard); effect = Effects.PutOntoBattlefield(t) }
    }
    val zap = card("Anonymous Ward Probe") {
        manaCost = "{U}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Creature); effect = Effects.DealDamage(1, t) }
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(hidden, copier, echo, zap, landCopier, returnSpell))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun faceDown(d: GameTestDriver, mode: FaceDownMode?): EntityId {
        val id = d.putPermanentOnBattlefield(d.player2, hidden.name)
        d.replaceState(d.state.updateEntity(id) {
            val c = it.with(FaceDownComponent)
            if (mode == null) c else c.with(FaceDownModeComponent(mode))
        })
        return id
    }
    fun card(d: GameTestDriver, id: EntityId) = d.state.getEntity(id)!!.get<CardComponent>()!!
    fun castCopy(d: GameTestDriver, source: EntityId): EntityId {
        val id = d.putCardInHand(d.player1, copier.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpell(d.player1, id).error shouldBe null
        d.bothPass().error shouldBe null
        d.submitCardSelection(d.player1, listOf(source)).error shouldBe null
        return id
    }
    val json = Json { serializersModule = com.wingedsheep.engine.core.engineSerializersModule; allowStructuredMapKeys = true }
    for (mode in listOf(null, FaceDownMode.MORPH, FaceDownMode.MANIFEST, FaceDownMode.DISGUISE, FaceDownMode.CLOAK)) {
        test("entry copies only the public values for $mode") {
            val d = driver()
            val source = faceDown(d, mode)
            val copy = castCopy(d, source)
            val c = card(d, copy)
            c.name shouldBe ""
            c.typeLine shouldBe TypeLine.parse("Creature")
            c.manaValue shouldBe 0
            c.colors shouldBe emptySet()
            c.baseStats!!.basePower shouldBe 2
            c.baseStats!!.baseToughness shouldBe 2
            c.hasActivatedAbility shouldBe false
            c.oracleText shouldBe ""
            c.copyTriggeredAbilities shouldBe emptyList()
            c.copyWardCosts shouldBe listOfNotNull(mode?.faceDownWard)
            d.state.getEntity(copy)!!.has<FaceDownComponent>() shouldBe false
            d.state.getEntity(copy)!!.has<MorphDataComponent>() shouldBe false
            d.state.projectedState.hasKeyword(copy, Keyword.FLYING) shouldBe false
            d.getLifeTotal(d.player1) shouldBe 20
            val transformer = com.wingedsheep.engine.view.ClientStateTransformer(
                cardRegistry = d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = d.cardRegistry))
            for (viewer in listOf(d.player1, d.player2)) {
                val view = transformer.transform(d.state, viewer).cards[copy]!!
                view.name shouldBe ""
                view.isFaceDown shouldBe false
                view.oracleText shouldBe ""
                view.backFaceName shouldBe null
            }
            d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
            card(d, copy).copyWardCosts shouldBe c.copyWardCosts
            d.replaceState(d.state.updateEntity(source) { it.without<FaceDownComponent>().without<FaceDownModeComponent>() })
            card(d, copy) shouldBe c
            d.replaceState(d.zones.moveToZone(d.state, copy, Zone.GRAVEYARD).state)
            card(d, copy).name shouldBe copier.name
            card(d, copy).copyWardCosts shouldBe emptyList()
        }
    }
    test("effect-driven entry copies public face-down characteristics") {
        val d = driver()
        val source = faceDown(d, FaceDownMode.CLOAK)
        val id = d.putCardInGraveyard(d.player1, copier.name)
        val spell = d.putCardInHand(d.player1, returnSpell.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpellWithTargets(d.player1, spell, listOf(ChosenTarget.Card(id, d.player1, Zone.GRAVEYARD))).error shouldBe null
        d.bothPass().error shouldBe null
        d.submitCardSelection(d.player1, listOf(source)).error shouldBe null
        card(d, id).name shouldBe ""
        card(d, id).copyWardCosts shouldBe listOf(WardCost.Mana("{2}"))
        d.state.getEntity(id)!!.has<FaceDownComponent>() shouldBe false
    }
    test("land play entry can copy an anonymous creature without hidden abilities") {
        val d = driver()
        val source = faceDown(d, FaceDownMode.MANIFEST)
        val id = d.putCardInHand(d.player1, landCopier.name)
        d.playLand(d.player1, id).error shouldBe null
        d.submitCardSelection(d.player1, listOf(source)).error shouldBe null
        card(d, id).name shouldBe ""
        card(d, id).typeLine shouldBe TypeLine.parse("Creature")
        card(d, id).copyWardCosts shouldBe emptyList()
    }
    test("face-down characteristics override the upright identity of a flipped permanent") {
        val d = driver()
        val source = faceDown(d, null)
        d.replaceState(d.state.updateEntity(source) { it.with(FlippedComponent(card(d, source))) })
        d.state.getEntity(source)!!.copiableCardComponent()!!.name shouldBe ""
        castCopy(d, source).let { card(d, it).name shouldBe "" }
    }
    test("token copy and copy of that token retain ward but no hidden DFC or numeric abilities") {
        val d = driver()
        val source = faceDown(d, FaceDownMode.CLOAK)
        d.replaceState(d.state.updateEntity(source) { it.with(ControllerComponent(d.player1)) })
        d.replaceState(d.state.updateEntity(source) { it.with(ToxicComponent(4))
            .with(DoubleFacedComponent(hidden.name, hidden.name)) })
        fun tokenOf(target: EntityId): EntityId {
            val before = d.state.getBattlefield().toSet()
            val id = d.putCardInHand(d.player1, echo.name)
            d.giveMana(d.player1, Color.BLUE, 1)
            d.castSpell(d.player1, id, targets = listOf(target)).error shouldBe null
            d.bothPass().error shouldBe null
            return (d.state.getBattlefield().toSet() - before).single()
        }
        val first = tokenOf(source)
        d.state.getEntity(first)!!.get<DoubleFacedComponent>()!!.faceChanges shouldBe 0
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        val flipped = com.wingedsheep.engine.handlers.effects.permanent.types.flipDfcInPlace(d.state, d.cardRegistry, first)!!
        d.replaceState(flipped.first)
        card(d, first).name shouldBe ""
        card(d, first).copyWardCosts shouldBe listOf(WardCost.Mana("{2}"))
        val second = tokenOf(first)
        d.state.getEntity(first)!!.get<DoubleFacedComponent>()!!.faceChanges shouldBe 1
        d.state.getEntity(second)!!.get<DoubleFacedComponent>()!!.faceChanges shouldBe 0
        for (id in listOf(first, second)) {
            val c = d.state.getEntity(id)!!
            c.has<FaceDownComponent>() shouldBe false
            c.has<DoubleFacedComponent>() shouldBe true
            c.get<DoubleFacedComponent>()!!.frontCardDefinitionId shouldBe "face-down"
            c.get<DoubleFacedComponent>()!!.backCardDefinitionId shouldBe "face-down"
            c.has<ToxicComponent>() shouldBe false
            card(d, id).copyWardCosts shouldBe listOf(WardCost.Mana("{2}"))
            card(d, id).name shouldBe ""
        }
    }
    test("face-up copy of disguise actually counters an unpaid opposing spell") {
        val d = driver()
        val copy = castCopy(d, faceDown(d, FaceDownMode.DISGUISE))
        repeat(2) { d.putLandOnBattlefield(d.player2, "Island") }
        val spell = d.putCardInHand(d.player2, zap.name)
        d.giveMana(d.player2, Color.BLUE, 1)
        d.passPriority(d.player1)
        d.castSpell(d.player2, spell, targets = listOf(copy)).error shouldBe null
        d.bothPass().error shouldBe null
        d.submitManaAutoPayOrDecline(d.player2, autoPay = false).error shouldBe null
        d.state.stack shouldBe emptyList()
        d.state.projectedState.getToughness(copy) shouldBe 2
    }
    test("recopy loses a previous copied ward cost when the new source has none") {
        val d = driver()
        val copy = castCopy(d, faceDown(d, FaceDownMode.CLOAK))
        val source = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val result = EachPermanentBecomesCopyOfTargetExecutor(PredicateEvaluator(cardRegistry = d.cardRegistry), d.cardRegistry)
            .execute(d.state, EachPermanentBecomesCopyOfTargetEffect(target = EffectTarget.ContextTarget(0), affected = EffectTarget.Self),
                EffectContext(sourceId = copy, controllerId = d.player1, targets = listOf(ChosenTarget.Permanent(source))))
        d.replaceState(result.state)
        card(d, copy).copyWardCosts shouldBe emptyList()
        card(d, copy).name shouldBe "Grizzly Bears"
    }
    for (path in listOf("source", "equipped", "chosen", "attached replacement")) {
        test("$path token copying freezes public values on both faces") {
            val d = driver()
            val source = faceDown(d, FaceDownMode.CLOAK)
            d.replaceState(d.state.updateEntity(source) {
                it.with(DoubleFacedComponent(hidden.name, hidden.name)).with(ToxicComponent(4))
            })
            val evaluator = PredicateEvaluator(cardRegistry = d.cardRegistry)
            val context = EffectContext(sourceId = source, controllerId = d.player1)
            val before = d.state.getBattlefield().toSet()
            val result = when (path) {
                "source" -> com.wingedsheep.engine.handlers.effects.token.CreateTokenCopyOfSourceExecutor(
                    d.cardRegistry, predicateEvaluator = evaluator).execute(d.state, CreateTokenCopyOfSourceEffect(), context)
                "chosen" -> com.wingedsheep.engine.handlers.effects.token.CreateTokenCopyOfChosenPermanentExecutor
                    .createTokenCopy(d.state, source, d.player1, cardRegistry = d.cardRegistry, predicateEvaluator = evaluator)
                "equipped" -> {
                    val equipment = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
                    d.replaceState(d.state.updateEntity(equipment) {
                        it.with(com.wingedsheep.engine.state.components.battlefield.AttachedToComponent(source))
                    })
                    com.wingedsheep.engine.handlers.effects.token.CreateTokenCopyOfEquippedCreatureExecutor(
                        d.cardRegistry, predicateEvaluator = evaluator).execute(d.state,
                            CreateTokenCopyOfEquippedCreatureEffect(grantHaste = true), context.copy(sourceId = equipment))
                }
                else -> com.wingedsheep.engine.handlers.effects.token.TokenCreationReplacementHelper
                    .createAttachedPermanentCopies(d.state, source, d.player1, 1,
                        cardRegistry = d.cardRegistry, predicateEvaluator = evaluator)
            }
            d.replaceState(result.state)
            val token = (d.state.getBattlefield().toSet() - before).single { d.state.getEntity(it)!!.has<TokenComponent>() }
            card(d, token).name shouldBe ""
            card(d, token).copyWardCosts shouldBe listOf(WardCost.Mana("{2}"))
            d.state.getEntity(token)!!.has<ToxicComponent>() shouldBe false
            val transformed = com.wingedsheep.engine.handlers.effects.permanent.types.flipDfcInPlace(d.state, d.cardRegistry, token)!!
            d.replaceState(transformed.first)
            card(d, token).name shouldBe ""
            card(d, token).copyWardCosts shouldBe listOf(WardCost.Mana("{2}"))
            d.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe (path == "equipped")
            val transformer = com.wingedsheep.engine.view.ClientStateTransformer(
                cardRegistry = d.cardRegistry, predicateEvaluator = evaluator)
            for (viewer in listOf(d.player1, d.player2)) {
                val view = transformer.transform(d.state, viewer).cards[token]!!
                view.name shouldBe ""
                view.oracleText shouldBe ""
                view.backFaceName shouldBe null
                view.isFaceDown shouldBe false
            }
        }
    }
    test("numeric copy exceptions still apply when the source is face down") {
        val d = driver()
        val id = faceDown(d, null)
        val source = d.state.getEntity(id)!!
        val result = CopyExceptionApplier.withNumericKeywords(source, source,
            CopyExceptions(addedNumericKeywords = listOf(com.wingedsheep.sdk.scripting.KeywordAbility.Numeric(Keyword.TOXIC, 1))))
        result.get<ToxicComponent>()!!.amount shouldBe 1
    }
    for (optional in listOf(false, true)) {
        test("attached-copy replacement dispatch keeps hidden identity private when optional=$optional") {
            val d = driver()
            val source = faceDown(d, FaceDownMode.DISGUISE)
            d.replaceState(d.state.updateEntity(source) {
                it.with(DoubleFacedComponent(hidden.name, hidden.name)).with(ToxicComponent(4))
            })
            val equipment = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
            d.replaceState(d.state.updateEntity(equipment) {
                it.with(com.wingedsheep.engine.state.components.battlefield.AttachedToComponent(source))
                    .with(com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent(
                        listOf(com.wingedsheep.sdk.scripting.ReplaceTokenCreationWithAttachedCopy(optional = optional))))
            })
            val evaluator = PredicateEvaluator(cardRegistry = d.cardRegistry)
            val before = d.state.getBattlefield().toSet()
            val result = com.wingedsheep.engine.handlers.effects.token.TokenCreationReplacementHelper.checkReplacement(
                d.state, CreateTokenCopyOfSourceEffect(), EffectContext(sourceId = equipment, controllerId = d.player1),
                1, d.player1, cardRegistry = d.cardRegistry, predicateEvaluator = evaluator)!!
            d.replaceState(result.state)
            if (optional) {
                d.state.pendingDecision!!.prompt.contains(hidden.name) shouldBe false
                d.state.pendingDecision!!.prompt.contains("face-down creature") shouldBe true
                d.submitYesNo(d.player1, true).error shouldBe null
            }
            val token = (d.state.getBattlefield().toSet() - before).single()
            card(d, token).name shouldBe ""
            card(d, token).copyWardCosts shouldBe listOf(WardCost.Mana("{2}"))
            d.state.getEntity(token)!!.has<ToxicComponent>() shouldBe false
            val transformed = com.wingedsheep.engine.handlers.effects.permanent.types.flipDfcInPlace(d.state, d.cardRegistry, token)!!
            d.replaceState(transformed.first)
            card(d, token).name shouldBe ""
            card(d, token).copyWardCosts shouldBe listOf(WardCost.Mana("{2}"))
            val transformer = com.wingedsheep.engine.view.ClientStateTransformer(
                cardRegistry = d.cardRegistry, predicateEvaluator = evaluator)
            for (viewer in listOf(d.player1, d.player2)) {
                val view = transformer.transform(d.state, viewer).cards[token]!!
                view.name shouldBe ""
                view.oracleText shouldBe ""
                view.backFaceName shouldBe null
            }
        }
    }
})
