package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
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
import com.wingedsheep.sdk.scripting.ActivatedAbilityReach
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.IncreaseActivatedAbilityCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith

/**
 * `IncreaseActivatedAbilityCost(targeting = …)` — "Abilities your opponents activate that target a
 * Merfolk you control cost {2} more to activate" (Kopala, Warden of Waves).
 *
 * Targets are chosen before the total cost is determined (CR 601.2c, then 601.2f; CR 602.2b applies
 * that order to activated abilities), so the tax is priced against the chosen targets. Pinned here:
 *  - an opponent's ability targeting a matching permanent pays {2} more;
 *  - the enumerator runs before targets exist and offers the ability untaxed;
 *  - targeting something else, or the static's controller activating, is untaxed;
 *  - targeting two matching permanents is taxed once (Kopala's ruling);
 *  - a `{T}:` ability with no mana in its cost gains the {2}.
 */
class TargetedActivatedAbilityCostIncreaseScenarioTest : ScenarioTestBase() {

    private val warden = card("Tax Test Warden") {
        manaCost = "{1}{U}{U}"
        typeLine = "Creature — Merfolk Wizard"
        power = 2
        toughness = 2
        oracleText = "Abilities your opponents activate that target a Merfolk you control cost {2} more to activate."
        staticAbility {
            ability = IncreaseActivatedAbilityCost(
                filter = GroupFilter(GameObjectFilter.Any),
                amount = DynamicAmount.Fixed(2),
                reach = ActivatedAbilityReach.ACTIVATED_BY_OPPONENTS,
                targeting = GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.MERFOLK).youControl()),
            )
        }
    }

    private val merfolk = card("Tax Test Merfolk") {
        manaCost = "{U}"
        typeLine = "Creature — Merfolk"
        power = 1
        toughness = 1
    }

    private val rod = card("Tax Test Rod") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "{1}: Target creature gets -1/-0 until end of turn.\n" +
            "{1}: Two target creatures get -1/-0 until end of turn.\n" +
            "{T}: Target creature gets -1/-0 until end of turn."
        activatedAbility {
            val creature = target(TargetFilter.Creature)
            cost = Costs.Mana("{1}")
            effect = Effects.ModifyStats(-1, 0, creature)
        }
        activatedAbility {
            val (first, second) = targets(TargetFilter.Creature, count = 2)
            cost = Costs.Mana("{1}")
            effect = Effects.ModifyStats(-1, 0, first).then(Effects.ModifyStats(-1, 0, second))
        }
        activatedAbility {
            val creature = target(TargetFilter.Creature)
            cost = Costs.Tap
            effect = Effects.ModifyStats(-1, 0, creature)
        }
    }

    private fun abilityId(index: Int): AbilityId =
        cardRegistry.getCard("Tax Test Rod")!!.script.activatedAbilities[index].id

    private fun TestGame.activate(playerId: EntityId, sourceId: EntityId, index: Int, targets: List<EntityId>) =
        execute(
            ActivateAbility(
                playerId = playerId,
                sourceId = sourceId,
                abilityId = abilityId(index),
                targets = targets.map { ChosenTarget.Permanent(it) }
            )
        ).also { if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay() }

    private fun TestGame.untappedLands(playerNumber: Int): Int {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getBattlefield().count { id ->
            state.getEntity(id)?.let { e ->
                state.projectedState.getController(id) == playerId &&
                    e.get<CardComponent>()?.name == "Island" && e.get<TappedComponent>() == null
            } == true
        }
    }

    /** Player 2 (active) holds the Rod; player 1 holds the Warden and its Merfolk. */
    private fun board(rodController: Int = 2, lands: Int = 1) = scenario()
        .withPlayers("Warden", "Opponent")
        .withCardOnBattlefield(1, "Tax Test Warden")
        .withCardOnBattlefield(1, "Tax Test Merfolk")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(rodController, "Tax Test Rod")
        .withLandsOnBattlefield(rodController, "Island", lands)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(rodController)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        cardRegistry.register(warden)
        cardRegistry.register(merfolk)
        cardRegistry.register(rod)

        test("an opponent's ability targeting a Merfolk costs {2} more; enumeration offers it untaxed") {
            val game = board(lands = 3)
            val rodId = game.findPermanent("Tax Test Rod")!!
            val merfolkId = game.findPermanent("Tax Test Merfolk")!!

            val offered = game.getLegalActions(2).firstOrNull {
                it.action.let { a -> a is ActivateAbility && a.abilityId == abilityId(0) }
            }
            withClue("targets aren't chosen at enumeration, so the printed {1} is offered") {
                offered shouldNotBe null
                offered!!.description shouldStartWith "{1}:"
            }

            game.activate(game.player2Id, rodId, 0, listOf(merfolkId)).error shouldBe null
            withClue("{1} + {2} tax taps all three Islands") { game.untappedLands(2) shouldBe 0 }
        }

        test("one Island can't pay the taxed cost") {
            val game = board(lands = 1)
            val rodId = game.findPermanent("Tax Test Rod")!!
            val merfolkId = game.findPermanent("Tax Test Merfolk")!!
            game.activate(game.player2Id, rodId, 0, listOf(merfolkId)).error shouldNotBe null
        }

        test("targeting a non-Merfolk is untaxed") {
            val game = board(lands = 1)
            val rodId = game.findPermanent("Tax Test Rod")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.activate(game.player2Id, rodId, 0, listOf(bears)).error shouldBe null
        }

        test("the Warden's controller targeting their own Merfolk is untaxed") {
            val game = board(rodController = 1, lands = 1)
            val rodId = game.findPermanent("Tax Test Rod")!!
            val merfolkId = game.findPermanent("Tax Test Merfolk")!!
            game.activate(game.player1Id, rodId, 0, listOf(merfolkId)).error shouldBe null
        }

        test("targeting two Merfolk is taxed only once") {
            val game = board(lands = 4)
            val rodId = game.findPermanent("Tax Test Rod")!!
            val merfolkId = game.findPermanent("Tax Test Merfolk")!!
            val wardenId = game.findPermanent("Tax Test Warden")!!
            game.activate(game.player2Id, rodId, 1, listOf(merfolkId, wardenId)).error shouldBe null
            withClue("{1} + one {2} tax, not two") { game.untappedLands(2) shouldBe 1 }
        }

        test("a {T}: ability targeting a Merfolk gains a {2} mana cost") {
            val noMana = board(lands = 0)
            noMana.activate(noMana.player2Id, noMana.findPermanent("Tax Test Rod")!!, 2, listOf(
                noMana.findPermanent("Tax Test Merfolk")!!)).error shouldNotBe null

            val game = board(lands = 2)
            game.activate(game.player2Id, game.findPermanent("Tax Test Rod")!!, 2, listOf(
                game.findPermanent("Tax Test Merfolk")!!)).error shouldBe null
            game.untappedLands(2) shouldBe 0
        }
    }
}
