package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.DosanTheFallingLeaf
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Dosan the Falling Leaf — "Players can cast spells only during their own turns."
 *
 * A [com.wingedsheep.sdk.scripting.PlayersCantCastSpells] over `Player.Each` whose `IsNotYourTurn`
 * gate is read from the *caster's* seat (`conditionFromCaster`). The multiplayer case is the one a
 * controller-relative gate gets wrong: on a third player's turn, Dosan's controller is also off
 * turn, and must be locked out just like the opponent.
 */
class DosanTheFallingLeafScenarioTest : FunSpec({

    val probe = card("Dosan Test Glint") { manaCost = "{G}"; typeLine = "Instant"; oracleText = "" }

    fun GameTestDriver.castActionsFor(playerId: EntityId, cardId: EntityId): List<CastSpell> =
        LegalActionEnumerator.create(cardRegistry)
            .enumerate(state, playerId)
            .mapNotNull { it.action as? CastSpell }
            .filter { it.cardId == cardId }

    fun driver(startingPlayer: Int = 0) = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(DosanTheFallingLeaf, probe))
        initMirrorMatch(Deck.of("Forest" to 40), startingLife = 20, startingPlayer = startingPlayer)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("the non-active player can't cast an instant on Dosan's controller's turn") {
        val d = driver(startingPlayer = 0)
        d.activePlayer shouldBe d.player1
        d.putCreatureOnBattlefield(d.player1, "Dosan the Falling Leaf")
        val spell = d.putCardInHand(d.player2, "Dosan Test Glint")
        d.giveMana(d.player2, Color.GREEN, 1)

        d.castActionsFor(d.player2, spell) shouldHaveSize 0
    }

    test("the active player can still cast spells on their own turn") {
        val d = driver(startingPlayer = 0)
        d.putCreatureOnBattlefield(d.player1, "Dosan the Falling Leaf")
        val spell = d.putCardInHand(d.player1, "Dosan Test Glint")
        d.giveMana(d.player1, Color.GREEN, 1)

        d.castActionsFor(d.player1, spell).isNotEmpty() shouldBe true
    }

    test("the lock binds Dosan's controller on an opponent's turn, and frees that opponent") {
        val d = driver(startingPlayer = 1)
        d.activePlayer shouldBe d.player2
        d.putCreatureOnBattlefield(d.player1, "Dosan the Falling Leaf")
        val own = d.putCardInHand(d.player1, "Dosan Test Glint")
        d.giveMana(d.player1, Color.GREEN, 1)
        val theirs = d.putCardInHand(d.player2, "Dosan Test Glint")
        d.giveMana(d.player2, Color.GREEN, 1)

        d.castActionsFor(d.player1, own) shouldHaveSize 0
        d.castActionsFor(d.player2, theirs).isNotEmpty() shouldBe true
    }

    test("in multiplayer, Dosan's controller is locked out on a third player's turn") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(DosanTheFallingLeaf, probe))
        val players = d.initMultiplayer(List(3) { Deck.of("Forest" to 40) }, startingPlayer = 2)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (controller, bystander, active) = players
        d.activePlayer shouldBe active

        d.putCreatureOnBattlefield(controller, "Dosan the Falling Leaf")
        val controllerSpell = d.putCardInHand(controller, "Dosan Test Glint")
        d.giveMana(controller, Color.GREEN, 1)
        val bystanderSpell = d.putCardInHand(bystander, "Dosan Test Glint")
        d.giveMana(bystander, Color.GREEN, 1)
        val activeSpell = d.putCardInHand(active, "Dosan Test Glint")
        d.giveMana(active, Color.GREEN, 1)

        d.castActionsFor(controller, controllerSpell) shouldHaveSize 0
        d.castActionsFor(bystander, bystanderSpell) shouldHaveSize 0
        d.castActionsFor(active, activeSpell).isNotEmpty() shouldBe true
    }

    test("the lock lifts when Dosan leaves the battlefield") {
        val d = driver(startingPlayer = 0)
        val dosan = d.putCreatureOnBattlefield(d.player1, "Dosan the Falling Leaf")
        val spell = d.putCardInHand(d.player2, "Dosan Test Glint")
        d.giveMana(d.player2, Color.GREEN, 1)
        d.castActionsFor(d.player2, spell) shouldHaveSize 0

        d.moveToGraveyard(dosan)
        d.castActionsFor(d.player2, spell).isNotEmpty() shouldBe true
    }
})
