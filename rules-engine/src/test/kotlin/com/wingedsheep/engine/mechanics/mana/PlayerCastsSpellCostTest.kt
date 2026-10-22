package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [SpellCostTarget.PlayerCasts]: a tax on the one player its source names — the player an Aura
 * enchants, or the opponent / player chosen as it entered — never on anyone else, and on nobody
 * while the source names no one. Any other player reference is rejected at construction.
 */
class PlayerCastsSpellCostTest : FunSpec({

    fun taxer(player: Player) = card("Test ${player.description} Taxer") {
        manaCost = "{1}"
        typeLine = "Artifact"
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.PlayerCasts(player, GameObjectFilter.Creature),
                modification = CostModification.IncreaseGeneric(2),
            )
        }
    }

    val enchantedTaxer = taxer(Player.EnchantedPlayer)
    val chosenOpponentTaxer = taxer(Player.ChosenOpponent)
    val chosenPlayerTaxer = taxer(Player.ChosenPlayer)
    val bear = card("Test Bear") { manaCost = "{1}{G}"; typeLine = "Creature — Bear"; power = 2; toughness = 2 }
    val ritual = card("Test Ritual") { manaCost = "{G}"; typeLine = "Sorcery" }
    val taxed = ManaCost.parse("{3}{G}")

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(enchantedTaxer, chosenOpponentTaxer, chosenPlayerTaxer, bear, ritual))
        initMirrorMatch(deck = Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.calculator() = CostCalculator(cardRegistry, PredicateEvaluator(cardRegistry = cardRegistry))

    test("EnchantedPlayer taxes only the player the source is attached to") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, enchantedTaxer.name)
        val calc = d.calculator()
        withClue("unattached, it names nobody") {
            calc.calculateEffectiveCost(d.state, bear, d.player2) shouldBe bear.manaCost
            calc.calculateEffectiveCost(d.state, bear, d.player1) shouldBe bear.manaCost
        }

        d.replaceState(d.state.updateEntity(source) { it.with(AttachedToComponent(d.player2)) })
        calc.calculateEffectiveCost(d.state, bear, d.player2) shouldBe taxed
        withClue("the source's controller isn't the enchanted player") {
            calc.calculateEffectiveCost(d.state, bear, d.player1) shouldBe bear.manaCost
        }
        withClue("the filter still applies") {
            calc.calculateEffectiveCost(d.state, ritual, d.player2) shouldBe ritual.manaCost
        }

        d.replaceState(d.state.updateEntity(source) { it.with(AttachedToComponent(d.player1)) })
        withClue("a self-curse taxes its own controller") {
            calc.calculateEffectiveCost(d.state, bear, d.player1) shouldBe taxed
            calc.calculateEffectiveCost(d.state, bear, d.player2) shouldBe bear.manaCost
        }
    }

    test("ChosenOpponent and ChosenPlayer read the as-enters choice") {
        val d = driver()
        val opp = d.putPermanentOnBattlefield(d.player1, chosenOpponentTaxer.name)
        val any = d.putPermanentOnBattlefield(d.player1, chosenPlayerTaxer.name)
        val calc = d.calculator()
        withClue("no choice recorded, no tax") {
            calc.calculateEffectiveCost(d.state, bear, d.player2) shouldBe bear.manaCost
        }

        d.replaceState(
            d.state
                .updateEntity(opp) { it.with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.OPPONENT to ChoiceValue.EntityChoice(d.player2)))) }
                .updateEntity(any) { it.with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.PLAYER to ChoiceValue.EntityChoice(d.player1)))) }
        )
        withClue("the chosen opponent pays the opponent taxer only") {
            calc.calculateEffectiveCost(d.state, bear, d.player2) shouldBe taxed
        }
        withClue("the chosen player — here its own controller — pays the player taxer only") {
            calc.calculateEffectiveCost(d.state, bear, d.player1) shouldBe taxed
        }
    }

    test("a reference the source doesn't record is rejected") {
        shouldThrow<IllegalArgumentException> { SpellCostTarget.PlayerCasts(Player.You) }
        shouldThrow<IllegalArgumentException> { SpellCostTarget.PlayerCasts(Player.EachOpponent) }
    }

    test("description names the player") {
        enchantedTaxer.script.staticAbilities.single().description shouldBe
            "creature spells enchanted player casts cost {2} more"
    }
})
