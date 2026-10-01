package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceActivatedAbilityCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith

/**
 * `ReduceActivatedAbilityCost(onlyIfTargetIsSource = true)` — "Activated abilities of Equipment you
 * control that target this creature cost {1} less to activate" (Bladegraft Aspirant).
 *
 * Targets are chosen before the total cost is determined (CR 601.2c, then 601.2f; CR 602.2b applies
 * that order to activated abilities), so the handler prices the activation against the chosen
 * target: the reduction applies only when one of them is the static's source. The enumerator runs
 * before targets are chosen and prices a targeted ability optimistically. Pinned down here:
 *  - targeting the source: reduced, offered and paid at the reduced cost;
 *  - targeting another creature: full price (rejected when only the reduced cost is affordable);
 *  - an untargeted ability of a matching Equipment: never reduced, also at enumeration;
 *  - a non-Equipment source targeting the creature: the `filter` still gates it;
 *  - "Equipment **you** control" reads the static's controller, so an opponent's Equipment
 *    targeting the creature is not discounted.
 */
class TargetedActivatedAbilityCostReductionScenarioTest : ScenarioTestBase() {

    private val aspirant = card("Graft Test Aspirant") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Phyrexian Warrior"
        power = 2
        toughness = 3
        oracleText = "Activated abilities of Equipment you control that target this creature cost {1} less to activate."
        staticAbility {
            ability = ReduceActivatedAbilityCost(
                filter = GroupFilter(GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).youControl()),
                amount = DynamicAmount.Fixed(1),
                onlyIfTargetIsSource = true
            )
        }
    }

    private val blade = card("Graft Test Blade") {
        manaCost = "{1}"
        typeLine = "Artifact — Equipment"
        oracleText = "{2}: Target creature gets +1/+0 until end of turn.\n{2}: You gain 1 life."
        activatedAbility {
            val creature = target(TargetFilter.Creature)
            cost = Costs.Mana("{2}")
            effect = Effects.ModifyStats(1, 0, creature)
        }
        activatedAbility {
            cost = Costs.Mana("{2}")
            effect = Effects.GainLife(1)
        }
    }

    private val rod = card("Graft Test Rod") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "{2}: Target creature gets +1/+0 until end of turn."
        activatedAbility {
            val creature = target(TargetFilter.Creature)
            cost = Costs.Mana("{2}")
            effect = Effects.ModifyStats(1, 0, creature)
        }
    }

    private fun abilityId(name: String, index: Int): AbilityId =
        cardRegistry.getCard(name)!!.script.activatedAbilities[index].id

    private fun TestGame.actionFor(playerNumber: Int, abilityId: AbilityId) =
        getLegalActions(playerNumber).firstOrNull {
            it.action.let { a -> a is ActivateAbility && a.abilityId == abilityId }
        }

    private fun TestGame.activate(playerId: EntityId, sourceId: EntityId, abilityId: AbilityId, target: EntityId?) =
        execute(
            ActivateAbility(
                playerId = playerId,
                sourceId = sourceId,
                abilityId = abilityId,
                targets = listOfNotNull(target?.let { ChosenTarget.Permanent(it) })
            )
        ).also { if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay() }

    private fun board(
        aspirantController: Int = 1,
        sourceName: String = "Graft Test Blade",
        lands: Int = 1
    ) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(aspirantController, "Graft Test Aspirant")
        .withCardOnBattlefield(1, sourceName)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Mountain", lands)
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        cardRegistry.register(aspirant)
        cardRegistry.register(blade)
        cardRegistry.register(rod)

        test("an Equipment ability targeting the creature costs {1} less — offered and paid reduced") {
            val game = board()
            val aspirantId = game.findPermanent("Graft Test Aspirant")!!
            val bladeId = game.findPermanent("Graft Test Blade")!!

            val offered = game.actionFor(1, abilityId("Graft Test Blade", 0))
            withClue("targets aren't chosen at enumeration, so the targeted ability is priced optimistically") {
                offered shouldNotBe null
                offered!!.description shouldStartWith "{1}:"
                offered.isAffordable shouldBe true
            }

            val result = game.activate(game.player1Id, bladeId, abilityId("Graft Test Blade", 0), aspirantId)
            withClue("one Mountain pays the reduced {1}: ${result.error}") { result.error shouldBe null }
            game.resolveStack()
            game.state.projectedState.getPower(aspirantId) shouldBe 3
        }

        test("the same ability targeting another creature costs full price") {
            val game = board()
            val bladeId = game.findPermanent("Graft Test Blade")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            withClue("one Mountain cannot pay the unreduced {2}") {
                game.activate(game.player1Id, bladeId, abilityId("Graft Test Blade", 0), bears).error shouldNotBe null
            }
        }

        test("targeting another creature with full mana still works at {2}") {
            val game = board(lands = 2)
            val bladeId = game.findPermanent("Graft Test Blade")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.activate(game.player1Id, bladeId, abilityId("Graft Test Blade", 0), bears).error shouldBe null
            game.resolveStack()
            game.state.projectedState.getPower(bears) shouldBe 3
        }

        test("an untargeted ability of the Equipment is never reduced") {
            val game = board()
            val bladeId = game.findPermanent("Graft Test Blade")!!

            val offered = game.actionFor(1, abilityId("Graft Test Blade", 1))
            withClue("no targets, so no optimistic discount at enumeration either") {
                offered shouldNotBe null
                offered!!.description shouldStartWith "{2}:"
                offered.isAffordable shouldBe false
            }
            game.activate(game.player1Id, bladeId, abilityId("Graft Test Blade", 1), null).error shouldNotBe null
        }

        test("a non-Equipment artifact targeting the creature is not reduced") {
            val game = board(sourceName = "Graft Test Rod")
            val aspirantId = game.findPermanent("Graft Test Aspirant")!!
            val rodId = game.findPermanent("Graft Test Rod")!!

            game.activate(game.player1Id, rodId, abilityId("Graft Test Rod", 0), aspirantId).error shouldNotBe null
        }

        test("an opponent's Equipment targeting the creature is not reduced — 'Equipment you control'") {
            val game = board(aspirantController = 2)
            val aspirantId = game.findPermanent("Graft Test Aspirant")!!
            val bladeId = game.findPermanent("Graft Test Blade")!!

            game.activate(game.player1Id, bladeId, abilityId("Graft Test Blade", 0), aspirantId).error shouldNotBe null
        }
    }
}
