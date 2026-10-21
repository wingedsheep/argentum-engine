package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import io.kotest.matchers.shouldBe

/**
 * `StatePredicate.HasManaAbility` off the battlefield: a card in a library or graveyard has its
 * printed abilities even though they don't function there, so "an artifact card with a mana
 * ability" (Moonsilver Key) reads the printed activated and triggered mana abilities and printed
 * basic land types. Moonsilver Key's rulings name both kinds: most artifacts' mana abilities are
 * activated, some (Gauntlet of Power) are triggered.
 */
class HasManaAbilityOffBattlefieldTest : ScenarioTestBase() {
    init {
        val rock = card("Presence Test Rock") {
            typeLine = "Artifact"
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.AddMana(Color.GREEN)
                manaAbility = true
                timing = TimingRule.ManaAbility
            }
        }
        val pinger = card("Presence Test Relic") {
            typeLine = "Artifact"
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.GainLife(1)
            }
        }
        val amplifier = card("Presence Test Amplifier") {
            typeLine = "Artifact"
            staticAbility {
                ability = AdditionalManaOnTap(color = Color.GREEN, amount = DynamicAmounts.fixed(1))
            }
        }
        val dual = card("Presence Test Dual") {
            typeLine = "Land — Forest Island"
        }
        val wasteland = card("Presence Test Barren Land") {
            typeLine = "Land"
        }
        listOf(rock, pinger, amplifier, dual, wasteland).forEach(cardRegistry::register)

        fun TestGame.has(id: EntityId) =
            services.predicateEvaluator.matchesStatePredicate(state, id, StatePredicate.HasManaAbility)

        fun TestGame.library(name: String) = findCardsInLibrary(1, name).single()

        fun board() = scenario()
            .withPlayers("First", "Second")
            .withCardInLibrary(1, rock.name)
            .withCardInLibrary(1, pinger.name)
            .withCardInLibrary(1, amplifier.name)
            .withCardInLibrary(1, dual.name)
            .withCardInLibrary(1, wasteland.name)
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInGraveyard(1, rock.name)
            .withCardOnBattlefield(1, rock.name)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("a printed activated mana ability counts in the library and the graveyard") {
            val g = board()
            g.has(g.library(rock.name)) shouldBe true
            g.has(g.state.getGraveyard(g.player1Id).single()) shouldBe true
        }

        test("a printed triggered mana ability counts in the library") {
            val g = board()
            g.has(g.library(amplifier.name)) shouldBe true
        }

        test("an activated ability that isn't a mana ability doesn't count; neither does a vanilla card") {
            val g = board()
            g.has(g.library(pinger.name)) shouldBe false
            g.has(g.library("Grizzly Bears")) shouldBe false
        }

        test("printed basic land types give a land card its intrinsic mana abilities") {
            val g = board()
            g.has(g.library("Forest")) shouldBe true
            g.has(g.library(dual.name)) shouldBe true
            g.has(g.library(wasteland.name)) shouldBe false
        }

        test("a face-down card off the battlefield has no abilities; a phased-out permanent doesn't exist") {
            val g = board()
            val forest = g.library("Forest")
            val rockInLibrary = g.library(rock.name)
            val onField = g.findPermanent(rock.name)!!
            g.state = g.state
                .updateEntity(forest) { it.with(FaceDownComponent) }
                .updateEntity(rockInLibrary) { it.with(FaceDownComponent) }
                .updateEntity(onField) { it.with(PhasedOutComponent(g.player1Id)) }
            g.has(forest) shouldBe false
            g.has(rockInLibrary) shouldBe false
            g.has(onField) shouldBe false
        }

        test("the battlefield reading is unchanged") {
            val g = board()
            g.has(g.findPermanent(rock.name)!!) shouldBe true
        }

        test("Moonsilver Key's filter: an artifact card with a mana ability or a basic land card") {
            val g = board()
            val filter = GameObjectFilter.Artifact.withManaAbility() or GameObjectFilter.BasicLand
            fun matches(id: EntityId) = services.predicateEvaluator.matches(
                g.state, g.state.projectedState, id, filter,
                PredicateContext(controllerId = g.player1Id),
            )
            matches(g.library(rock.name)) shouldBe true
            matches(g.library(amplifier.name)) shouldBe true
            matches(g.library("Forest")) shouldBe true
            matches(g.library(pinger.name)) shouldBe false
            matches(g.library(dual.name)) shouldBe false
            matches(g.library("Grizzly Bears")) shouldBe false
        }
    }
}
