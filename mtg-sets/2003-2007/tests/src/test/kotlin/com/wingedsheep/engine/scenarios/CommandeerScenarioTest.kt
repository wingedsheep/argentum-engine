package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.csp.cards.Commandeer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Commandeer (CSP #29) — "You may exile two blue cards from your hand rather than pay this
 * spell's mana cost. Gain control of target noncreature spell. You may choose new targets for it."
 */
class CommandeerScenarioTest : FunSpec({

    class Game(val d: GameTestDriver, val you: EntityId, val opponent: EntityId)

    /** The opponent is active and holds priority in their main phase; you hold Commandeer. */
    fun newGame(): Game {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(Commandeer))
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val opponent = d.activePlayer!!
        return Game(d, d.getOpponent(opponent), opponent)
    }

    fun pitch(you: EntityId, commandeer: EntityId, spell: EntityId, fodder: List<EntityId>) = CastSpell(
        playerId = you,
        cardId = commandeer,
        targets = listOf(ChosenTarget.Spell(spell)),
        useAlternativeCost = true,
        alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
        additionalCostPayment = AdditionalCostPayment(exiledCards = fodder)
    )

    test("pitching two blue cards steals an enchantment spell, which enters under your control") {
        val g = newGame()
        val d = g.d
        val enchantment = d.putCardInHand(g.opponent, "Test Enchantment")
        d.giveMana(g.opponent, Color.WHITE, 2)
        d.castSpell(g.opponent, enchantment).error shouldBe null
        d.passPriority(g.opponent)

        val fodder = listOf(d.putCardInHand(g.you, "Counterspell"), d.putCardInHand(g.you, "Counterspell"))
        val commandeer = d.putCardInHand(g.you, "Commandeer")
        d.submit(pitch(g.you, commandeer, enchantment, fodder)).error shouldBe null
        while (d.stackSize > 0) d.bothPass()

        withClue("the stolen permanent spell enters under Commandeer's caster") {
            d.getController(enchantment) shouldBe g.you
        }
        d.getExileCardNames(g.you) shouldBe listOf("Counterspell", "Counterspell")
        d.getGraveyardCardNames(g.you) shouldBe listOf("Commandeer")
    }

    test("hard-cast: take a Lightning Bolt aimed at you and point it at its caster") {
        val g = newGame()
        val d = g.d
        val bolt = d.putCardInHand(g.opponent, "Lightning Bolt")
        d.giveMana(g.opponent, Color.RED, 1)
        d.castSpell(g.opponent, bolt, targets = listOf(g.you)).error shouldBe null
        d.passPriority(g.opponent)

        val commandeer = d.putCardInHand(g.you, "Commandeer")
        d.giveMana(g.you, Color.BLUE, 7)
        d.submit(
            CastSpell(g.you, commandeer, targets = listOf(ChosenTarget.Spell(bolt)), paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        d.bothPass() // Commandeer resolves

        // "You may choose new targets for it": redirect the Bolt to the opponent.
        d.submitCardSelection(g.you, listOf(g.opponent)).error shouldBe null
        d.bothPass() // the stolen Bolt resolves

        d.getLifeTotal(g.opponent) shouldBe 17
        d.getLifeTotal(g.you) shouldBe 20
        withClue("a stolen instant still goes to its owner's graveyard") {
            d.getGraveyardCardNames(g.opponent) shouldBe listOf("Lightning Bolt")
        }
    }

    test("a creature spell can't be targeted") {
        val g = newGame()
        val d = g.d
        val elves = d.putCardInHand(g.opponent, "Llanowar Elves")
        d.giveMana(g.opponent, Color.GREEN, 1)
        d.castSpell(g.opponent, elves).error shouldBe null
        d.passPriority(g.opponent)

        val fodder = listOf(d.putCardInHand(g.you, "Counterspell"), d.putCardInHand(g.you, "Counterspell"))
        val commandeer = d.putCardInHand(g.you, "Commandeer")
        d.submit(pitch(g.you, commandeer, elves, fodder)).error shouldNotBe null
    }

    test("the pitch cost needs two blue cards") {
        val g = newGame()
        val d = g.d
        val enchantment = d.putCardInHand(g.opponent, "Test Enchantment")
        d.giveMana(g.opponent, Color.WHITE, 2)
        d.castSpell(g.opponent, enchantment).error shouldBe null
        d.passPriority(g.opponent)

        val fodder = listOf(d.putCardInHand(g.you, "Counterspell"), d.putCardInHand(g.you, "Lightning Bolt"))
        val commandeer = d.putCardInHand(g.you, "Commandeer")
        d.submit(pitch(g.you, commandeer, enchantment, fodder)).error shouldNotBe null
    }
})
