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
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * "X can't be 0" — `spell { xMinimum = 1 }` (CardScript.xMinimum), checked as X is announced
 * (CR 601.2b) on every casting rail:
 *
 *  - The cast offer's X slider starts at the floor, and an offer that can't pay for it can't be cast.
 *  - An announced X below the floor is rejected and the card stays in hand; X at the floor resolves.
 *  - A cast without paying its mana cost has 0 as the only legal X (CR 107.3b), so it can't be cast
 *    at all — Mind Grind's ruling. The free-cast rail itself lives in [GrantFreeCastThisTurnTest];
 *    here a hand-built free cast is rejected by the validator whatever X it claims.
 *  - A spell with no floor still offers X = 0 (the default doesn't move).
 */
class SpellXMinimumTest : ScenarioTestBase() {

    private val floored = card("X Floored At One") {
        manaCost = "{X}{G}"
        typeLine = "Sorcery"
        spell {
            xMinimum = 1
            effect = Effects.GainLife(DynamicAmounts.xValue())
        }
    }

    private val unfloored = card("X Unfloored") {
        manaCost = "{X}{G}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(DynamicAmounts.xValue()) }
    }

    private val freeCastRitual = card("Floor Test Free Cast Ritual") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell { effect = Effects.GrantNextSpellFreeCast() }
    }

    private fun TestGame.handCardId(name: String): EntityId =
        state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.offerFor(name: String) = getLegalActions(1).first {
        (it.action as? CastSpell)?.cardId == handCardId(name) && it.actionType == "CastSpell"
    }

    private fun game(spell: String, forests: Int) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, spell)
        .withLandsOnBattlefield(1, "Forest", forests)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        cardRegistry.register(floored)
        cardRegistry.register(unfloored)
        cardRegistry.register(freeCastRitual)

        context("the cast offer") {
            test("the X slider starts at the floor") {
                val g = game("X Floored At One", forests = 4)
                val offer = g.offerFor("X Floored At One")
                offer.hasXCost shouldBe true
                offer.isAffordable shouldBe true
                offer.minX shouldBe 1
                offer.maxAffordableX shouldBe 3
            }

            test("mana for only X = 0 leaves the spell uncastable") {
                val g = game("X Floored At One", forests = 1)
                withClue("one Forest pays {G} but not X = 1") { g.offerFor("X Floored At One").isAffordable shouldBe false }
            }

            test("without a floor, X = 0 is still offered") {
                val g = game("X Unfloored", forests = 1)
                val offer = g.offerFor("X Unfloored")
                offer.isAffordable shouldBe true
                offer.minX shouldBe 0
            }
        }

        context("cast validation (CR 601.2b)") {
            test("X = 0 is rejected and the card stays in hand") {
                val g = game("X Floored At One", forests = 4)
                val cast = g.execute(CastSpell(g.player1Id, g.handCardId("X Floored At One"), xValue = 0))
                cast.error.shouldNotBeNull() shouldContain "X can't be 0"
                g.isInHand(1, "X Floored At One") shouldBe true
            }

            test("X at the floor casts and resolves for X") {
                val g = game("X Floored At One", forests = 4)
                g.execute(CastSpell(g.player1Id, g.handCardId("X Floored At One"), xValue = 1)).error shouldBe null
                g.resolveStack()
                g.getLifeTotal(1) shouldBe 21
            }

            test("a cast without paying its mana cost can't be cast at all (CR 107.3b)") {
                val g = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "X Floored At One")
                    .withCardInHand(1, "Floor Test Free Cast Ritual")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                g.castSpell(1, "Floor Test Free Cast Ritual").error shouldBe null
                g.resolveStack()
                g.state.pendingFreeCastSpells.size shouldBe 1

                val spell = g.handCardId("X Floored At One")
                val freeCast = g.getLegalActions(1).single {
                    it.actionType == "CastWithoutPayingManaCost" && (it.action as? CastSpell)?.cardId == spell
                }
                withClue("the free cast's only legal X is 0, which the floor forbids") { freeCast.isAffordable shouldBe false }

                val cast = g.execute(
                    CastSpell(g.player1Id, spell, xValue = 2, useWithoutPayingManaCost = true)
                )
                cast.error.shouldNotBeNull() shouldContain "X can't be 0, so this spell can't be cast without paying its mana cost"
                g.isInHand(1, "X Floored At One") shouldBe true
            }
        }
    }
}
