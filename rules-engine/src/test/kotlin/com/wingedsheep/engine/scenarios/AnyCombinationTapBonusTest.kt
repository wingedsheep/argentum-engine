package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * [AdditionalManaOnTap] with `anyColor = true` and an amount above one — "adds an additional two
 * mana in any combination of colors" (Market Festival, Dawn's Reflection). Each unit's color is its
 * own choice: a manual tap asks once per unit (the land's mana first, then each bonus unit), and the
 * auto-payer treats every unit as flexible, so one tap can pay two different colored pips.
 */
class AnyCombinationTapBonusTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(GrovePlot, FestivalAura, OneColorAura, RainbowSpell))

        fun baseScenario(vararg auras: String) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Grove Plot")
            .apply {
                for (aura in auras) {
                    withCardOnBattlefield(1, aura)
                    withCardAttachedTo(1, aura, "Grove Plot")
                }
            }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("a manual tap asks for each unit's color, so the two bonus mana can differ") {
            val game = baseScenario("Festival Aura").build()

            game.execute(tapForMana(game)).error shouldBe null
            val first = pendingColor(game)
            first.prompt shouldContain "2 remaining"
            game.submitDecision(ColorChosenResponse(first.id, Color.RED)).error shouldBe null
            val second = pendingColor(game)
            game.submitDecision(ColorChosenResponse(second.id, Color.BLUE)).error shouldBe null

            game.getPendingDecision() shouldBe null
            val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
            withClue("the land's {G} plus one red and one blue bonus, pool was $pool") {
                pool.green shouldBe 1
                pool.red shouldBe 1
                pool.blue shouldBe 1
                pool.total shouldBe 3
            }
        }

        test("both units may be the same color") {
            val game = baseScenario("Festival Aura").build()

            game.execute(tapForMana(game)).error shouldBe null
            repeat(2) { game.submitDecision(ColorChosenResponse(pendingColor(game).id, Color.WHITE)).error shouldBe null }

            val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
            withClue("pool was $pool") {
                pool.white shouldBe 2
                pool.green shouldBe 1
            }
        }

        test("two any-color auras on one land: every unit of both is chosen in turn") {
            val game = baseScenario("Festival Aura", "One Color Aura").build()

            game.execute(tapForMana(game)).error shouldBe null
            for (color in listOf(Color.RED, Color.BLACK, Color.WHITE)) {
                game.submitDecision(ColorChosenResponse(pendingColor(game).id, color)).error shouldBe null
            }

            game.getPendingDecision() shouldBe null
            val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
            withClue("{G} plus three independently chosen bonus mana, pool was $pool") {
                pool.green shouldBe 1
                pool.red shouldBe 1
                pool.black shouldBe 1
                pool.white shouldBe 1
                pool.total shouldBe 4
            }
        }

        test("auto-pay spends the two bonus units as two different colors from one tap") {
            val game = baseScenario("Festival Aura")
                .withCardInHand(1, "Rainbow Spell")
                .build()

            withClue("one enchanted land pays {R}{U}{G}") {
                game.castSpell(1, "Rainbow Spell").error shouldBe null
            }
            game.state.stack.size shouldBe 1
        }
    }

    companion object {
        private fun tapForMana(game: ScenarioTestBase.TestGame) = ActivateAbility(
            playerId = game.player1Id,
            sourceId = game.findPermanent("Grove Plot")!!,
            abilityId = GrovePlot.activatedAbilities.first { it.isManaAbility }.id
        )

        private fun pendingColor(game: ScenarioTestBase.TestGame): ChooseColorDecision {
            val decision = game.getPendingDecision() as? ChooseColorDecision
            withClue("the any-color tap bonus should be asking for a color") { decision shouldNotBe null }
            return decision!!
        }

        /** "{T}: Add {G}." */
        private val GrovePlot: CardDefinition = card("Grove Plot") {
            typeLine = "Land"
            activatedAbility {
                manaAbility = true
                cost = Costs.Tap
                effect = Effects.AddMana(Color.GREEN)
            }
        }

        /** Market Festival's shape: two additional mana in any combination of colors. */
        private val FestivalAura = card("Festival Aura") {
            typeLine = "Enchantment — Aura"
            staticAbility { ability = AdditionalManaOnTap(amount = DynamicAmount.Fixed(2), anyColor = true) }
        }

        /** Fertile Ground's shape: one additional mana of any color. */
        private val OneColorAura = card("One Color Aura") {
            typeLine = "Enchantment — Aura"
            staticAbility { ability = AdditionalManaOnTap(amount = DynamicAmount.Fixed(1), anyColor = true) }
        }

        private val RainbowSpell = card("Rainbow Spell") {
            manaCost = "{R}{U}{G}"
            typeLine = "Sorcery"
            spell { effect = Effects.DrawCards(1) }
        }
    }
}
