package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CounterTax
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.puzzles.advanceToStackResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [AiProfile.taxCountersReadTheirMana], as [AiProfile.LIVE] ships it.
 *
 * Game `2f830698` (2026-10-15, Edge of Eternities sealed): two Divert Disasters ("counter target
 * spell unless its controller pays {2}") held from turn 9 to 21. Nova Hellkite resolved on turn 15
 * off seven lands — two left over, four Islands open on our side — because one Divert alone gets
 * paid and the line where the first drains their mana and the second counters was never seen. On
 * turn 21 both were thrown at a Bombard cast off ten lands, and both taxes were paid.
 */
class TaxCountersReadTheirManaTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun TestGame.choice(profile: AiProfile = AiProfile.LIVE): GameAction =
        AIPlayer.create(cardRegistry, player1Id, profile).chooseAction(state)

    private fun TestGame.castName(action: GameAction): String? =
        (action as? CastSpell)?.let { state.getEntity(it.cardId)?.get<CardComponent>()?.name }

    /**
     * Seat 2 casts Nova Hellkite (five mana) off [theirLands] Mountains, so `theirLands - 5` stay
     * open; seat 1 holds [diverts] Divert Disasters behind four Islands and gets the response window.
     */
    private fun hellkiteOnTheStack(theirLands: Int, diverts: Int = 2) = scenario()
        .withPlayers()
        .withTurnNumber(15)
        .withActivePlayer(2)
        .withLandsOnBattlefield(2, "Mountain", theirLands)
        .withCardInHand(2, "Nova Hellkite")
        .withCardsInHand(2, "Craw Wurm", 2)
        .withLandsOnBattlefield(1, "Island", 4)
        .withCardsInHand(1, "Divert Disaster", diverts)
        .build()
        .also { it.castSpell(2, "Nova Hellkite").error shouldBe null }
        .advanceToStackResponse(1)

    private fun TestGame.reading(): CounterTax.Reading {
        val divert = state.getZone(player1Id, Zone.HAND).first {
            state.getEntity(it)?.get<CardComponent>()?.name == "Divert Disaster"
        }
        val spell = state.stack.single()
        return CounterTax.read(
            state, player1Id, intents.forName("Divert Disaster")!!,
            CastSpell(player1Id, divert, listOf(ChosenTarget.Spell(spell))),
            intents,
        )
    }

    init {
        test("Divert Disaster reads as a tax counter of two") {
            intents.forName("Divert Disaster")!!.counterTax shouldBe 2
            intents.forName("Counterspell")!!.counterTax shouldBe null
        }

        test("T15: two mana open against two Diverts is the first of a stack, and the AI starts it") {
            val game = hellkiteOnTheStack(theirLands = 7)

            game.reading().shouldBeInstanceOf<CounterTax.Reading.FirstOfAStack>()
            game.castName(game.choice()) shouldBe "Divert Disaster"
        }

        test("with one Divert, two open mana pays the only tax there is") {
            hellkiteOnTheStack(theirLands = 7, diverts = 1).reading() shouldBe CounterTax.Reading.Futile
        }

        test("T21: a caster with more open mana than every tax we can stack is not taxed at all") {
            // Six open against two Diverts' four: they pay both and the spell resolves anyway.
            val game = hellkiteOnTheStack(theirLands = 11)

            game.reading() shouldBe CounterTax.Reading.Futile
            game.castName(game.choice()) shouldNotBe "Divert Disaster"
        }

        test("a tapped-out caster is just countered — the leaf already sees it") {
            val game = hellkiteOnTheStack(theirLands = 5)

            game.reading() shouldBe CounterTax.Reading.Silent
            game.castName(game.choice()) shouldBe "Divert Disaster"
        }
    }
}
