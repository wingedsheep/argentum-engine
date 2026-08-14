package com.wingedsheep.engine.targeting

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.targeting.StackObjectTargeting
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Engine-level coverage of targeting an **ability on the stack by its source** — the mechanic behind
 * Echo, Perceptive Prodigy and Scientist Supreme of A.I.M.
 *
 * Two halves:
 *
 * 1. [StackObjectTargeting.permitsAbilities] — the single seam deciding whether a stack-zone target
 *    requirement may offer abilities at all. "Target spell" must stay spell-only (the
 *    `TargetSpellExcludesAbilitiesTest` regression); an ability-naming filter, including the new
 *    [CardPredicate.AbilitySourceMatches], must not.
 *
 * 2. Enumeration parity. The legal-action enumerator and the authoritative target finder ask that
 *    question separately, and the enumerator used to answer "spells only" unconditionally — so an
 *    ability-targeting card was accepted when executed but never *offered*, i.e. unplayable through
 *    the UI and invisible to the AI. That is asserted here on a plain
 *    `Targets.ActivatedOrTriggeredAbilityYouControl` card, independent of the source predicate.
 */
class AbilitySourceTargetingTest : FunSpec({

    context("StackObjectTargeting.permitsAbilities") {
        test("plain 'target spell' filters stay spell-only") {
            StackObjectTargeting.permitsAbilities(TargetFilter.SpellOnStack.baseFilter) shouldBe false
            StackObjectTargeting.permitsAbilities(TargetFilter.CreatureSpellOnStack.baseFilter) shouldBe false
        }

        test("filters naming an ability permit abilities") {
            StackObjectTargeting.permitsAbilities(
                TargetFilter.ActivatedOrTriggeredAbilityOnStack.baseFilter
            ) shouldBe true
            StackObjectTargeting.permitsAbilities(
                TargetFilter.InstantSorcerySpellOrAbilityOnStack.baseFilter
            ) shouldBe true
        }

        test("an ability-source predicate alone permits abilities") {
            // It can only ever be true for an ability, so it must not be treated as spell-only.
            StackObjectTargeting.permitsAbilities(
                GameObjectFilter(
                    cardPredicates = listOf(CardPredicate.AbilitySourceMatches(GameObjectFilter.Creature))
                )
            ) shouldBe true
        }

        test("the Echo/Scientist target filter permits abilities") {
            val echoLike = (
                Targets.ActivatedOrTriggeredAbilityYouControlFrom(GameObjectFilter.Artifact)
                    as TargetObject
                ).filter
            StackObjectTargeting.permitsAbilities(echoLike.baseFilter) shouldBe true
        }
    }

    context("enumeration offers abilities on the stack") {
        // {T}: Target creature you control gets +1/+0 until end of turn.
        val pumper = card("Stack Target Test Pumper") {
            manaCost = "{1}"
            typeLine = "Creature — Soldier"
            power = 1
            toughness = 1
            oracleText = "{T}: Target creature you control gets +1/+0 until end of turn."
            activatedAbility {
                cost = AbilityCost.Tap
                effect = Effects.ModifyStats(1, 0, EffectTarget.ContextTarget(0))
                target = Targets.CreatureYouControl
                timing = TimingRule.InstantSpeed
            }
        }

        // The unnarrowed "copy target activated or triggered ability you control" shape shared by
        // Gogo, Master of Mimicry and Peter Parker's Camera.
        val copier = card("Stack Target Test Copier") {
            manaCost = "{1}"
            typeLine = "Artifact"
            oracleText = "{T}: Copy target activated or triggered ability you control."
            activatedAbility {
                cost = AbilityCost.Tap
                val ability = target(
                    "target activated or triggered ability you control",
                    Targets.ActivatedOrTriggeredAbilityYouControl
                )
                effect = Effects.CopyTargetSpellOrAbility(ability)
                timing = TimingRule.InstantSpeed
            }
        }

        test("an activated ability on the stack is enumerated as a legal target for a copy ability") {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all + listOf(pumper, copier))
            driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
            val me = driver.activePlayer!!

            val pumperId = driver.putCreatureOnBattlefield(me, "Stack Target Test Pumper")
            driver.removeSummoningSickness(pumperId)
            val copierId = driver.putPermanentOnBattlefield(me, "Stack Target Test Copier")
            val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val pumpAbilityId =
                driver.cardRegistry.requireCard("Stack Target Test Pumper").activatedAbilities[0].id
            driver.submitSuccess(
                ActivateAbility(
                    playerId = me, sourceId = pumperId, abilityId = pumpAbilityId,
                    targets = listOf(ChosenTarget.Permanent(bears))
                )
            )
            val pumpOnStack = driver.getTopOfStack()!!

            val offered = driver.legalActions(me)
                .filter { (it.action as? ActivateAbility)?.sourceId == copierId }
                .flatMap { it.validTargets ?: emptyList() }
            offered shouldContain pumpOnStack
        }
    }
})
