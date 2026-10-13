package com.wingedsheep.engine.targeting

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.targeting.withTargetConstraints
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.utils.TargetEnumerationUtils
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.*
import io.kotest.matchers.shouldBe

class OwnerTargetDistributionTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMultiplayer(List(4) { Deck.of("Mountain" to 40) })
    }
    val req = TargetObject(filter = TargetFilter(GameObjectFilter.InstantOrSorcery, Zone.GRAVEYARD),
        ownerDistribution = OwnerDistribution.EXACTLY_ONE)
    test("enumeration counts owners with matching cards and exposes disjoint pools") {
        val d = setup()
        val a = d.putCardInGraveyard(d.player1, "Lightning Bolt")
        val b = d.putCardInGraveyard(d.player1, "Lightning Bolt")
        val c = d.putCardInGraveyard(d.player2, "Lightning Bolt")
        d.putCardInGraveyard(d.state.turnOrder[2], "Grizzly Bears")
        val info = TargetEnumerationUtils(PredicateEvaluator(null)).buildTargetInfos(d.state, d.player1, listOf(req)).single()
        info.minTargets shouldBe 2
        info.maxTargets shouldBe 2
        info.exclusiveGroups shouldBe listOf(listOf(a, b), listOf(c))
        val v = TargetValidator(PredicateEvaluator(null))
        fun check(ids: List<com.wingedsheep.sdk.model.EntityId>) = v.validateTargets(d.state,
            ids.map { ChosenTarget.Card(it, d.state.getEntity(it)!!.get<com.wingedsheep.engine.state.components.identity.OwnerComponent>()!!.playerId, Zone.GRAVEYARD) }, listOf(req), d.player1, targetingSourceType = com.wingedsheep.engine.handlers.TargetingSourceType.SPELL)
        check(listOf(a, b)).shouldNotBeNull()
        check(listOf(a)).shouldNotBeNull()
        check(listOf(a, c)).shouldBeNull()
        check(emptyList()).shouldNotBeNull()
        val optional = req.copy(ownerDistribution = OwnerDistribution.UP_TO_ONE)
        v.validateTargets(d.state, emptyList(), listOf(optional), d.player1, targetingSourceType = com.wingedsheep.engine.handlers.TargetingSourceType.SPELL).shouldBeNull()
    }
    test("empty eligible scope has zero required targets") {
        val d = setup()
        val info = TargetEnumerationUtils(PredicateEvaluator(null)).buildTargetInfos(d.state, d.player1, listOf(req)).single()
        info.minTargets shouldBe 0
        info.maxTargets shouldBe 0
        TargetValidator(PredicateEvaluator(null)).validateTargets(d.state, emptyList(), listOf(req), d.player1, targetingSourceType = com.wingedsheep.engine.handlers.TargetingSourceType.SPELL).shouldBeNull()
    }
    test("numeric compatibility requires enough distinct owners, not just enough cards") {
        val one = card("Owner Value One") { manaCost = "{1}"; typeLine = "Instant" }
        val two = card("Owner Value Two") { manaCost = "{2}"; typeLine = "Instant" }
        val d = setup().apply { registerCards(listOf(one, two)) }
        val a = d.putCardInGraveyard(d.player1, one.name)
        val aa = d.putCardInGraveyard(d.player1, one.name)
        val b = d.putCardInGraveyard(d.player2, two.name)
        val combined = req.copy(sameNumericProperty = CardNumericProperty.MANA_VALUE)
        val utils = TargetEnumerationUtils(PredicateEvaluator(null))
        val impossible = utils.buildTargetInfos(d.state, d.player1, listOf(combined)).single()
        impossible.minTargets shouldBe 2
        impossible.validTargets shouldBe emptyList()
        val bb = d.putCardInGraveyard(d.player2, one.name)
        val info = utils.buildTargetInfos(d.state, d.player1, listOf(combined)).single()
        info.selectionGroups shouldBe listOf(listOf(a, aa, bb))
        val pending = TargetRequirementInfo(0, "combined").withTargetConstraints(d.state, combined, listOf(a, aa, b, bb))
        pending.minTargets shouldBe 2
        pending.maxTargets shouldBe 2
    }

    val spell = card("Owner Spell Count Probe") {
        manaCost = "{0}"; typeLine = "Sorcery"
        spell {
            targets(req.filter, ownerDistribution = OwnerDistribution.EXACTLY_ONE)
            effect = Effects.GainLife(3, target(Targets.Player))
        }
    }
    val activated = card("Owner Activation Count Probe") {
        manaCost = "{0}"; typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            targets(req.filter, ownerDistribution = OwnerDistribution.EXACTLY_ONE)
            effect = Effects.GainLife(3, target(Targets.Player))
        }
    }
    val modal = card("Owner Modal Count Probe") {
        manaCost = "{0}"; typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = com.wingedsheep.sdk.scripting.effects.ModalEffect.chooseOne(
                mode("Distributed targets then a player") {
                    targets(req.filter, ownerDistribution = OwnerDistribution.EXACTLY_ONE)
                    effect = Effects.GainLife(3, target(Targets.Player))
                })
        }
    }
    fun resolve(d: GameTestDriver) {
        var guard = 0
        while (d.state.stack.isNotEmpty() && d.pendingDecision == null && guard++ < 40)
            d.passPriority(d.state.priorityPlayerId!!).error.shouldBeNull()
        d.state.stack.size shouldBe 0
    }
    for (isActivation in listOf(false, true)) for (ownerCount in listOf(0, 2)) {
        test("${if (isActivation) "activation" else "spell"} binds $ownerCount owner targets before a following player target") {
            val d = setup().apply {
                registerCards(listOf(spell, activated))
                passPriorityUntil(Step.PRECOMBAT_MAIN)
            }
            val cards = if (ownerCount == 0) emptyList() else listOf(
                d.putCardInGraveyard(d.player1, "Lightning Bolt"), d.putCardInGraveyard(d.player2, "Lightning Bolt"))
            val chosen = cards.zip(listOf(d.player1, d.player2)).map { (id, owner) -> ChosenTarget.Card(id, owner, Zone.GRAVEYARD) } +
                ChosenTarget.Player(d.player1)
            if (isActivation) {
                val source = d.putPermanentOnBattlefield(d.player1, activated.name)
                d.submit(ActivateAbility(d.player1, source, activated.activatedAbilities.single().id, targets = chosen)).error.shouldBeNull()
            } else {
                d.castSpellWithTargets(d.player1, d.putCardInHand(d.player1, spell.name), chosen).error.shouldBeNull()
            }
            val stored = d.state.getEntity(d.state.stack.last())!!.get<TargetsComponent>()!!
            stored.targetRequirements.map { it.count } shouldBe listOf(ownerCount, 1)
            val bindings = EffectContext.buildNamedTargets(stored.targetRequirements, stored.targets)
            bindings[stored.targetRequirements.last().id] shouldBe ChosenTarget.Player(d.player1)
            if (cards.isNotEmpty()) d.replaceState(d.state.removeEntity(cards.first()))
            resolve(d)
            d.getLifeTotal(d.player1) shouldBe 23
        }
    }
    test("a modal trigger binds owner counts and the following player's named target") {
        val d = setup().apply { registerCards(listOf(modal)); passPriorityUntil(Step.PRECOMBAT_MAIN) }
        val cards = listOf(d.putCardInGraveyard(d.player1, "Lightning Bolt"), d.putCardInGraveyard(d.player2, "Lightning Bolt"))
        d.castSpell(d.player1, d.putCardInHand(d.player1, modal.name)).error.shouldBeNull()
        var guard = 0
        while (d.pendingDecision == null && guard++ < 40) d.passPriority(d.state.priorityPlayerId!!).error.shouldBeNull()
        (d.pendingDecision as? ChooseOptionDecision)?.let { dec ->
            d.submitDecision(dec.playerId, OptionChosenResponse(dec.id, 0)).error.shouldBeNull()
        }
        val decision = d.pendingDecision as ChooseTargetsDecision
        d.submitDecision(decision.playerId, TargetsResponse(decision.id, mapOf(0 to cards, 1 to listOf(d.player1)))).error.shouldBeNull()
        val stored = d.state.getEntity(d.state.stack.last())!!.get<TargetsComponent>()!!
        stored.targetRequirements.map { it.count } shouldBe listOf(2, 1)
        val component = d.state.getEntity(d.state.stack.last())!!.get<TriggeredAbilityOnStackComponent>()!!
        component.modeTargetRequirements.getValue(0).map { it.count } shouldBe listOf(2, 1)
        d.replaceState(d.state.removeEntity(cards.first()))
        resolve(d)
        d.getLifeTotal(d.player1) shouldBe 23
    }

    test("modal target counts preserve an explicitly skipped owner group before an overlapping optional slot") {
        val probe = card("Owner Modal Optional Count Probe") {
            manaCost = "{0}"; typeLine = "Artifact"
            triggeredAbility {
                trigger = Triggers.self.enters()
                effect = com.wingedsheep.sdk.scripting.effects.ModalEffect.chooseOne(mode("Optional groups") {
                    targets(req.filter, ownerDistribution = OwnerDistribution.UP_TO_ONE)
                    effect = Effects.Exile(target(req.filter, optional = true))
                })
            }
        }
        val d = setup().apply { registerCards(listOf(probe)); passPriorityUntil(Step.PRECOMBAT_MAIN) }
        val a = d.putCardInGraveyard(d.player1, "Lightning Bolt")
        d.castSpell(d.player1, d.putCardInHand(d.player1, probe.name)).error.shouldBeNull()
        var guard = 0
        while (d.pendingDecision == null && guard++ < 40) d.passPriority(d.state.priorityPlayerId!!).error.shouldBeNull()
        (d.pendingDecision as? ChooseOptionDecision)?.let { dec ->
            d.submitDecision(dec.playerId, OptionChosenResponse(dec.id, 0)).error.shouldBeNull()
        }
        val decision = d.pendingDecision as ChooseTargetsDecision
        d.submitDecision(decision.playerId, TargetsResponse(decision.id, mapOf(0 to emptyList(), 1 to listOf(a)))).error.shouldBeNull()
        d.state.getEntity(d.state.stack.last())!!.get<TargetsComponent>()!!.targetRequirements.map { it.count } shouldBe listOf(0, 1)
        resolve(d)
        (a in d.state.getZone(d.player1, Zone.EXILE)) shouldBe true
    }

    test("repeated modal choices keep different owner counts and their own following recipients") {
        val probe = card("Owner Repeated Mode Probe") {
            manaCost = "{0}"; typeLine = "Artifact"
            triggeredAbility {
                trigger = Triggers.self.enters()
                effect = com.wingedsheep.sdk.scripting.effects.ModalEffect(
                    modes = listOf(mode("Optional owner targets then a player") {
                        targets(req.filter, ownerDistribution = OwnerDistribution.UP_TO_ONE)
                        effect = Effects.GainLife(3, target(Targets.Player))
                    }), chooseCount = 2, allowRepeat = true)
            }
        }
        val d = setup().apply { registerCards(listOf(probe)); passPriorityUntil(Step.PRECOMBAT_MAIN) }
        val a = d.putCardInGraveyard(d.player1, "Lightning Bolt")
        val b = d.putCardInGraveyard(d.player2, "Lightning Bolt")
        d.castSpell(d.player1, d.putCardInHand(d.player1, probe.name)).error.shouldBeNull()
        var guard = 0
        while (d.pendingDecision == null && guard++ < 40) d.passPriority(d.state.priorityPlayerId!!).error.shouldBeNull()
        while (d.pendingDecision is ChooseOptionDecision) {
            val dec = d.pendingDecision as ChooseOptionDecision
            d.submitDecision(dec.playerId, OptionChosenResponse(dec.id, 0)).error.shouldBeNull()
        }
        val first = d.pendingDecision as ChooseTargetsDecision
        d.submitDecision(first.playerId, TargetsResponse(first.id, mapOf(0 to emptyList(), 1 to listOf(d.player1)))).error.shouldBeNull()
        val second = d.pendingDecision as ChooseTargetsDecision
        d.submitDecision(second.playerId, TargetsResponse(second.id, mapOf(0 to listOf(a, b), 1 to listOf(d.player2)))).error.shouldBeNull()
        val stored = d.state.getEntity(d.state.stack.last())!!.get<TriggeredAbilityOnStackComponent>()!!
        stored.modeTargetRequirementsOrdered.map { group -> group.map { it.count } } shouldBe listOf(listOf(0, 1), listOf(2, 1))
        d.replaceState(d.state.removeEntity(a))
        resolve(d)
        d.getLifeTotal(d.player1) shouldBe 23
        d.getLifeTotal(d.player2) shouldBe 23
    }

    for (isActivation in listOf(false, true)) {
        test("repeated modal ${if (isActivation) "activation" else "spell"} retains each occurrence's bound owner counts") {
            val repeated = com.wingedsheep.sdk.scripting.effects.ModalEffect(
                modes = listOf(mode("Optional owner targets then a player") {
                    targets(req.filter, ownerDistribution = OwnerDistribution.UP_TO_ONE)
                    effect = Effects.GainLife(3, target(Targets.Player))
                }), chooseCount = 2, allowRepeat = true)
            val probe = card("Owner Repeated ${if (isActivation) "Activation" else "Spell"} Probe") {
                manaCost = "{0}"; typeLine = if (isActivation) "Artifact" else "Sorcery"
                if (isActivation) activatedAbility { cost = Costs.Tap; effect = repeated }
                else spell { effect = repeated }
            }
            val d = setup().apply { registerCards(listOf(probe)); passPriorityUntil(Step.PRECOMBAT_MAIN) }
            val a = d.putCardInGraveyard(d.player1, "Lightning Bolt")
            val b = d.putCardInGraveyard(d.player2, "Lightning Bolt")
            val picks = listOf(listOf(ChosenTarget.Player(d.player1)), listOf(
                ChosenTarget.Card(a, d.player1, Zone.GRAVEYARD),
                ChosenTarget.Card(b, d.player2, Zone.GRAVEYARD), ChosenTarget.Player(d.player2)))
            if (isActivation) {
                val source = d.putPermanentOnBattlefield(d.player1, probe.name)
                d.submit(ActivateAbility(d.player1, source, probe.activatedAbilities.single().id,
                    targets = picks.flatten(), chosenModes = listOf(0, 0), modeTargetsOrdered = picks)).error.shouldBeNull()
            } else {
                d.submit(CastSpell(d.player1, d.putCardInHand(d.player1, probe.name),
                    chosenModes = listOf(0, 0), modeTargetsOrdered = picks)).error.shouldBeNull()
            }
            d.state.getEntity(d.state.stack.last())!!.get<TargetsComponent>()!!.targetRequirements.map { it.count } shouldBe listOf(0, 1, 2, 1)
            d.replaceState(d.state.removeEntity(a))
            resolve(d)
            d.getLifeTotal(d.player1) shouldBe 23
            d.getLifeTotal(d.player2) shouldBe 23
        }
    }

})
