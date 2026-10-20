package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.mechanics.mana.isSatisfiedBy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Color
import io.kotest.matchers.shouldBe

/**
 * `StatePredicate.HasActivatedAbilities` counts the activated abilities an object *has* — printed,
 * keyword (cycling) and granted, activatable now or not — and drops the own ones a lose-all-abilities
 * effect removed. Plus the `permanentSourcesOnly` axis of `CardTypeSpellsOrAbilitiesOnly`.
 */
class HasActivatedAbilitiesPredicateTest : ScenarioTestBase() {
    init {
        val pinger = card("Presence Test Pinger") {
            typeLine = "Creature — Human"; power = 1; toughness = 1
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.GainLife(1)
            }
        }
        val dork = card("Presence Test Dork") {
            typeLine = "Creature — Elf"; power = 1; toughness = 1
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.AddMana(Color.GREEN)
                manaAbility = true
                timing = TimingRule.ManaAbility
            }
        }
        val cycler = card("Presence Test Cycler") {
            typeLine = "Creature — Beast"; power = 2; toughness = 2
            keywordAbility(KeywordAbility.cycling("{2}"))
        }
        val granter = card("Presence Test Granter") {
            typeLine = "Enchantment"
            staticAbility {
                ability = GrantActivatedAbility(
                    ability = ActivatedAbility(id = AbilityId.next(), cost = Costs.Tap, effect = Effects.GainLife(1)),
                    filter = GroupFilter.AllCreaturesYouControl,
                )
            }
        }
        listOf(pinger, dork, cycler, granter).forEach(cardRegistry::register)

        fun TestGame.has(id: EntityId, atLeast: Int = 1, excludeMana: Boolean = false) =
            services.predicateEvaluator.matchesStatePredicate(
                state, id, StatePredicate.HasActivatedAbilities(atLeast, excludeMana)
            )

        fun board(vararg battlefield: String) = scenario()
            .withPlayers("First", "Second")
            .apply { battlefield.forEach { withCardOnBattlefield(1, it) } }
            .withCardInGraveyard(1, cycler.name)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("printed abilities count; a vanilla creature has none") {
            val g = board("Grizzly Bears", pinger.name)
            g.has(g.findPermanent("Grizzly Bears")!!) shouldBe false
            val p = g.findPermanent(pinger.name)!!
            g.has(p) shouldBe true
            g.has(p, atLeast = 2) shouldBe false
        }

        test("a mana ability counts unless mana abilities are excluded") {
            val g = board(dork.name)
            val d = g.findPermanent(dork.name)!!
            g.has(d) shouldBe true
            g.has(d, excludeMana = true) shouldBe false
        }

        test("cycling counts on the battlefield and in the graveyard") {
            val g = board(cycler.name)
            g.has(g.findPermanent(cycler.name)!!, excludeMana = true) shouldBe true
            val inYard = g.state.getGraveyard(g.player1Id).single()
            g.has(inYard, excludeMana = true) shouldBe true
        }

        test("granted abilities count alongside printed ones") {
            val g = board(granter.name, "Grizzly Bears", pinger.name)
            val bears = g.findPermanent("Grizzly Bears")!!
            g.has(bears) shouldBe true
            g.has(bears, atLeast = 2) shouldBe false
            g.has(g.findPermanent(pinger.name)!!, atLeast = 2) shouldBe true
        }

        test("losing all abilities drops the printed ones; a static grant is still counted, as the enumerator offers it") {
            val g = board(granter.name, pinger.name)
            val p = g.findPermanent(pinger.name)!!
            val result = services.effectExecutorRegistry.execute(
                g.state,
                Effects.RemoveAllAbilities(EffectTarget.ContextTarget(0)),
                EffectContext(sourceId = null, controllerId = g.player1Id, targets = listOf(ChosenTarget.Permanent(p))),
            )
            result.error shouldBe null
            g.state = result.state

            g.has(p) shouldBe true
            g.has(p, atLeast = 2) shouldBe false
        }

        test("permanentSourcesOnly refuses abilities of creature cards off the battlefield") {
            val creatureAbilities = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
                CardType.CREATURE, allowSpells = false, allowAbilities = true,
            )
            val ofPermanents = creatureAbilities.copy(permanentSourcesOnly = true)
            val fromHand = SpellPaymentContext(
                isAbilityActivation = true,
                abilitySourceCardTypes = setOf(CardType.CREATURE),
                isAbilitySourceOnBattlefield = false,
            )
            val onBattlefield = fromHand.copy(isAbilitySourceOnBattlefield = true)

            creatureAbilities.isSatisfiedBy(fromHand) shouldBe true
            ofPermanents.isSatisfiedBy(fromHand) shouldBe false
            ofPermanents.isSatisfiedBy(onBattlefield) shouldBe true
            ofPermanents.isSatisfiedBy(SpellPaymentContext(isCreature = true, cardTypes = setOf(CardType.CREATURE))) shouldBe false
        }
    }
}
