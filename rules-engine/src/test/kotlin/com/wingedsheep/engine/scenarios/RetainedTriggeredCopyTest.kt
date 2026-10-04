package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.BudgetMode
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.BudgetModalDecision
import com.wingedsheep.engine.core.BudgetModalResponse
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class RetainedTriggeredCopyTest : FunSpec({
    val copier = card("Test Retained Trigger Copier") {
        typeLine = "Creature — Shapeshifter"
        power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.you.beginningOf(Step.UPKEEP)
            val t = target(TargetFilter.Creature)
            effect = Effects.Composite(listOf(Effects.May(Effects.EachPermanentBecomesCopyOfTarget(
                target = t, affected = EffectTarget.Self,
                exceptions = CopyExceptions(retainResolvingTriggeredAbility = true)
            ))))
        }
    }
    val simple = card("Test Retained Self Copier") {
        typeLine = "Creature — Shapeshifter"
        power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.you.beginningOf(Step.UPKEEP)
            effect = Effects.EachPermanentBecomesCopyOfTarget(
                target = EffectTarget.Self, affected = EffectTarget.Self,
                exceptions = CopyExceptions(retainResolvingTriggeredAbility = true)
            )
        }
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(copier, simple))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    val json = Json { serializersModule = com.wingedsheep.engine.core.engineSerializersModule; allowStructuredMapKeys = true }
    fun roundTrip(d: GameTestDriver) {
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
    }
    fun nextOwnUpkeep(d: GameTestDriver) {
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.UPKEEP)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.UPKEEP)
    }
    fun card(d: GameTestDriver, id: EntityId) = d.state.getEntity(id)!!.get<CardComponent>()!!
    fun chooseAndCopy(d: GameTestDriver, target: EntityId, yes: Boolean = true) {
        d.submitTargetSelection(d.player1, listOf(target)).error shouldBe null
        d.bothPass().error shouldBe null
        d.submitYesNo(d.player1, yes).error shouldBe null
    }
    fun chooseOption(d: GameTestDriver, label: String) {
        val decision = d.pendingDecision as ChooseOptionDecision
        val index = decision.options.indexOf(label)
        check(index >= 0) { "Expected $label in ${decision.options}" }
        d.submitDecision(decision.playerId, OptionChosenResponse(decision.id, index)).error shouldBe null
    }
    val retainSelf = Effects.EachPermanentBecomesCopyOfTarget(
        target = EffectTarget.Self, affected = EffectTarget.Self,
        exceptions = CopyExceptions(retainResolvingTriggeredAbility = true)
    )
    test("preselected modal modes retain complete trigger after an earlier mode pauses") {
        val modalCopier = card("Test Retained Preselected Modal Copier") {
            typeLine = "Creature — Shapeshifter"
            power = 1; toughness = 1
            triggeredAbility {
                trigger = Triggers.you.beginningOf(Step.UPKEEP)
                effect = Effects.Modal(chooseCount = 2, modes = listOf(
                    Mode.noTarget(Effects.May(Effects.GainLife(1)), "Optional life"),
                    Mode.noTarget(retainSelf, "Copy with ability")
                ))
            }
        }
        val d = driver()
        d.registerCards(listOf(modalCopier))
        val id = d.putPermanentOnBattlefield(d.player1, modalCopier.name)
        nextOwnUpkeep(d)
        chooseOption(d, "Optional life")
        chooseOption(d, "Copy with ability")
        d.bothPass().error shouldBe null
        roundTrip(d)
        d.submitYesNo(d.player1, true).error shouldBe null
        val retained = card(d, id).copyTriggeredAbilities.single()
        retained.effect shouldBe modalCopier.script.triggeredAbilities.single().effect
        retained.trigger shouldBe modalCopier.script.triggeredAbilities.single().trigger
    }
    test("a resolution action choice retains its full enclosing trigger") {
        val choiceCopier = card("Test Retained Action Choice Copier") {
            typeLine = "Creature — Shapeshifter"
            power = 1; toughness = 1
            triggeredAbility {
                trigger = Triggers.you.beginningOf(Step.UPKEEP)
                effect = Effects.ChooseAction(listOf(
                    EffectChoice("Copy with ability", retainSelf),
                    EffectChoice("Gain life instead", Effects.GainLife(1))
                ))
            }
        }
        val d = driver()
        d.registerCards(listOf(choiceCopier))
        val id = d.putPermanentOnBattlefield(d.player1, choiceCopier.name)
        nextOwnUpkeep(d)
        d.bothPass().error shouldBe null
        roundTrip(d)
        chooseOption(d, "Copy with ability")
        card(d, id).copyTriggeredAbilities.single().effect shouldBe choiceCopier.script.triggeredAbilities.single().effect
    }
    test("a nested modal choice retains its full enclosing composite trigger") {
        val nestedCopier = card("Test Retained Nested Modal Copier") {
            typeLine = "Creature — Shapeshifter"
            power = 1; toughness = 1
            triggeredAbility {
                trigger = Triggers.you.beginningOf(Step.UPKEEP)
                effect = Effects.Composite(listOf(Effects.GainLife(1), Effects.Modal(modes = listOf(
                    Mode.noTarget(retainSelf, "Copy with ability"),
                    Mode.noTarget(Effects.GainLife(1), "Gain life instead")
                ))))
            }
        }
        val d = driver()
        d.registerCards(listOf(nestedCopier))
        val id = d.putPermanentOnBattlefield(d.player1, nestedCopier.name)
        nextOwnUpkeep(d)
        d.bothPass().error shouldBe null
        roundTrip(d)
        chooseOption(d, "Copy with ability")
        card(d, id).copyTriggeredAbilities.single().effect shouldBe nestedCopier.script.triggeredAbilities.single().effect
    }
    test("a budget mode retains its full enclosing trigger through its serialized decision") {
        val budgetCopier = card("Test Retained Budget Copier") {
            typeLine = "Creature — Shapeshifter"
            power = 1; toughness = 1
            triggeredAbility {
                trigger = Triggers.you.beginningOf(Step.UPKEEP)
                effect = Effects.BudgetModal(2, listOf(
                    BudgetMode(1, Effects.May(Effects.GainLife(1)), "Optional life"),
                    BudgetMode(1, retainSelf, "Copy with ability")
                ))
            }
        }
        val d = driver()
        d.registerCards(listOf(budgetCopier))
        val id = d.putPermanentOnBattlefield(d.player1, budgetCopier.name)
        nextOwnUpkeep(d)
        d.bothPass().error shouldBe null
        roundTrip(d)
        val decision = d.pendingDecision as BudgetModalDecision
        d.submitDecision(decision.playerId, BudgetModalResponse(decision.id, listOf(0, 1))).error shouldBe null
        roundTrip(d)
        d.submitYesNo(d.player1, true).error shouldBe null
        card(d, id).copyTriggeredAbilities.single().effect shouldBe budgetCopier.script.triggeredAbilities.single().effect
    }
    test("discover hand choice preserves the enclosing trigger for its follow-up copy") {
        val discoverCopier = card("Test Retained Discover Copier") {
            typeLine = "Creature — Shapeshifter"
            power = 1; toughness = 1
            triggeredAbility {
                trigger = Triggers.you.beginningOf(Step.UPKEEP)
                effect = Effects.Discover(2, thenEffect = retainSelf)
            }
        }
        val d = driver()
        d.registerCards(listOf(discoverCopier))
        val id = d.putPermanentOnBattlefield(d.player1, discoverCopier.name)
        val hit = d.putCardOnTopOfLibrary(d.player1, "Grizzly Bears")
        nextOwnUpkeep(d)
        d.bothPass().error shouldBe null
        roundTrip(d)
        d.submitYesNo(d.player1, false).error shouldBe null
        d.getHand(d.player1).contains(hit) shouldBe true
        card(d, id).copyTriggeredAbilities.single().effect shouldBe discoverCopier.script.triggeredAbilities.single().effect
    }
    test("declining a payable life cost retains the enclosing pay-or-suffer trigger") {
        val payer = card("Test Retained Pay Or Suffer Copier") {
            typeLine = "Creature — Shapeshifter"
            power = 1; toughness = 1
            triggeredAbility {
                trigger = Triggers.you.beginningOf(Step.UPKEEP)
                effect = Effects.PayOrSuffer(Costs.pay.PayLife(1), suffer = retainSelf)
            }
        }
        val d = driver()
        d.registerCards(listOf(payer))
        val id = d.putPermanentOnBattlefield(d.player1, payer.name)
        nextOwnUpkeep(d)
        d.bothPass().error shouldBe null
        roundTrip(d)
        d.submitYesNo(d.player1, false).error shouldBe null
        card(d, id).copyTriggeredAbilities.single().effect shouldBe payer.script.triggeredAbilities.single().effect
    }
    test("the next player's payment retains the enclosing any-player trigger") {
        val payer = card("Test Retained Any Player Copier") {
            typeLine = "Creature — Shapeshifter"
            power = 1; toughness = 1
            triggeredAbility {
                trigger = Triggers.you.beginningOf(Step.UPKEEP)
                effect = Effects.AnyPlayerMayPay(Costs.pay.PayLife(1), consequence = retainSelf)
            }
        }
        val d = driver()
        d.registerCards(listOf(payer))
        val id = d.putPermanentOnBattlefield(d.player1, payer.name)
        nextOwnUpkeep(d)
        d.bothPass().error shouldBe null
        roundTrip(d)
        d.submitYesNo(d.player1, false).error shouldBe null
        roundTrip(d)
        d.submitYesNo(d.player2, true).error shouldBe null
        card(d, id).copyTriggeredAbilities.single().effect shouldBe payer.script.triggeredAbilities.single().effect
    }
    test("a secret bid winner's copy retains the enclosing secret-bid trigger") {
        val bidder = card("Test Retained Secret Bid Copier") {
            typeLine = "Creature — Shapeshifter"
            power = 1; toughness = 1
            triggeredAbility {
                trigger = Triggers.you.beginningOf(Step.UPKEEP)
                effect = Effects.SecretBid(highestBidderEffect = retainSelf)
            }
        }
        val d = driver()
        d.registerCards(listOf(bidder))
        val id = d.putPermanentOnBattlefield(d.player1, bidder.name)
        nextOwnUpkeep(d)
        d.bothPass().error shouldBe null
        repeat(2) {
            roundTrip(d)
            val decision = d.pendingDecision!!
            val bid = if (decision.playerId == d.player1) 1 else 0
            d.submitDecision(decision.playerId, NumberChosenResponse(decision.id, bid)).error shouldBe null
        }
        card(d, id).copyTriggeredAbilities.single().effect shouldBe bidder.script.triggeredAbilities.single().effect
    }
    test("target and may pauses round trip complete trigger rules text and replace old instances") {
        val d = driver()
        val id = d.putPermanentOnBattlefield(d.player1, copier.name)
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        nextOwnUpkeep(d)
        roundTrip(d)
        d.submitTargetSelection(d.player1, listOf(bear)).error shouldBe null
        d.state.getEntity(d.state.stack.single())!!.get<TriggeredAbilityOnStackComponent>()!!
            .resolvingTriggeredAbility shouldBe copier.script.triggeredAbilities.single()
        d.bothPass().error shouldBe null
        roundTrip(d)
        val result = d.submitYesNo(d.player1, true)
        result.error shouldBe null
        result.events.filterIsInstance<com.wingedsheep.engine.core.CopiableTriggeredAbilityAddedEvent>()
            .single().entityId shouldBe id
        card(d, id).name shouldBe "Grizzly Bears"
        card(d, id).copyTriggeredAbilities.size shouldBe 1
        card(d, id).copyTriggeredAbilities.single().allTargetRequirements.size shouldBe 1
        nextOwnUpkeep(d)
        chooseAndCopy(d, bear)
        card(d, id).copyTriggeredAbilities.size shouldBe 1
    }
    test("declining the copy keeps the original identity and adds no ability") {
        val d = driver()
        val id = d.putPermanentOnBattlefield(d.player1, copier.name)
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        nextOwnUpkeep(d)
        chooseAndCopy(d, bear, false)
        card(d, id).name shouldBe copier.name
        card(d, id).copyTriggeredAbilities.size shouldBe 0
    }
    test("self copying retains printed trigger and adds a separately identified instance") {
        val d = driver()
        val id = d.putPermanentOnBattlefield(d.player1, simple.name)
        nextOwnUpkeep(d)
        d.bothPass().error shouldBe null
        card(d, id).copyTriggeredAbilities.size shouldBe 1
        nextOwnUpkeep(d)
        d.state.stack.size shouldBe 2
        d.bothPass().error shouldBe null
        d.bothPass().error shouldBe null
        card(d, id).copyTriggeredAbilities.size shouldBe 3
        card(d, id).copyTriggeredAbilities.map { it.id }.distinct().size shouldBe 3
    }
    test("changing the source before resolution retains the frozen trigger rather than its new text") {
        val d = driver()
        val id = d.putPermanentOnBattlefield(d.player1, copier.name)
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        nextOwnUpkeep(d)
        d.submitTargetSelection(d.player1, listOf(bear)).error shouldBe null
        // The same battlefield object changes its copiable identity while the trigger waits.
        val changed = card(d, bear).copy(ownerId = d.player1)
        d.replaceState(d.state.updateEntity(id) { it.with(changed) })
        d.bothPass().error shouldBe null
        d.submitYesNo(d.player1, true).error shouldBe null
        card(d, id).copyTriggeredAbilities.single().allTargetRequirements.size shouldBe 1
        nextOwnUpkeep(d)
        chooseAndCopy(d, bear)
        card(d, id).copyTriggeredAbilities.size shouldBe 1
    }
    test("text changes are frozen at detection even when the source text changes before resolution") {
        val changedCopier = card("Test Retained Elf Copier") {
            typeLine = "Creature — Shapeshifter"
            power = 1; toughness = 1
            triggeredAbility {
                trigger = Triggers.you.beginningOf(Step.UPKEEP)
                val t = target(TargetFilter.Creature.copy(baseFilter = Filters.Creature.withSubtype("Elf")))
                effect = Effects.Composite(listOf(Effects.May(Effects.EachPermanentBecomesCopyOfTarget(
                    target = t, affected = EffectTarget.Self,
                    exceptions = CopyExceptions(retainResolvingTriggeredAbility = true)
                ))))
            }
        }
        val d = driver()
        d.registerCards(listOf(changedCopier))
        val id = d.putPermanentOnBattlefield(d.player1, changedCopier.name)
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val replacement = com.wingedsheep.engine.state.components.identity.TextReplacementComponent(listOf(
            com.wingedsheep.engine.state.components.identity.TextReplacement(
                "Elf", "Bear", com.wingedsheep.engine.state.components.identity.TextReplacementCategory.CREATURE_TYPE
            )
        ))
        d.replaceState(d.state.updateEntity(id) { it.with(replacement) })
        nextOwnUpkeep(d)
        d.submitTargetSelection(d.player1, listOf(bear)).error shouldBe null
        val frozen = changedCopier.script.triggeredAbilities.single().applyTextReplacement(replacement)
        d.state.getEntity(d.state.stack.single())!!.get<TriggeredAbilityOnStackComponent>()!!
            .resolvingTriggeredAbility shouldBe frozen
        d.replaceState(d.state.updateEntity(id) {
            it.without<com.wingedsheep.engine.state.components.identity.TextReplacementComponent>()
        })
        d.bothPass().error shouldBe null
        d.submitYesNo(d.player1, true).error shouldBe null
        card(d, id).copyTriggeredAbilities.single().targetRequirement shouldBe frozen.targetRequirement
        nextOwnUpkeep(d)
        chooseAndCopy(d, bear)
        card(d, id).name shouldBe "Grizzly Bears"
    }
    test("an illegal copy target fizzles without appending a retained trigger") {
        val removal = card("Test Retained Copy Removal") {
            manaCost = "{U}"; typeLine = "Instant"
            spell { val t = target(TargetFilter.Creature); effect = Effects.Destroy(t) }
        }
        val d = driver()
        d.registerCards(listOf(removal))
        val id = d.putPermanentOnBattlefield(d.player1, copier.name)
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        nextOwnUpkeep(d)
        d.submitTargetSelection(d.player1, listOf(bear)).error shouldBe null
        val spell = d.putCardInHand(d.player1, removal.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpellWithTargets(d.player1, spell, listOf(
            com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(bear)
        )).error shouldBe null
        d.bothPass().error shouldBe null
        d.bothPass().error shouldBe null
        d.state.pendingDecision shouldBe null
        card(d, id).name shouldBe copier.name
        card(d, id).copyTriggeredAbilities.size shouldBe 0
    }
    test("copy applier adds one instance per copy and does nothing without an enclosing trigger") {
        val d = driver()
        val id = d.putPermanentOnBattlefield(d.player1, simple.name)
        val base = card(d, id)
        val exception = CopyExceptions(retainResolvingTriggeredAbility = true)
        val applier = com.wingedsheep.engine.handlers.effects.copy.CopyExceptionApplier
        applier.apply(base, exception) shouldBe base
        val first = applier.apply(base, exception, simple.script.triggeredAbilities.single())
        val second = applier.apply(first, exception, simple.script.triggeredAbilities.single())
        second.copyTriggeredAbilities.size shouldBe 2
        second.copyTriggeredAbilities.map { it.id }.distinct().size shouldBe 2
    }
    test("stack copy and pending rewrites keep the captured original ability") {
        val ability = copier.script.triggeredAbilities.single()
        val d = driver()
        val id = d.putPermanentOnBattlefield(d.player1, copier.name)
        val pending = com.wingedsheep.engine.event.PendingTrigger(
            ability = ability, sourceId = id, sourceName = copier.name,
            controllerId = d.player1, triggerContext = com.wingedsheep.engine.event.TriggerContext()
        )
        pending.copy(ability = ability.copy(effect = Effects.GainLife(1))).rulesText shouldBe ability
        val stacked = TriggeredAbilityOnStackComponent(
            sourceId = id, sourceName = copier.name, controllerId = d.player1,
            effect = ability.effect, description = ability.description, resolvingTriggeredAbility = ability
        )
        val copied = com.wingedsheep.engine.handlers.effects.stack.CopyTargetTriggeredAbilityExecutor
            .cloneAbility(stacked, d.player2)
        copied.resolvingTriggeredAbility shouldBe ability
        com.wingedsheep.engine.handlers.EffectContext.forTriggeredAbility(copied).resolvingTriggeredAbility shouldBe ability
    }
    for (fromSelf in listOf(false, true)) {
        test("token copy retains its enclosing trigger fromSelf=$fromSelf") {
            val maker = card("Test Retained Token Maker $fromSelf") {
                typeLine = "Creature — Shapeshifter"
                power = 1; toughness = 1
                triggeredAbility {
                    trigger = Triggers.you.beginningOf(Step.UPKEEP)
                    val exceptions = CopyExceptions(retainResolvingTriggeredAbility = true)
                    effect = if (fromSelf) Effects.CreateTokenCopyOfSelf(exceptions = exceptions)
                        else Effects.CreateTokenCopyOfTarget(EffectTarget.Self, exceptions = exceptions)
                }
            }
            val d = driver()
            d.registerCards(listOf(maker))
            val source = d.putPermanentOnBattlefield(d.player1, maker.name)
            nextOwnUpkeep(d)
            d.bothPass().error shouldBe null
            val token = d.state.getBattlefield().single { it != source && card(d, it).name == maker.name }
            card(d, token).copyTriggeredAbilities.size shouldBe 1
            card(d, token).copyTriggeredAbilities.single().effect shouldBe maker.script.triggeredAbilities.single().effect
        }
    }
    test("plain Clone inherits retained trigger and can recopy on its own upkeep") {
        val d = driver()
        val id = d.putPermanentOnBattlefield(d.player1, copier.name)
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        nextOwnUpkeep(d)
        chooseAndCopy(d, bear)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val clone = d.putCardInHand(d.player1, "Clone")
        d.giveMana(d.player1, Color.BLUE, 4)
        d.castSpell(d.player1, clone).error shouldBe null
        d.bothPass().error shouldBe null
        d.submitCardSelection(d.player1, listOf(id)).error shouldBe null
        card(d, clone).copyTriggeredAbilities.size shouldBe 1
        card(d, clone).copyTriggeredAbilities.single() shouldBe card(d, id).copyTriggeredAbilities.single()
        nextOwnUpkeep(d)
        d.submitTargetSelection(d.player1, listOf(bear)).error shouldBe null
        d.submitTargetSelection(d.player1, listOf(bear)).error shouldBe null
        d.state.stack.map { d.state.getEntity(it)!!.get<TriggeredAbilityOnStackComponent>()!!.sourceId }
            .toSet() shouldBe setOf(id, clone)
        repeat(2) {
            d.bothPass().error shouldBe null
            d.submitYesNo(d.player1, true).error shouldBe null
        }
        card(d, clone).name shouldBe "Grizzly Bears"
        card(d, clone).copyTriggeredAbilities.size shouldBe 1
    }
})
