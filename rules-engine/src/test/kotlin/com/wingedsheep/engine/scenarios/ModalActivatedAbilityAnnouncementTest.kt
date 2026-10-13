package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.battlefield.ChosenModesEverComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class ModalActivatedAbilityAnnouncementTest : ScenarioTestBase() {
    private val modalCard = card("Modal Test Artifact") {
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = ModalEffect.chooseOneNotYetChosen(
                mode("Destroy target creature and gain 2 life") {
                    val creature = target(TargetFilter.Creature)
                    effect = Effects.Destroy(creature) then Effects.GainLife(2)
                },
                Mode.noTarget(Effects.DrawCards(1), "Draw a card")
            )
        }
    }
    private val multiCard = card("Multi Modal Test Artifact") {
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = ModalEffect(modes = listOf(
                mode("Destroy target creature and gain 2 life") {
                    val creature = target(TargetFilter.Creature)
                    effect = Effects.Destroy(creature) then Effects.GainLife(2)
                }, Mode.noTarget(Effects.DrawCards(1), "Draw a card")
            ), chooseCount = 2)
        }
    }
    private val repeatCard = card("Repeat Modal Test Artifact") {
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = ModalEffect(modes = listOf(
                Mode.noTarget(Effects.GainLife(2), "Gain 2 life"),
                Mode.noTarget(Effects.DrawCards(1), "Draw a card")
            ), chooseCount = 2, allowRepeat = true)
        }
    }
    private val zeroCard = card("Zero Modal Test Artifact") {
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = ModalEffect(modes = listOf(Mode.noTarget(Effects.DrawCards(1))), chooseCount = 1, minChooseCount = 0)
        }
    }
    private val twoTargetCard = card("Two Target Modal Test Artifact") {
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = ModalEffect(modes = listOf(
                mode("Destroy target creature and gain 2 life") {
                    val creature = target(TargetFilter.Creature)
                    effect = Effects.Destroy(creature) then Effects.GainLife(2)
                },
                mode("Tap target creature") {
                    val creature = target(TargetFilter.Creature)
                    effect = Effects.Tap(creature)
                }
            ), chooseCount = 2)
        }
    }
    private val wardCard = card("Warded Modal Test Creature") {
        typeLine = "Creature — Bear"
        power = 2; toughness = 2
        keywordAbility(com.wingedsheep.sdk.scripting.KeywordAbility.Ward(com.wingedsheep.sdk.scripting.effects.WardCost.Mana("{2}")))
    }
    private val optionalGroups = card("Optional Modal Target Groups") {
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = ModalEffect.chooseOne(
                mode("Add counters to up to two separately targeted creatures") {
                    val first = target(com.wingedsheep.sdk.scripting.targets.TargetObject(filter = TargetFilter.Creature, count = 1, minCount = 0))
                    val second = target(com.wingedsheep.sdk.scripting.targets.TargetObject(filter = TargetFilter.Creature, count = 1, minCount = 0))
                    effect = Effects.AddCounters(com.wingedsheep.sdk.core.CounterType.CHARGE, 1, first) then
                        Effects.AddCounters(com.wingedsheep.sdk.core.CounterType.CHARGE, 2, second)
                }, Mode.noTarget(Effects.DrawCards(1), "Draw a card")
            )
        }
    }
    private val outerGroups = card("Outer Modal Target Groups") {
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            val first = target(TargetFilter.Creature, optional = true)
            val second = target(TargetFilter.Creature, optional = true)
            effect = ModalEffect.chooseOne(Mode.noTarget(
                Effects.AddCounters(com.wingedsheep.sdk.core.CounterType.CHARGE, 1, first) then
                    Effects.AddCounters(com.wingedsheep.sdk.core.CounterType.CHARGE, 2, second),
                "Add counters to the announced creatures"
            ))
        }
    }
    private val modalMana = card("Modal Mana Test Artifact") {
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.PayLife(1)
            manaAbility = true
            effect = ModalEffect.chooseOne(
                Mode.noTarget(Effects.AddMana(com.wingedsheep.sdk.core.Color.GREEN), "Add green"),
                Mode.noTarget(Effects.AddMana(com.wingedsheep.sdk.core.Color.RED), "Add red")
            )
        }
    }
    init {
        cardRegistry.register(listOf(modalCard, multiCard, repeatCard, zeroCard, twoTargetCard, wardCard, optionalGroups, outerGroups, modalMana))
        fun game(name: String = modalCard.name, creature: Boolean = true): TestGame {
            val b = scenario().withPlayers().withCardOnBattlefield(1, name)
                .withCardInLibrary(1, "Forest").withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
            if (creature) b.withCardOnBattlefield(2, "Grizzly Bears")
            return b.build()
        }
        fun TestGame.action(name: String = modalCard.name) = ActivateAbility(player1Id, findPermanent(name)!!,
            cardRegistry.getCard(name)!!.activatedAbilities.single().id)
        fun TestGame.pick(index: Int) = submitDecision(OptionChosenResponse(getPendingDecision()!!.id, index))

        for (cancel in listOf(false, true)) {
            test("modal mana choice restores the enclosing payment window when ${if (cancel) "cancelled" else "completed"}") {
                val g = scenario().withPlayers().withCardOnBattlefield(1, modalCard.name)
                    .withCardOnBattlefield(1, modalMana.name).withCardOnBattlefield(2, wardCard.name)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest").build()
                g.execute(g.action()); g.pick(0); g.selectTargets(listOf(g.findPermanent(wardCard.name)!!))
                g.resolveStack()
                val payment = g.getPendingDecision() as SelectManaSourcesDecision
                g.execute(g.action(modalMana.name)).error shouldBe null
                if (cancel) g.submitDecision(CancelDecisionResponse(g.getPendingDecision()!!.id)).error shouldBe null
                else g.pick(0).error shouldBe null
                (g.getPendingDecision() as SelectManaSourcesDecision).id shouldBe payment.id
                g.state.continuationStack.filterIsInstance<ReopenManaPaymentDecisionContinuation>() shouldBe emptyList()
                g.getLifeTotal(1) shouldBe if (cancel) 20 else 19
                (g.state.getEntity(g.player1Id)!!.get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()?.green ?: 0) shouldBe if (cancel) 0 else 1
                g.submitManaSourcesDecision().error shouldBe null
                g.resolveStack(); g.hasPendingDecision() shouldBe false
            }
        }
        test("modal stack placement preserves an omitted outer target group") {
            val g = game(outerGroups.name); val bear = g.findPermanent("Grizzly Bears")!!
            g.execute(g.action(outerGroups.name).copy(
                targets = listOf(ChosenTarget.Permanent(bear)), targetGroupCounts = listOf(0, 1),
                chosenModes = listOf(0), modeTargetsOrdered = listOf(emptyList())
            )).error shouldBe null
            val requirements = g.state.getEntity(g.state.stack.single())!!
                .get<com.wingedsheep.engine.state.components.stack.TargetsComponent>()!!.targetRequirements
            requirements.map { it.count } shouldBe listOf(0, 1)
            g.resolveStack()
            g.state.getEntity(bear)!!.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()!!
                .getCount(com.wingedsheep.sdk.core.CounterType.CHARGE) shouldBe 2
        }

        test("mandatory-target modes disappear when they have no legal targets") {
            val g = game(creature = false)
            g.execute(g.action()).error shouldBe null
            (g.getPendingDecision() as ChooseOptionDecision).options shouldBe listOf("Draw a card")
            g.pick(0); g.handSize(1) shouldBe 0
            g.resolveStack(); g.handSize(1) shouldBe 1
        }
        test("unavailable, duplicate and mismatched submitted modes are rejected before payment") {
            for (modes in listOf(listOf(3), listOf(0, 0), emptyList())) {
                val g = game(); val action = g.action()
                g.execute(action.copy(chosenModes = modes, modeTargetsOrdered = modes.map { emptyList() })).error shouldNotBe null
                g.state.getEntity(action.sourceId)!!.has<TappedComponent>() shouldBe false
            }
            val g = game()
            g.execute(g.action().copy(chosenModes = listOf(0), modeTargetsOrdered = emptyList())).error shouldNotBe null
        }
        test("mode continuation round-trips and cancelling it leaves no activation state") {
            val g = game(); val action = g.action(); g.execute(action)
            val frame = (g.state.continuationStack.last() as Suspension).answer as ActivationModalModeContinuation
            val encoded = Json.encodeToString(frame)
            Json.decodeFromString<ActivationModalModeContinuation>(encoded) shouldBe frame
            g.submitDecision(CancelDecisionResponse(g.getPendingDecision()!!.id)).error shouldBe null
            g.state.stack shouldBe emptyList()
            g.state.getEntity(action.sourceId)!!.get<ChosenModesEverComponent>() shouldBe null
        }
        test("targets are announced and visible before resolution and an all-illegal target fizzles the whole ability") {
            val g = game(); val action = g.action(); val bear = g.findPermanent("Grizzly Bears")!!
            g.execute(action); g.pick(0)
            g.state.getEntity(action.sourceId)!!.has<TappedComponent>() shouldBe false
            g.selectTargets(listOf(bear)).error shouldBe null
            val stack = g.state.getEntity(g.state.stack.single())!!.get<ActivatedAbilityOnStackComponent>()!!
            stack.chosenModes shouldBe listOf(0)
            stack.modeTargetsOrdered shouldBe listOf(listOf(ChosenTarget.Permanent(bear)))
            val client = g.getClientState(2)
            client.cards[g.state.stack.single()]!!.chosenModeDescriptions shouldBe listOf("Destroy target creature and gain 2 life")
            g.state = g.state.removeEntity(bear)
            g.resolveStack(); g.getLifeTotal(1) shouldBe 20
            g.state.getEntity(action.sourceId)!!.get<ChosenModesEverComponent>()!!.modeIndices shouldBe setOf(0)
        }
        test("all-illegal targets also fizzle a chosen nontarget mode") {
            val g = game(multiCard.name); val bear = g.findPermanent("Grizzly Bears")!!
            g.execute(g.action(multiCard.name)); g.pick(0); g.pick(0)
            g.selectTargets(listOf(bear)).error shouldBe null
            g.state = g.state.removeEntity(bear)
            // There is only one target across both modes; all illegal means the entire ability fizzles.
            g.resolveStack(); g.handSize(1) shouldBe 0; g.getLifeTotal(1) shouldBe 20
        }
        test("choose-two executes in printed order and carries each mode's targets") {
            val g = game(multiCard.name); val bear = g.findPermanent("Grizzly Bears")!!
            g.execute(g.action(multiCard.name)); g.pick(1); g.pick(0)
            g.selectTargets(listOf(bear)).error shouldBe null
            g.resolveStack(); g.handSize(1) shouldBe 1; g.getLifeTotal(1) shouldBe 22
            g.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }
        test("repeatable modes execute each announced occurrence") {
            val g = game(repeatCard.name)
            g.execute(g.action(repeatCard.name)); g.pick(0); g.pick(0)
            g.resolveStack(); g.getLifeTotal(1) shouldBe 24
        }
        test("explicitly choosing zero resolves without asking for a mode again") {
            val g = game(zeroCard.name)
            g.execute(g.action(zeroCard.name)); g.pick(1).error shouldBe null
            g.resolveStack(); g.hasPendingDecision() shouldBe false; g.handSize(1) shouldBe 0
        }
        test("an exhausted source offers no activation and rejects an engine-direct submission") {
            val g = game(); val action = g.action()
            g.state = g.state.updateEntity(action.sourceId) { it.with(ChosenModesEverComponent(setOf(0, 1))) }
            g.execute(action).error shouldNotBe null
            g.getLegalActions(1).none { (it.action as? ActivateAbility)?.sourceId == action.sourceId } shouldBe true
        }
        test("an illegal mode target cannot borrow another mode's named target") {
            val g = scenario().withPlayers().withCardOnBattlefield(1, twoTargetCard.name)
                .withCardOnBattlefield(2, "Grizzly Bears").withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest").build()
            val bear = g.findPermanent("Grizzly Bears")!!; val giant = g.findPermanent("Hill Giant")!!
            g.execute(g.action(twoTargetCard.name)); g.pick(0); g.pick(0)
            g.selectTargets(listOf(bear)); g.selectTargets(listOf(giant)).error shouldBe null
            g.state = g.state.removeEntity(bear)
            g.resolveStack(); g.getLifeTotal(1) shouldBe 22
            g.state.getEntity(giant) shouldNotBe null
            g.state.getEntity(giant)!!.has<TappedComponent>() shouldBe true
        }
        test("copies retain the mode and use the copy's new target without recording a new choice") {
            val g = scenario().withPlayers().withCardOnBattlefield(1, modalCard.name)
                .withCardOnBattlefield(2, "Grizzly Bears").withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest").build()
            val action = g.action(); val bear = g.findPermanent("Grizzly Bears")!!; val giant = g.findPermanent("Hill Giant")!!
            g.execute(action); g.pick(0); g.selectTargets(listOf(bear))
            val original = g.state.stack.single()
            val reqs = g.state.getEntity(original)!!.get<com.wingedsheep.engine.state.components.stack.TargetsComponent>()!!.targetRequirements
            val copied = com.wingedsheep.engine.handlers.effects.stack.CopyTargetSpellOrAbilityExecutor.cloneAndPush(
                g.state, original, g.player1Id, listOf(ChosenTarget.Permanent(giant)), reqs)
            copied.error shouldBe null; g.state = copied.state
            g.resolveStack(); g.isInGraveyard(2, "Grizzly Bears") shouldBe true
            g.isInGraveyard(2, "Hill Giant") shouldBe true; g.getLifeTotal(1) shouldBe 24
            g.state.getEntity(action.sourceId)!!.get<ChosenModesEverComponent>()!!.modeIndices shouldBe setOf(0)
            g.hasPendingDecision() shouldBe false
        }
        test("announcement emits targeting events so ward can counter a modal activation") {
            val g = scenario().withPlayers().withCardOnBattlefield(1, modalCard.name)
                .withCardOnBattlefield(2, wardCard.name).withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest").build()
            val target = g.findPermanent(wardCard.name)!!
            val action = g.action(); g.execute(action); g.pick(0)
            g.selectTargets(listOf(target)).error shouldBe null
            g.state.stack.size shouldBe 2
            g.resolveStack()
            val decision = g.getPendingDecision() as SelectManaSourcesDecision
            decision.requiredCost shouldBe "{2}"
            g.submitManaSourcesDecision().error shouldBe null
            g.resolveStack(); g.state.getEntity(target) shouldNotBe null
            g.state.getEntity(action.sourceId)!!.get<ChosenModesEverComponent>()!!.modeIndices shouldBe setOf(0)
        }
        test("an omitted optional target slot stays distinct from the following slot") {
            val g = game(optionalGroups.name); val bear = g.findPermanent("Grizzly Bears")!!
            g.execute(g.action(optionalGroups.name)); g.pick(0)
            g.submitDecision(TargetsResponse(g.getPendingDecision()!!.id, mapOf(0 to emptyList(), 1 to listOf(bear)))).error shouldBe null
            g.resolveStack()
            g.state.getEntity(bear)!!.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()!!
                .getCount(com.wingedsheep.sdk.core.CounterType.CHARGE) shouldBe 2
        }
    }
}
