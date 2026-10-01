package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.event.GrantedActivatedAbility
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.matchers.shouldBe

class ManaAbilityPresenceTest : ScenarioTestBase() {
    init {
        val mana = ActivatedAbility(AbilityId("presence_mana"), AbilityCost.Tap, Effects.AddColorlessMana(1), isManaAbility = true)
        cardRegistry.register(card("Presence mana land") { typeLine = "Land"; activatedAbility { cost = AbilityCost.Tap; manaAbility = true; effect = Effects.AddColorlessMana(1) } })
        cardRegistry.register(card("Presence plain land") { typeLine = "Land" })
        cardRegistry.register(card("Presence inert mana land") { typeLine = "Land"; activatedAbility { cost = AbilityCost.Tap; manaAbility = true; effect = Effects.AddDynamicMana(DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).count(), setOf(Color.GREEN)) } })
        cardRegistry.register(card("Presence mana artifact") { typeLine = "Artifact"; activatedAbility { cost = AbilityCost.Tap; manaAbility = true; effect = Effects.AddColorlessMana(1) } })
        cardRegistry.register(card("Presence grant") { typeLine = "Enchantment"; staticAbility { ability = GrantActivatedAbility(mana, GroupFilter(GameObjectFilter.Land)) } })
        cardRegistry.register(card("Presence recursive grant") { typeLine = "Enchantment"; staticAbility { ability = GrantActivatedAbility(mana, GroupFilter(GameObjectFilter.Land.withManaAbility())) } })
        cardRegistry.register(card("Presence triggered mana land") { typeLine = "Land"; staticAbility { ability = AdditionalManaOnSourceTap(GameObjectFilter.Land, Color.GREEN) } })
        cardRegistry.register(card("Presence hand ability") { typeLine = "Land"; activatedAbility { cost = AbilityCost.Tap; manaAbility = true; activateFromZone = Zone.HAND; effect = Effects.AddColorlessMana(1) } })

        fun matches(game: TestGame, name: String, filter: GameObjectFilter = GameObjectFilter.Permanent.withManaAbility()): Boolean {
            return PredicateEvaluator(cardRegistry).matches(game.state, game.state.projectedState, game.findPermanent(name)!!, filter, PredicateContext(controllerId = game.player1Id))
        }

        test("counts intrinsic basic land and printed colorless mana but not a blank land") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Forest")
                .withCardOnBattlefield(1, "Presence mana land").withCardOnBattlefield(1, "Presence plain land").build()
            matches(game, "Forest") shouldBe true
            matches(game, "Presence mana land") shouldBe true
            matches(game, "Presence plain land") shouldBe false
        }
        test("presence ignores tapped state and whether a mana ability produces zero") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Presence inert mana land", tapped = true).build()
            matches(game, "Presence inert mana land") shouldBe true
        }
        test("a triggered mana ability counts even without any activated mana ability") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Presence triggered mana land").build()
            matches(game, "Presence triggered mana land") shouldBe true
        }
        test("phasing out a granter removes its mana ability grant") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Presence plain land").withCardOnBattlefield(1, "Presence grant").build()
            matches(game, "Presence plain land") shouldBe true
            game.state = game.state.updateEntity(game.findPermanent("Presence grant")!!) { it.with(PhasedOutComponent(game.player1Id)) }
            matches(game, "Presence plain land") shouldBe false
        }
        test("is not restricted to lands and composes with the land type requirement") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Presence mana artifact").build()
            matches(game, "Presence mana artifact") shouldBe true
            matches(game, "Presence mana artifact", GameObjectFilter.Land.withManaAbility()) shouldBe false
        }
        test("static grant gives a blank land a mana ability") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Presence plain land").withCardOnBattlefield(1, "Presence grant").build()
            matches(game, "Presence plain land") shouldBe true
        }
        test("self-referential grant does not create a mana ability from nothing or recurse") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Presence plain land").withCardOnBattlefield(1, "Presence recursive grant").build()
            matches(game, "Presence plain land") shouldBe false
        }
        test("face-down suppresses printed mana but retains a runtime mana grant") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Presence mana land").build()
            val id = game.findPermanent("Presence mana land")!!
            game.state = game.state.updateEntity(id) { it.with(FaceDownComponent) }
            matches(game, "Presence mana land") shouldBe false
            game.state = game.state.copy(grantedActivatedAbilities = listOf(GrantedActivatedAbility(id, mana, Duration.EndOfTurn)))
            matches(game, "Presence mana land") shouldBe true
        }
        test("removed abilities use supplied projection and later grants still count") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Presence mana land").build()
            val id = game.findPermanent("Presence mana land")!!
            val projection = ProjectedState(game.state, mapOf(id to game.state.projectedState.getProjectedValues(id)!!.copy(lostAllAbilities = true)))
            val evaluator = PredicateEvaluator(cardRegistry)
            evaluator.matches(game.state, projection, id, GameObjectFilter.Permanent.withManaAbility(), PredicateContext(controllerId = game.player1Id)) shouldBe false
            game.state = game.state.copy(grantedActivatedAbilities = listOf(GrantedActivatedAbility(id, mana, Duration.EndOfTurn)))
            evaluator.matches(game.state, projection, id, GameObjectFilter.Permanent.withManaAbility(), PredicateContext(controllerId = game.player1Id)) shouldBe true
        }
        test("intrinsic mana follows supplied projected land types rather than printed characteristics") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Presence mana artifact").build()
            val id = game.findPermanent("Presence mana artifact")!!
            val values = game.state.projectedState.getProjectedValues(id)!!
            val projection = ProjectedState(game.state, mapOf(id to values.copy(types = setOf("LAND"), subtypes = setOf("Forest"), lostAllAbilities = true, basicLandTypesSetByEffect = true)))
            PredicateEvaluator(cardRegistry).matches(game.state, projection, id, GameObjectFilter.Land.withManaAbility(), PredicateContext(controllerId = game.player1Id)) shouldBe true
        }
        test("ability removal suppresses a basic land's intrinsic ability without erasing its subtype") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Forest").build()
            val id = game.findPermanent("Forest")!!
            val projection = ProjectedState(game.state, mapOf(id to game.state.projectedState.getProjectedValues(id)!!.copy(lostAllAbilities = true)))
            PredicateEvaluator(cardRegistry).matches(game.state, projection, id, GameObjectFilter.Land.withManaAbility(), PredicateContext(controllerId = game.player1Id)) shouldBe false
        }
        test("phased-out permanents and hand-only mana abilities do not match") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Presence mana land").withCardOnBattlefield(1, "Presence hand ability").build()
            val id = game.findPermanent("Presence mana land")!!
            game.state = game.state.updateEntity(id) { it.with(PhasedOutComponent(game.player1Id)) }
            PredicateEvaluator(cardRegistry).matches(game.state, game.state.projectedState, id, GameObjectFilter.Permanent.withManaAbility(), PredicateContext(controllerId = game.player1Id)) shouldBe false
            matches(game, "Presence hand ability") shouldBe false
        }
    }
}
