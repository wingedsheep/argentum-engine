package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "You may spend colorless mana as though it were mana of any color to cast that spell" —
 * [SpellPaymentContext.colorlessAsAnyColor], carried by a may-play grant's `colorlessAsAnyColor`
 * rider (Abstruse Appropriation).
 *
 * CR 609.4b: spending mana "as though it were mana of any color" affects only how the cost may be
 * paid — the cost is unchanged and the mana actually spent is still colorless. CR 118.14 contrasts
 * the broader "mana of any type", which also lets colored mana pay `{C}`; this permission doesn't.
 */
class ColorlessAsAnyColorTest : ScenarioTestBase() {
    init {
        val colorlessAsAny = SpellPaymentContext(colorlessAsAnyColor = true)
        fun cost(value: String) = ManaCost.parse(value)

        // --- The floating pool --------------------------------------------------------------

        test("colorless pays a colored pip only under the permission") {
            ManaPool(colorless = 1).canPay(cost("{W}"), colorlessAsAny) shouldBe true
            ManaPool(colorless = 1).canPay(cost("{W}")) shouldBe false
            ManaPool(colorless = 1).canPay(cost("{W}"), SpellPaymentContext()) shouldBe false
        }

        test("colored mana still can't pay {C} — the permission is one-directional") {
            ManaPool(white = 1).canPay(cost("{C}"), colorlessAsAny) shouldBe false
        }

        test("hybrid, Phyrexian and monocolored-hybrid colored halves accept colorless") {
            for (symbol in listOf("{W/U}", "{B/P}", "{R/G/P}", "{2/G}")) {
                withClue(symbol) {
                    ManaPool(colorless = 1).pay(cost(symbol), colorlessAsAny)?.colorless shouldBe 0
                }
            }
        }

        test("CR 609.4b — the mana actually spent is still colorless") {
            val result = ManaPool(colorless = 2).payPartial(cost("{W}{B}"), colorlessAsAny)
            result.remainingCost.isEmpty() shouldBe true
            result.manaSpent.colorless shouldBe 2
            result.manaSpent.white shouldBe 0
            result.manaSpent.black shouldBe 0
        }

        test("matching keeps colorless for a strict {C} pip and native color for the colored one") {
            val pool = ManaPool(white = 1, colorless = 1)
            pool.canPay(cost("{W}{C}"), colorlessAsAny) shouldBe true
            pool.canPay(cost("{C}{W}"), colorlessAsAny) shouldBe true
            pool.canPay(cost("{W}{W}"), colorlessAsAny) shouldBe true
            pool.canPay(cost("{C}{C}"), colorlessAsAny) shouldBe false
        }

        test("a generic-only cost pays normally under the permission") {
            ManaPool(colorless = 2).pay(cost("{2}"), colorlessAsAny)?.colorless shouldBe 0
            ManaPool(colorless = 1).canPay(cost("{2}"), colorlessAsAny) shouldBe false
        }

        test("colorless can't be counted twice") {
            ManaPool(colorless = 1).canPay(cost("{W}{1}"), colorlessAsAny) shouldBe false
            ManaPool(colorless = 1).canPay(cost("{W}{B}"), colorlessAsAny) shouldBe false
        }

        // --- The auto-tap solver ------------------------------------------------------------

        cardRegistry.register(card("Test Colorless Land") {
            typeLine = "Land"
            activatedAbility { cost = AbilityCost.Tap; manaAbility = true; effect = Effects.AddColorlessMana(1) }
        })

        test("solver taps colorless sources for colored pips only under the permission") {
            val game = scenario().withPlayers().withLandsOnBattlefield(1, "Test Colorless Land", 2)
                .withActivePlayer(1).build()
            services.manaSolver.canPay(game.state, game.player1Id, cost("{W}{B}"), spellContext = colorlessAsAny) shouldBe true
            services.manaSolver.canPay(game.state, game.player1Id, cost("{W}{B}")) shouldBe false
            val solution = services.manaSolver.solve(game.state, game.player1Id, cost("{W}{B}"), spellContext = colorlessAsAny)!!
            solution.sources.size shouldBe 2
            solution.manaProduced.values.all { it.color == null && it.colorless == 1 } shouldBe true
        }

        test("solver prefers a native colored source and uses colorless only for the missing color") {
            val game = scenario().withPlayers().withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Test Colorless Land", 1).withActivePlayer(1).build()
            val plains = game.findPermanent("Plains")!!
            val solution = services.manaSolver.solve(game.state, game.player1Id, cost("{W}{B}"), spellContext = colorlessAsAny)!!
            solution.manaProduced[plains]?.color shouldBe Color.WHITE
            solution.manaProduced.values.count { it.color == null && it.colorless == 1 } shouldBe 1
        }

        test("solver spends a multi-mana colorless source's extra mana on a second colored pip") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Sol Ring").withActivePlayer(1).build()
            services.manaSolver.canPay(game.state, game.player1Id, cost("{W}{B}"), spellContext = colorlessAsAny) shouldBe true
            services.manaSolver.solve(game.state, game.player1Id, cost("{W}{B}"), spellContext = colorlessAsAny)!!
                .sources.size shouldBe 1
            services.manaSolver.canPay(game.state, game.player1Id, cost("{W}{B}{1}"), spellContext = colorlessAsAny) shouldBe false
        }

