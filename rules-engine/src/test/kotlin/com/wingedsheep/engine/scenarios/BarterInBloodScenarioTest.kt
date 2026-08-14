package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.BarterInBlood
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Barter in Blood (MRD #57) — "Each player sacrifices two creatures of their choice."
 *
 * The interesting cases are the two the `count = 1` edicts never reach: a player who must *choose*
 * two out of more (the prompt has to demand exactly two, not one), and a player who controls fewer
 * than two, who sacrifices as many as they can rather than nothing at all (CR 608.2c / the printed
 * ruling "if a player controls only one creature, that creature is sacrificed").
 */
class BarterInBloodScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + BarterInBlood)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.castBarter(caster: com.wingedsheep.sdk.model.EntityId) {
        val card = putCardInHand(caster, "Barter in Blood")
        giveMana(caster, Color.BLACK, 4)
        castSpell(caster, card).isSuccess shouldBe true
        bothPass()
    }

    test("each player with exactly two creatures loses both, no prompt") {
        val d = driver()
        val bear1 = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val bear2 = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val bear3 = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val bear4 = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")

        d.castBarter(d.player1)

        withClue("two-for-two is exact, so the executor auto-sacrifices without asking") {
            d.pendingDecision shouldBe null
        }
        listOf(bear1, bear2, bear3, bear4).forEach {
            d.state.getBattlefield().contains(it) shouldBe false
        }
    }

    test("a player controlling three creatures must choose exactly two") {
        val d = driver()
        val keep = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val give1 = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val give2 = d.putCreatureOnBattlefield(d.player1, "Hill Giant")
        // player2 controls exactly two, so only player1 is prompted.
        d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.putCreatureOnBattlefield(d.player2, "Centaur Courser")

        d.castBarter(d.player1)

        val decision = d.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe d.player1
        withClue("'sacrifices two' is not an edict for one — the choice is exactly two") {
            decision.minSelections shouldBe 2
            decision.maxSelections shouldBe 2
        }

        d.submitCardSelection(d.player1, listOf(give1, give2))

        d.state.getBattlefield().contains(keep) shouldBe true
        d.state.getBattlefield().contains(give1) shouldBe false
        d.state.getBattlefield().contains(give2) shouldBe false
        withClue("player2's two creatures went too") {
            d.getPermanents(d.player2).none { d.getCardName(it) == "Grizzly Bears" } shouldBe true
        }
    }

    test("a player controlling one creature sacrifices it; one controlling none loses nothing") {
        val d = driver()
        val lone = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        // player1 controls no creatures at all.

        d.castBarter(d.player1)

        d.pendingDecision shouldBe null
        withClue("CR 608.2c: sacrifice as many as you can, not zero") {
            d.state.getBattlefield().contains(lone) shouldBe false
            d.getGraveyardCardNames(d.player2).contains("Grizzly Bears") shouldBe true
        }
    }
})
