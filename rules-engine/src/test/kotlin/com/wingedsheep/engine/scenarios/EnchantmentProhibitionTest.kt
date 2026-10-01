package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.GameState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.handlers.effects.permanent.attachments.AttachmentMover
import com.wingedsheep.engine.handlers.predicates.EnchantRestriction
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.*
import io.kotest.matchers.shouldBe

class EnchantmentProhibitionTest : ScenarioTestBase() {
    init {
        val shield = card("Red Aura Shield") {
            manaCost = "{W}"; typeLine = "Enchantment — Aura"
            auraTarget = TargetObject(filter = TargetFilter.Creature)
            staticAbility { ability = PreventEnchantment(
                auras = GameObjectFilter.Enchantment.withSubtype("Aura").withColor(Color.RED),
                exceptSource = true, filter = GroupFilter.attachedCreature()) }
        }
        val red = card("Red Test Aura") {
            manaCost = "{R}"; typeLine = "Enchantment — Aura"
            auraTarget = TargetObject(filter = TargetFilter.Creature)
        }
        val white = card("White Test Aura") {
            manaCost = "{W}"; typeLine = "Enchantment — Aura"
            auraTarget = TargetObject(filter = TargetFilter.Creature)
        }
        val mover = card("Move Test Aura") {
            manaCost = "{W}"; typeLine = "Sorcery"
            spell { val t = target(TargetFilter.Permanent); effect = Effects.AttachToChosenHost(t) }
        }
        val returner = card("Return Test Aura") {
            manaCost = "{W}"; typeLine = "Sorcery"
            spell { val t = target(TargetFilter.PermanentInYourGraveyard); effect = Effects.PutOntoBattlefield(t) }
        }
        val mute = card("Mute Test Aura") {
            manaCost = "{W}"; typeLine = "Instant"
            spell { val t = target(TargetFilter.Permanent); effect = Effects.RemoveAllAbilities(t) }
        }
        listOf(shield, red, white, mover, returner, mute).forEach(cardRegistry::register)
        fun board() = scenario().withPlayers("Player1", "Player2").withCardOnBattlefield(1, "Grizzly Bears")
            .withCardAttachedTo(1, shield.name, "Grizzly Bears")
            .withCardOnBattlefield(1, "Hill Giant")
            .withCardInHand(1, red.name).withCardInHand(1, white.name)
            .withLandsOnBattlefield(1, "Mountain", 1).withLandsOnBattlefield(1, "Plains", 3)
            .withActivePlayer(1)
        test("restriction filters Auras using their actual colors and legal-target enumeration") {
            val g = board().build()
            val host = g.findPermanent("Grizzly Bears")!!
            val redId = g.state.getHand(g.player1Id).first { g.state.getEntity(it)?.get<CardComponent>()?.name == red.name }
            val whiteId = g.state.getHand(g.player1Id).first { g.state.getEntity(it)?.get<CardComponent>()?.name == white.name }
            val finder = TargetFinder(services.predicateEvaluator)
            (host in finder.findLegalTargets(g.state, red.script.auraTarget!!, g.player1Id, redId)) shouldBe false
            (host in finder.findLegalTargets(g.state, white.script.auraTarget!!, g.player1Id, whiteId)) shouldBe true
            g.castSpell(1, white.name, host).error shouldBe null
            g.resolveStack()
            g.isOnBattlefield(white.name) shouldBe true
        }
        test("source-bound restrictions survive a persisted game state round trip") {
            val g = board().build()
            val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
            g.state = json.decodeFromString<GameState>(json.encodeToString(g.state))
            val host = g.findPermanent("Grizzly Bears")!!
            val id = g.state.getHand(g.player1Id).first { g.state.getEntity(it)?.get<CardComponent>()?.name == red.name }
            EnchantRestriction.hostAllowsAura(g.state, g.state.projectedState, services.predicateEvaluator, id, host) shouldBe false
        }
        test("abilities of an Aura outside the battlefield can target a prohibited host") {
            val g = board().withCardInGraveyard(1, red.name).build()
            val host = g.findPermanent("Grizzly Bears")!!
            val aura = g.state.getZone(g.player1Id, Zone.GRAVEYARD).first {
                g.state.getEntity(it)?.get<CardComponent>()?.name == red.name
            }
            val requirement = TargetObject(filter = TargetFilter.Creature)
            val finder = TargetFinder(services.predicateEvaluator)
            val validator = TargetValidator(services.predicateEvaluator)
            for (sourceType in listOf(TargetingSourceType.ACTIVATED_ABILITY, TargetingSourceType.TRIGGERED_ABILITY)) {
                (host in finder.findLegalTargets(g.state, requirement, g.player1Id, aura,
                    targetingSourceType = sourceType)) shouldBe true
                validator.validateTargets(g.state, listOf(ChosenTarget.Permanent(host)), listOf(requirement),
                    g.player1Id, sourceId = aura, targetingSourceType = sourceType) shouldBe null
            }
            (validator.validateTargets(g.state, listOf(ChosenTarget.Permanent(host)), listOf(requirement),
                g.player1Id, sourceId = aura, targetingSourceType = TargetingSourceType.SPELL) != null) shouldBe true
        }
        test("non-targeted Aura entry cannot choose a prohibited host") {
            val g = board().withCardInGraveyard(1, red.name).withCardInHand(1, returner.name).build()
            val forbidden = g.findPermanent("Grizzly Bears")!!
            val allowed = g.findPermanent("Hill Giant")!!
            g.castSpellTargetingGraveyardCard(1, returner.name, 1, red.name).error shouldBe null
            g.resolveStack()
            val d = g.state.pendingDecision as ChooseTargetsDecision
            (forbidden in d.legalTargets.getValue(0)) shouldBe false
            (allowed in d.legalTargets.getValue(0)) shouldBe true
            g.submitDecision(TargetsResponse(d.id, mapOf(0 to listOf(allowed)))).error shouldBe null
            g.resolveStack()
            g.isOnBattlefield(red.name) shouldBe true
        }
        test("non-targeted reattachment leaves the Aura on its original host") {
            val g = board().withCardAttachedTo(1, red.name, "Hill Giant")
                .withCardInHand(1, mover.name).build()
            val aura = g.findPermanent(red.name)!!
            val host = g.findPermanent("Grizzly Bears")!!
            AttachmentMover.canAttach(g.state, services.predicateEvaluator, cardRegistry, aura, host) shouldBe false
            g.castSpell(1, mover.name, aura).error shouldBe null
            g.resolveStack()
            g.state.pendingDecision shouldBe null
            g.state.getEntity(aura)!!.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()!!.targetId shouldBe g.findPermanent("Hill Giant")
        }
        test("removing the source ability removes its prohibition") {
            val g = board().withCardInHand(1, mute.name).build()
            val host = g.findPermanent("Grizzly Bears")!!
            g.castSpell(1, mute.name, g.findPermanent(shield.name)!!).error shouldBe null
            g.resolveStack()
            g.castSpell(1, red.name, host).error shouldBe null
            g.resolveStack()
            g.isOnBattlefield(red.name) shouldBe true
        }
        test("removing the host's abilities does not remove another source's rule restriction") {
            val g = board().withCardInHand(1, mute.name).build()
            val host = g.findPermanent("Grizzly Bears")!!
            g.castSpell(1, mute.name, host).error shouldBe null
            g.resolveStack()
            val id = g.state.getHand(g.player1Id).first { g.state.getEntity(it)?.get<CardComponent>()?.name == red.name }
            EnchantRestriction.hostAllowsAura(g.state, g.state.projectedState, services.predicateEvaluator, id, host) shouldBe false
        }
        test("a composite prohibition begun in an earlier layer survives source ability removal") {
            val composite = card("Composite Aura Shield") {
                manaCost = "{W}"; typeLine = "Enchantment — Aura"
                auraTarget = TargetObject(filter = TargetFilter.Creature)
                staticAbility { ability = CompositeStaticAbility(listOf(
                    GrantColor(Color.BLUE, GroupFilter.attachedCreature()),
                    PreventEnchantment(exceptSource = true, filter = GroupFilter.attachedCreature())
                )) }
            }
            cardRegistry.register(composite)
            val g = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, composite.name, "Grizzly Bears")
                .withCardInHand(1, red.name).withCardInHand(1, mute.name)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Mountain", 1).withActivePlayer(1).build()
            val source = g.findPermanent(composite.name)!!
            val host = g.findPermanent("Grizzly Bears")!!
            g.castSpell(1, mute.name, source).error shouldBe null
            g.resolveStack()
            g.state.projectedState.hasLostAllAbilities(source) shouldBe true
            g.state.projectedState.hasColor(host, Color.BLUE) shouldBe true
            val aura = g.state.getHand(g.player1Id).first {
                g.state.getEntity(it)?.get<CardComponent>()?.name == red.name
            }
            EnchantRestriction.hostAllowsAura(g.state, g.state.projectedState,
                services.predicateEvaluator, aura, host) shouldBe false
            (g.castSpell(1, red.name, host).error != null) shouldBe true
        }
        test("battlefield Aura filters see projected colors rather than printed colors") {
            val painter = card("Paint Test Aura") {
                manaCost = "{W}"; typeLine = "Instant"
                spell { val t = target(TargetFilter.Permanent); effect = Effects.ChangeColor(t, setOf(Color.RED)) }
            }
            cardRegistry.register(painter)
            val g = board().withCardAttachedTo(1, white.name, "Grizzly Bears")
                .withCardInHand(1, painter.name).build()
            g.castSpell(1, painter.name, g.findPermanent(white.name)!!).error shouldBe null
            g.resolveStack()
            g.isInGraveyard(1, white.name) shouldBe true
            g.isOnBattlefield(shield.name) shouldBe true
        }
        test("token-copy entry uses the new object's colors and cannot inherit a source exception") {
            val copier = card("Copy Test Shield") {
                manaCost = "{W}"; typeLine = "Sorcery"
                spell { val t = target(TargetFilter.Permanent)
                    effect = Effects.CreateTokenCopyOfTarget(t, overrideColors = setOf(Color.RED)) }
            }
            cardRegistry.register(copier)
            val g = board().withCardInHand(1, copier.name).build()
            val original = g.findPermanent(shield.name)!!
            g.castSpell(1, copier.name, original).error shouldBe null
            g.resolveStack()
            val d = g.state.pendingDecision as ChooseTargetsDecision
            (g.findPermanent("Grizzly Bears")!! in d.legalTargets.getValue(0)) shouldBe false
            val host = g.findPermanent("Hill Giant")!!
            g.submitDecision(TargetsResponse(d.id, mapOf(0 to listOf(host)))).error shouldBe null
            g.resolveStack()
            g.state.getBattlefield().count { g.state.getEntity(it)?.get<CardComponent>()?.name == shield.name } shouldBe 2
        }
        test("phasing out the source suspends its prohibition without removing it") {
            val phaser = card("Phase Test Shield") {
                manaCost = "{W}"; typeLine = "Instant"
                spell { val t = target(TargetFilter.Permanent); effect = Effects.PhaseOut(t) }
            }
            cardRegistry.register(phaser)
            val g = board().withCardInHand(1, phaser.name).build()
            g.castSpell(1, phaser.name, g.findPermanent(shield.name)!!).error shouldBe null
            g.resolveStack()
            g.castSpell(1, red.name, g.findPermanent("Grizzly Bears")!!).error shouldBe null
            g.resolveStack()
            g.isOnBattlefield(red.name) shouldBe true
        }
        test("multiple sources do not exempt each other's attachments") {
            val all = card("All Aura Shield") {
                manaCost = "{W}"; typeLine = "Enchantment — Aura"
                auraTarget = TargetObject(filter = TargetFilter.Creature)
                staticAbility { ability = PreventEnchantment(exceptSource = true, filter = GroupFilter.attachedCreature()) }
            }
            cardRegistry.register(all)
            val g = scenario().withPlayers("Player1", "Player2").withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, shield.name, "Grizzly Bears")
                .withCardAttachedTo(1, all.name, "Grizzly Bears").withActivePlayer(1).build()
            val host = g.findPermanent("Grizzly Bears")!!
            val evaluator = services.predicateEvaluator
            EnchantRestriction.hostAllowsAura(g.state, g.state.projectedState, evaluator, g.findPermanent(shield.name)!!, host) shouldBe false
            EnchantRestriction.hostAllowsAura(g.state, g.state.projectedState, evaluator, g.findPermanent(all.name)!!, host) shouldBe true
        }
    }
}
