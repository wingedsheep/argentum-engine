package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [CostGating.OnlyIf] reads "you" as the cost modifier's controller: a battlefield tax on other
 * players' spells (Paladin Class's "spells your opponents cast during your turn cost {1} more")
 * gates on its controller's turn and board, not the caster's. A self-cast modifier has no source
 * permanent, so its gate still reads the caster.
 */
class CostGatingControllerTest : FunSpec({

    val duringYourTurnTax = card("Test Your-Turn Tax") {
        manaCost = "{1}"
        typeLine = "Artifact"
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.OpponentsCast(GameObjectFilter.Any),
                modification = CostModification.IncreaseGeneric(1),
                gating = CostGating.OnlyIf(Conditions.IsYourTurn),
            )
        }
    }
    val whileYouControlCreatureTax = card("Test Board Tax") {
        manaCost = "{1}"
        typeLine = "Artifact"
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.AnyCaster(GameObjectFilter.Any),
                modification = CostModification.IncreaseGeneric(2),
                gating = CostGating.OnlyIf(Conditions.YouControl(GameObjectFilter.Creature)),
            )
        }
    }
    val selfCastOnYourTurn = card("Test Self Discount") {
        manaCost = "{2}{G}"
        typeLine = "Sorcery"
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.SelfCast,
                modification = CostModification.ReduceGeneric(2),
                gating = CostGating.OnlyIf(Conditions.IsYourTurn),
            )
        }
    }
    val ritual = card("Test Ritual") { manaCost = "{G}"; typeLine = "Sorcery" }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(duringYourTurnTax, whileYouControlCreatureTax, selfCastOnYourTurn, ritual))
        initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.calculator() = CostCalculator(cardRegistry, PredicateEvaluator(cardRegistry = cardRegistry))

    test("an opponent-tax gated on IsYourTurn reads the source controller's turn, not the caster's") {
        val d = driver()
        d.activePlayer shouldBe d.player1
        d.putPermanentOnBattlefield(d.player1, duringYourTurnTax.name)
        val calc = d.calculator()

        withClue("player 1's turn: player 2's spells are taxed") {
            calc.calculateEffectiveCost(d.state, ritual, d.player2) shouldBe ManaCost.parse("{1}{G}")
        }
        withClue("the controller's own spells are never in scope") {
            calc.calculateEffectiveCost(d.state, ritual, d.player1) shouldBe ritual.manaCost
        }

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = d.player2)
        withClue("player 2's own turn is not the taxer's controller's turn") {
            calc.calculateEffectiveCost(d.state, ritual, d.player2) shouldBe ritual.manaCost
        }
    }

    test("a player-scoped board condition on a battlefield modifier reads its controller") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, whileYouControlCreatureTax.name)
        d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val calc = d.calculator()

        withClue("only the caster controls a creature: the taxer's controller doesn't, so no tax") {
            calc.calculateEffectiveCost(d.state, ritual, d.player2) shouldBe ritual.manaCost
            calc.calculateEffectiveCost(d.state, ritual, d.player1) shouldBe ritual.manaCost
        }

        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        withClue("the taxer's controller controls a creature: every caster is taxed") {
            calc.calculateEffectiveCost(d.state, ritual, d.player2) shouldBe ManaCost.parse("{2}{G}")
            calc.calculateEffectiveCost(d.state, ritual, d.player1) shouldBe ManaCost.parse("{2}{G}")
        }
    }

    test("a self-cast modifier's gate still reads the caster") {
        val d = driver()
        val calc = d.calculator()

        calc.calculateEffectiveCost(d.state, selfCastOnYourTurn, d.player1) shouldBe ManaCost.parse("{G}")
        withClue("cast on player 1's turn by player 2, it's not the caster's turn") {
            calc.calculateEffectiveCost(d.state, selfCastOnYourTurn, d.player2) shouldBe selfCastOnYourTurn.manaCost
        }
    }
})