        test("floating colorless counts toward colored pips in the solver's affordability check") {
            val game = scenario().withPlayers().withLandsOnBattlefield(1, "Test Colorless Land", 1)
                .withActivePlayer(1).build()
            game.state = game.state.updateEntity(game.player1Id) { it.with(ManaPoolComponent(colorless = 1)) }
            services.manaSolver.canPay(game.state, game.player1Id, cost("{W}{W}"), spellContext = colorlessAsAny) shouldBe true
            services.manaSolver.canPay(game.state, game.player1Id, cost("{W}{W}")) shouldBe false
        }

        // --- Through a cast permission ------------------------------------------------------

        cardRegistry.register(card("Test White Knight") {
            manaCost = "{W}{W}"; typeLine = "Creature — Human Knight"; power = 2; toughness = 2
        })
        cardRegistry.register(card("Test White Tithe") {
            manaCost = "{W}"; typeLine = "Instant"
            spell { effect = Effects.GainLife(1) }
        })
        fun granter(name: String, colorlessAsAnyColor: Boolean) = card(name) {
            manaCost = "{0}"; typeLine = "Instant"
            spell {
                target(TargetFilter.NonlandPermanent)
                effect = Effects.Pipeline {
                    val taken = gather(CardSource.ChosenTargets)
                    exile(taken)
                    run(Effects.GrantMayPlayFromExile(
                        from = taken,
                        expiry = MayPlayExpiry.Permanent,
                        nonLandOnly = true,
                        colorlessAsAnyColor = colorlessAsAnyColor
                    ))
                }
            }
        }
        cardRegistry.register(granter("Test Colorless Appropriation", colorlessAsAnyColor = true))
        cardRegistry.register(granter("Test Plain Appropriation", colorlessAsAnyColor = false))

        fun appropriate(granterName: String): Pair<TestGame, EntityId> {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, granterName)
                .withCardInHand(1, "Test White Tithe")
                .withLandsOnBattlefield(1, "Test Colorless Land", 2)
                .withCardOnBattlefield(2, "Test White Knight")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val knight = game.findPermanent("Test White Knight")!!
            game.castSpell(1, granterName, knight).error shouldBe null
            game.resolveStack()
            val exiled = game.state.getExile(game.player2Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Test White Knight"
            }
            return game to exiled
        }

        test("the exiled card is offered and cast with colorless mana for its colored pips") {
            val (game, knight) = appropriate("Test Colorless Appropriation")
            val offered = LegalActionEnumerator.create(cardRegistry).enumerate(game.state, game.player1Id)
                .single { (it.action as? CastSpell)?.cardId == knight }
            offered.affordable shouldBe true

            game.execute(CastSpell(game.player1Id, knight)).error shouldBe null
            game.resolveStack()
            val onBattlefield = game.findPermanent("Test White Knight")
            onBattlefield shouldNotBe null
            game.state.getEntity(onBattlefield!!)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
        }

        test("without the rider the same grant can't be paid with colorless mana") {
            val (game, knight) = appropriate("Test Plain Appropriation")
            val offered = LegalActionEnumerator.create(cardRegistry).enumerate(game.state, game.player1Id)
                .singleOrNull { (it.action as? CastSpell)?.cardId == knight }
            offered?.affordable shouldNotBe true
            game.execute(CastSpell(game.player1Id, knight)).error shouldNotBe null
        }

        test("the permission covers only the granted spell, not other spells you cast") {
            val (game, _) = appropriate("Test Colorless Appropriation")
            val tithe = game.findCardsInHand(1, "Test White Tithe").single()
            game.execute(CastSpell(game.player1Id, tithe)).error shouldNotBe null
        }
    }
}
