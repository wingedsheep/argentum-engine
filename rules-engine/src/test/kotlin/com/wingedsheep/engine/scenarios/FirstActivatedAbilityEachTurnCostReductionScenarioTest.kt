package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.player.PermanentAbilitiesActivatedThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceActivatedAbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith

/**
 * `ReduceActivatedAbilityCost(firstEachTurn = true)` — "The first activated ability of an artifact
 * you activate each turn costs {2} less to activate" (Tezzeret, Betrayer of Flesh).
 *
 * Pinned down here:
 *  - the first artifact ability is offered and paid reduced; the second costs full price;
 *  - an artifact mana ability activated first uses the discount up;
 *  - an artifact tapped for mana by auto-pay *while paying* the first ability doesn't — the total
 *    cost was locked in before mana abilities were activated (CR 601.2f–h via CR 602.2b);
 *  - a non-artifact's ability is neither reduced nor counted;
 *  - an opponent's artifact abilities are never reduced ("you activate");
 *  - the per-turn memory resets at the next turn.
 */
class FirstActivatedAbilityEachTurnCostReductionScenarioTest : ScenarioTestBase() {

    private val tinkerer = card("First Test Tinkerer") {
        manaCost = "{2}{U}"
        typeLine = "Creature — Human Artificer"
        power = 1
        toughness = 1
        oracleText = "The first activated ability of an artifact you activate each turn costs {2} less to activate."
        staticAbility {
            ability = ReduceActivatedAbilityCost(
                filter = GroupFilter(GameObjectFilter.Artifact),
                amount = DynamicAmount.Fixed(2),
                firstEachTurn = true
            )
        }
    }

    private val engine = card("First Test Engine") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "{3}: You gain 1 life."
        activatedAbility {
            cost = Costs.Mana("{3}")
            effect = Effects.GainLife(1)
        }
    }

    private val rock = card("First Test Rock") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "{T}: Add {C}."
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddColorlessMana(1)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }

    private val idol = card("First Test Idol") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Elf"
        power = 1
        toughness = 1
        oracleText = "{1}: You gain 1 life."
        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.GainLife(1)
        }
    }

    private fun abilityId(name: String): AbilityId =
        cardRegistry.getCard(name)!!.script.activatedAbilities[0].id

    private fun TestGame.offered(playerNumber: Int, abilityId: AbilityId) =
        getLegalActions(playerNumber).firstOrNull {
            it.action.let { a -> a is ActivateAbility && a.abilityId == abilityId }
        }

    private fun TestGame.activate(playerId: EntityId, sourceId: EntityId, abilityId: AbilityId) =
        execute(ActivateAbility(playerId = playerId, sourceId = sourceId, abilityId = abilityId))
            .also { if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay() }

    private fun board(tinkererController: Int = 1, islands: Int = 3, withRock: Boolean = false) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(tinkererController, "First Test Tinkerer")
        .withCardOnBattlefield(1, "First Test Engine")
        .withCardOnBattlefield(1, "First Test Idol")
        .apply { if (withRock) withCardOnBattlefield(1, "First Test Rock") }
        .withLandsOnBattlefield(1, "Island", islands)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        cardRegistry.register(tinkerer)
        cardRegistry.register(engine)
        cardRegistry.register(rock)
        cardRegistry.register(idol)

        test("the first artifact ability costs {2} less; the second costs full price") {
            val game = board(islands = 4)
            val engineId = game.findPermanent("First Test Engine")!!

            withClue("offered at the reduced {1}") {
                game.offered(1, abilityId("First Test Engine"))!!.description shouldStartWith "{1}:"
            }
            game.activate(game.player1Id, engineId, abilityId("First Test Engine")).error shouldBe null
            game.resolveStack()

            withClue("the discount is spent: offered at {3}, and three Islands remain to pay it") {
                game.offered(1, abilityId("First Test Engine"))!!.description shouldStartWith "{3}:"
            }
            game.activate(game.player1Id, engineId, abilityId("First Test Engine")).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 22
        }

        test("a non-artifact's ability is neither reduced nor counted as the first") {
            val game = board(islands = 2)
            val idolId = game.findPermanent("First Test Idol")!!
            val engineId = game.findPermanent("First Test Engine")!!

            game.offered(1, abilityId("First Test Idol"))!!.description shouldStartWith "{1}:"
            game.activate(game.player1Id, idolId, abilityId("First Test Idol")).error shouldBe null
            game.resolveStack()

            withClue("the remaining Island still pays the engine's reduced {1}") {
                game.activate(game.player1Id, engineId, abilityId("First Test Engine")).error shouldBe null
            }
        }

        test("an artifact mana ability activated first uses the discount up") {
            val game = board(islands = 1, withRock = true)
            val rockId = game.findPermanent("First Test Rock")!!
            val engineId = game.findPermanent("First Test Engine")!!

            game.activate(game.player1Id, rockId, abilityId("First Test Rock")).error shouldBe null

            withClue("the rock's activation was the first, so the engine now costs {3}") {
                game.offered(1, abilityId("First Test Engine"))!!.description shouldStartWith "{3}:"
                game.activate(game.player1Id, engineId, abilityId("First Test Engine")).error shouldNotBe null
            }
        }

        test("an artifact tapped by auto-pay for the first ability doesn't take its discount away") {
            val game = board(islands = 0, withRock = true)
            val engineId = game.findPermanent("First Test Engine")!!

            withClue("the rock pays the locked-in {1}") {
                game.activate(game.player1Id, engineId, abilityId("First Test Engine")).error shouldBe null
            }
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 21
        }

        test("an opponent's static never reduces your activations") {
            val game = board(tinkererController = 2, islands = 1)
            val engineId = game.findPermanent("First Test Engine")!!

            game.offered(1, abilityId("First Test Engine"))!!.description shouldStartWith "{3}:"
            game.activate(game.player1Id, engineId, abilityId("First Test Engine")).error shouldNotBe null
        }

        test("the memory of activated permanents resets at the next turn") {
            val game = board(islands = 1)
            val engineId = game.findPermanent("First Test Engine")!!

            game.activate(game.player1Id, engineId, abilityId("First Test Engine")).error shouldBe null
            game.resolveStack()
            withClue("the engine and the Island auto-pay tapped for it are both remembered") {
                game.state.getEntity(game.player1Id)?.get<PermanentAbilitiesActivatedThisTurnComponent>()!!
                    .sources.map { it.entityId }.toSet() shouldBe setOf(engineId, game.findPermanent("Island")!!)
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            withClue("now the opponent's turn") { game.state.activePlayerId shouldBe game.player2Id }
            game.state.getEntity(game.player1Id)?.get<PermanentAbilitiesActivatedThisTurnComponent>() shouldBe null
        }
    }
}
