package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayPlayCardsFromExile
import com.wingedsheep.sdk.scripting.effects.ManaSpentAsAnyColor
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "You may spend mana from snow sources as though it were mana of any color to cast those spells"
 * — [SpellPaymentContext.spendAsAnyColor] = [ManaSpentAsAnyColor.SNOW] (Draugr Necromancer).
 *
 * CR 609.4b: spending mana "as though it were mana of any color" changes only how the cost may be
 * paid; the cost and the mana actually spent are unchanged. CR 107.4h: mana produced by a snow
 * source can be of any color or colorless, so both stand in. Only snow mana is widened — mana from
 * a non-snow source still has to match its pip — and `{C}` is not a color, so colored snow mana
 * still can't pay it.
 */
class SnowManaAsAnyColorTest : ScenarioTestBase() {
    init {
        val snowAsAny = SpellPaymentContext(spendAsAnyColor = ManaSpentAsAnyColor.SNOW)
        fun cost(value: String) = ManaCost.parse(value)

        // --- The floating pool --------------------------------------------------------------

        test("floating snow mana of another color pays a colored pip only under the permission") {
            val snowGreen = ManaPool(green = 1, snowMana = mapOf(Color.GREEN to 1))
            snowGreen.canPay(cost("{B}"), snowAsAny) shouldBe true
            snowGreen.canPay(cost("{B}")) shouldBe false
            snowGreen.pay(cost("{B}"), snowAsAny)?.green shouldBe 0
        }

        test("non-snow mana is not widened") {
            ManaPool(green = 1).canPay(cost("{B}"), snowAsAny) shouldBe false
            // One snow unit and one plain unit of the same color: only one can stand in.
            val mixed = ManaPool(green = 2, snowMana = mapOf(Color.GREEN to 1))
            mixed.canPay(cost("{B}{B}"), snowAsAny) shouldBe false
            mixed.canPay(cost("{B}{G}"), snowAsAny) shouldBe true
        }

        test("colorless snow mana pays a colored pip") {
            ManaPool(colorless = 1, snowColorless = 1).canPay(cost("{W}"), snowAsAny) shouldBe true
            ManaPool(colorless = 1).canPay(cost("{W}"), snowAsAny) shouldBe false
        }

        test("colored snow mana still can't pay {C}") {
            ManaPool(green = 1, snowMana = mapOf(Color.GREEN to 1)).canPay(cost("{C}"), snowAsAny) shouldBe false
        }

        test("hybrid and Phyrexian colored halves accept snow mana") {
            for (symbol in listOf("{W/U}", "{B/P}", "{2/R}")) {
                ManaPool(green = 1, snowMana = mapOf(Color.GREEN to 1)).pay(cost(symbol), snowAsAny)?.green shouldBe 0
            }
        }

        test("CR 609.4b — the mana actually spent keeps its color") {
            val result = ManaPool(green = 1, snowMana = mapOf(Color.GREEN to 1)).payPartial(cost("{B}"), snowAsAny)
            result.remainingCost.isEmpty() shouldBe true
            result.manaSpent.green shouldBe 1
            result.manaSpent.black shouldBe 0
        }

        // --- The auto-tap solver ------------------------------------------------------------

        test("solver taps a snow land of another color for a colored pip only under the permission") {
            val game = scenario().withPlayers().withLandsOnBattlefield(1, "Snow-Covered Forest", 2)
                .withActivePlayer(1).build()
            services.manaSolver.canPay(game.state, game.player1Id, cost("{B}{B}"), spellContext = snowAsAny) shouldBe true
            services.manaSolver.canPay(game.state, game.player1Id, cost("{B}{B}")) shouldBe false
            val solution = services.manaSolver.solve(game.state, game.player1Id, cost("{B}{B}"), spellContext = snowAsAny)!!
            solution.sources.size shouldBe 2
            solution.manaProduced.values.all { it.color == Color.GREEN } shouldBe true
        }

        test("solver won't tap a non-snow land of another color") {
            val game = scenario().withPlayers().withLandsOnBattlefield(1, "Forest", 2).withActivePlayer(1).build()
            services.manaSolver.canPay(game.state, game.player1Id, cost("{B}"), spellContext = snowAsAny) shouldBe false
        }

        test("solver taps snow Wastes (colorless snow) for a colored pip") {
            val game = scenario().withPlayers().withLandsOnBattlefield(1, "Snow-Covered Wastes", 1)
                .withActivePlayer(1).build()
            services.manaSolver.canPay(game.state, game.player1Id, cost("{R}"), spellContext = snowAsAny) shouldBe true
        }

        test("solver uses the native color first and snow only for the missing one") {
            val game = scenario().withPlayers().withLandsOnBattlefield(1, "Swamp", 1)
                .withLandsOnBattlefield(1, "Snow-Covered Forest", 1).withActivePlayer(1).build()
            val swamp = game.findPermanent("Swamp")!!
            val solution = services.manaSolver.solve(game.state, game.player1Id, cost("{B}{R}"), spellContext = snowAsAny)!!
            solution.manaProduced[swamp]?.color shouldBe Color.BLACK
            solution.sources.size shouldBe 2
            services.manaSolver.canPay(game.state, game.player1Id, cost("{R}{R}"), spellContext = snowAsAny) shouldBe false
        }

        test("floating snow mana counts toward colored pips in the solver's affordability check") {
            val game = scenario().withPlayers().withActivePlayer(1).build()
            game.state = game.state.updateEntity(game.player1Id) {
                it.with(ManaPoolComponent(green = 1, snowMana = mapOf(Color.GREEN to 1)))
            }
            services.manaSolver.canPay(game.state, game.player1Id, cost("{U}"), spellContext = snowAsAny) shouldBe true
            services.manaSolver.canPay(game.state, game.player1Id, cost("{U}")) shouldBe false
        }

        test("floating snow mana covers one pip and a land the rest — a partial floating payment stays snow-aware") {
            val game = scenario().withPlayers().withLandsOnBattlefield(1, "Swamp", 1).withActivePlayer(1).build()
            game.state = game.state.updateEntity(game.player1Id) {
                it.with(ManaPoolComponent(green = 1, snowMana = mapOf(Color.GREEN to 1)))
            }
            services.manaSolver.canPay(game.state, game.player1Id, cost("{B}{B}"), spellContext = snowAsAny) shouldBe true
            services.manaSolver.canPay(game.state, game.player1Id, cost("{B}{B}")) shouldBe false
            val partial = ManaPool(green = 1, snowMana = mapOf(Color.GREEN to 1)).payPartial(cost("{1}{B}{B}"), snowAsAny)
            partial.manaSpent.green shouldBe 1
            partial.remainingCost.genericAmount shouldBe 1
            partial.remainingCost.symbols.count { it == com.wingedsheep.sdk.core.ManaSymbol.Colored(Color.BLACK) } shouldBe 1
        }

        // --- Through a static exile-cast permission -----------------------------------------

        cardRegistry.register(card("Test Ice Warden") {
            manaCost = "{0}"; typeLine = "Artifact"
            staticAbility {
                ability = MayPlayCardsFromExile(
                    filter = GameObjectFilter.Nonland.ownedByOpponent().withCounter(CounterType.ICE),
                    spendAsAnyColor = ManaSpentAsAnyColor.SNOW,
                )
            }
        })
        cardRegistry.register(card("Test Blue Drake") {
            manaCost = "{1}{U}"; typeLine = "Creature — Drake"; power = 2; toughness = 2
        })
        cardRegistry.register(card("Test Blue Insight") {
            manaCost = "{U}"; typeLine = "Instant"
        })

        fun iced(lands: String): Pair<TestGame, EntityId> {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Test Ice Warden")
                .withCardInHand(1, "Test Blue Insight")
                .withLandsOnBattlefield(1, lands, 2)
                .withCardInExile(2, "Test Blue Drake")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val drake = game.state.getExile(game.player2Id).single()
            game.state = game.state.updateEntity(drake) {
                it.with(CountersComponent().withAdded(CounterType.ICE, 1))
            }
            return game to drake
        }

        test("the iced card is offered and cast with snow mana of another color") {
            val (game, drake) = iced("Snow-Covered Forest")
            LegalActionEnumerator.create(cardRegistry).enumerate(game.state, game.player1Id)
                .single { (it.action as? CastSpell)?.cardId == drake }.affordable shouldBe true
            game.execute(CastSpell(game.player1Id, drake)).error shouldBe null
            game.resolveStack()
            game.findPermanent("Test Blue Drake") shouldNotBe null
        }

        test("non-snow lands of another color can't pay for it") {
            val (game, drake) = iced("Forest")
            LegalActionEnumerator.create(cardRegistry).enumerate(game.state, game.player1Id)
                .singleOrNull { (it.action as? CastSpell)?.cardId == drake }?.affordable shouldNotBe true
            game.execute(CastSpell(game.player1Id, drake)).error shouldNotBe null
        }

        test("the permission covers only the iced spells, not your other spells") {
            val (game, _) = iced("Snow-Covered Forest")
            val insight = game.findCardsInHand(1, "Test Blue Insight").single()
            game.execute(CastSpell(game.player1Id, insight)).error shouldNotBe null
        }
    }
}
