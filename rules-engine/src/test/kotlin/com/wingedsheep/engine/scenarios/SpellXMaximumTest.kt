package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * "X can't be greater than <amount>" — `spell { xMaximum = ... }` (CardScript.xMaximum). X is
 * announced as the spell is cast (CR 601.2b); the cap is read then and only then (Open the Way's
 * ruling: "the number of players in the game is checked only as you choose the value for X").
 *
 *  - The cast offer's X slider is capped at the smaller of the cap and what the mana pays for.
 *  - An announced X above the cap is rejected and the card stays in hand; X equal to it casts.
 *  - A cap of 0 still leaves X = 0 castable.
 *  - The cap doesn't follow the board after the cast: the resolving spell uses the announced X.
 */
class SpellXMaximumTest : ScenarioTestBase() {

    private val playerCapped = card("X Capped By Players") {
        manaCost = "{X}{G}"
        typeLine = "Sorcery"
        spell {
            xMaximum = DynamicAmounts.playerCount(Player.Each)
            effect = Effects.GainLife(DynamicAmounts.xValue())
        }
    }

    private val creatureCapped = card("X Capped By Creatures") {
        manaCost = "{X}{G}"
        typeLine = "Instant"
        spell {
            xMaximum = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).count()
            effect = Effects.GainLife(DynamicAmounts.xValue())
        }
    }

    private fun TestGame.handCardId(name: String): EntityId =
        state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.offerFor(name: String) = getLegalActions(1).first {
        (it.action as? CastSpell)?.cardId == handCardId(name)
    }

    private fun game(spell: String, forests: Int, bears: Int = 0) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, spell)
        .withLandsOnBattlefield(1, "Forest", forests)
        .apply { repeat(bears) { withCardOnBattlefield(1, "Grizzly Bears") } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        cardRegistry.register(playerCapped)
        cardRegistry.register(creatureCapped)

        context("the cast offer") {
            test("the X slider stops at the cap even with mana for more") {
                val g = game("X Capped By Players", forests = 7)
                val offer = g.offerFor("X Capped By Players")
                offer.hasXCost shouldBe true
                withClue("seven Forests pay for X = 6, but two players cap X at 2") { offer.maxAffordableX shouldBe 2 }
            }

            test("mana still bounds X below the cap") {
                val g = game("X Capped By Players", forests = 2)
                g.offerFor("X Capped By Players").maxAffordableX shouldBe 1
            }

            test("a cap of 0 still offers the spell at X = 0") {
                val g = game("X Capped By Creatures", forests = 4)
                g.offerFor("X Capped By Creatures").maxAffordableX shouldBe 0
                g.execute(CastSpell(g.player1Id, g.handCardId("X Capped By Creatures"), xValue = 0)).error shouldBe null
            }
        }

        context("cast validation (CR 601.2b)") {
            test("X equal to the cap casts and resolves for X") {
                val g = game("X Capped By Players", forests = 7)
                g.execute(CastSpell(g.player1Id, g.handCardId("X Capped By Players"), xValue = 2)).error shouldBe null
                g.resolveStack()
                g.getLifeTotal(1) shouldBe 22
            }

            test("X above the cap is rejected and the card stays in hand") {
                val g = game("X Capped By Players", forests = 7)
                val cast = g.execute(CastSpell(g.player1Id, g.handCardId("X Capped By Players"), xValue = 3))
                cast.error.shouldNotBeNull() shouldContain "X can't be greater than 2"
                g.isInHand(1, "X Capped By Players") shouldBe true
            }

            test("the cap is read as X is announced, not again on resolution") {
                val g = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "X Capped By Creatures")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                g.execute(CastSpell(g.player1Id, g.handCardId("X Capped By Creatures"), xValue = 2)).error shouldBe null
                // A creature dies in response, so the cap would now read 1; the announced X stands.
                g.castSpell(1, "Shock", g.findPermanents("Grizzly Bears").first()).error shouldBe null
                g.resolveStack()
                g.findPermanents("Grizzly Bears").size shouldBe 1
                g.getLifeTotal(1) shouldBe 22
            }
        }
    }
}
