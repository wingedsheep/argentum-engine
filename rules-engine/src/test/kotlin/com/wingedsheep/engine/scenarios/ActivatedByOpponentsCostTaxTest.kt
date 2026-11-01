package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ActivatedAbilityReach
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.IncreaseActivatedAbilityCost
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * [IncreaseActivatedAbilityCost] with the [ActivatedAbilityReach.ACTIVATED_BY_OPPONENTS] reach —
 * "abilities your opponents activate cost {1} more to activate unless they're mana abilities"
 * (Tithe Taker) — and the [ConditionalStaticAbility] gate that makes it "during your turn".
 *
 * The activator decides, not the source's zone or controller: an opponent's battlefield ability
 * and their cycling from hand are both taxed, the static's controller's own abilities are not, and
 * an opponent's mana ability is exempt (CR 605). A cost with no mana part gains one (Tithe
 * Taker's ruling: "{T}:" becomes "{1}, {T}:").
 */
class ActivatedByOpponentsCostTaxTest : ScenarioTestBase() {

    private val opponentsTax = IncreaseActivatedAbilityCost(
        filter = GroupFilter(GameObjectFilter.Any),
        amount = DynamicAmounts.fixed(1),
        excludeManaAbilities = true,
        reach = ActivatedAbilityReach.ACTIVATED_BY_OPPONENTS,
    )

    init {
        cardRegistry.register(card("Opponent Tax Totem") {
            manaCost = "{1}"
            typeLine = "Artifact"
            staticAbility { ability = opponentsTax }
        })
        cardRegistry.register(card("Your Turn Tax Totem") {
            manaCost = "{1}"
            typeLine = "Artifact"
            staticAbility { ability = ConditionalStaticAbility(opponentsTax, Conditions.IsYourTurn) }
        })
        cardRegistry.register(card("Tax Test Healer") {
            manaCost = "{1}"
            typeLine = "Creature — Cleric"
            power = 1
            toughness = 1
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.GainLife(1)
            }
        })
        cardRegistry.register(card("Tax Test Elf") {
            manaCost = "{G}"
            typeLine = "Creature — Elf"
            power = 1
            toughness = 1
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.AddMana(Color.GREEN)
                manaAbility = true
            }
        })
        cardRegistry.register(card("Tax Test Cycler") {
            manaCost = "{3}"
            typeLine = "Sorcery"
            spell { effect = Effects.GainLife(1) }
            keywordAbility(KeywordAbility.cycling("{1}"))
        })

        fun TestGame.isTapped(id: EntityId) = state.getEntity(id)?.has<TappedComponent>() == true

        fun TestGame.ability(player: Int, sourceId: EntityId) =
            getLegalActions(player).first { (it.action as? ActivateAbility)?.sourceId == sourceId }

        context("ACTIVATED_BY_OPPONENTS reach") {
            test("an opponent's bare {T} ability becomes {1}, {T} and is unaffordable without mana") {
                val game = scenario()
                    .withPlayers("Taxer", "Opponent")
                    .withCardOnBattlefield(1, "Opponent Tax Totem")
                    .withCardOnBattlefield(2, "Tax Test Healer")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val action = game.ability(2, game.findPermanent("Tax Test Healer")!!)
                action.description shouldBe "{1}, {T}: You gain 1 life"
                action.isAffordable shouldBe false
            }

            test("the static's controller's own abilities are untaxed") {
                val game = scenario()
                    .withPlayers("Taxer", "Opponent")
                    .withCardOnBattlefield(1, "Opponent Tax Totem")
                    .withCardOnBattlefield(1, "Tax Test Healer")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val action = game.ability(1, game.findPermanent("Tax Test Healer")!!)
                action.description shouldBe "{T}: You gain 1 life"
                action.isAffordable shouldBe true
            }

            test("an opponent's cycling from hand is taxed — the activator decides, not the zone") {
                val game = scenario()
                    .withPlayers("Taxer", "Opponent")
                    .withCardOnBattlefield(1, "Opponent Tax Totem")
                    .withCardInHand(2, "Tax Test Cycler")
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cycle = game.getLegalActions(2).first { it.action is CycleCard }
                withClue("cycling {1} plus the {1} tax is {2}; one Forest can't pay it") {
                    cycle.manaCostString shouldBe "{2}"
                    cycle.isAffordable shouldBe false
                }
            }

            test("an opponent's mana ability is exempt") {
                val game = scenario()
                    .withPlayers("Taxer", "Opponent")
                    .withCardOnBattlefield(1, "Opponent Tax Totem")
                    .withCardOnBattlefield(2, "Tax Test Elf")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elf = game.findPermanent("Tax Test Elf")!!
                game.execute(game.ability(2, elf).action).error shouldBe null
                game.resolveStack()
                game.isTapped(elf) shouldBe true
            }

            test("paying the tax: one land buys the taxed ability back and it resolves") {
                val game = scenario()
                    .withPlayers("Taxer", "Opponent")
                    .withCardOnBattlefield(1, "Opponent Tax Totem")
                    .withCardOnBattlefield(2, "Tax Test Healer")
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val action = game.ability(2, game.findPermanent("Tax Test Healer")!!)
                action.isAffordable shouldBe true
                game.execute(action.action).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.isTapped(game.findPermanent("Forest")!!) shouldBe true
                game.getLifeTotal(2) shouldBe 21
            }
        }

        context("ConditionalStaticAbility gate — \"during your turn\"") {
            test("on its controller's turn, an opponent activating at instant speed is taxed") {
                val game = scenario()
                    .withPlayers("Taxer", "Opponent")
                    .withCardOnBattlefield(1, "Your Turn Tax Totem")
                    .withCardOnBattlefield(2, "Tax Test Healer")
                    .withActivePlayer(1)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.ability(2, game.findPermanent("Tax Test Healer")!!).description shouldBe
                    "{1}, {T}: You gain 1 life"
            }

            test("on the opponent's own turn, the gate is off and the ability keeps its printed cost") {
                val game = scenario()
                    .withPlayers("Taxer", "Opponent")
                    .withCardOnBattlefield(1, "Your Turn Tax Totem")
                    .withCardOnBattlefield(2, "Tax Test Healer")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val action = game.ability(2, game.findPermanent("Tax Test Healer")!!)
                action.description shouldBe "{T}: You gain 1 life"
                action.isAffordable shouldBe true
            }
        }
    }
}
