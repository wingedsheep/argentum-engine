package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Spelltwine (M13 #68) — {5}{U} Sorcery.
 *
 * "Exile target instant or sorcery card from your graveyard and target instant or sorcery card
 * from an opponent's graveyard. Copy those cards. Cast the copies if able without paying their
 * mana costs. Exile Spelltwine."
 *
 * Pins the two-target exile → copy → mandatory free cast: both copies are cast (no "may" offer),
 * in the order the caster picks, the copy of the opponent's card is the caster's spell, the
 * originals stay exiled, and Spelltwine exiles itself. It can't be cast without both targets.
 */
class SpelltwineScenarioTest : ScenarioTestBase() {

    private fun TestGame.castSpelltwine(mine: EntityId, theirs: EntityId): ExecutionResult {
        val spelltwine = state.getHand(player1Id)
            .first { state.getEntity(it)?.get<CardComponent>()?.name == "Spelltwine" }
        return execute(
            CastSpell(
                playerId = player1Id,
                cardId = spelltwine,
                targets = listOf(
                    ChosenTarget.Card(mine, player1Id, Zone.GRAVEYARD),
                    ChosenTarget.Card(theirs, player2Id, Zone.GRAVEYARD),
                ),
            )
        )
    }

    private fun TestGame.nameOf(id: EntityId): String? = state.getEntity(id)?.get<CardComponent>()?.name

    /** Drives resolution; fails on any decision other than the order pick and the copies' targets. */
    private fun TestGame.resolveAll(castFirst: String) {
        var guard = 0
        while (guard++ < 30) {
            when (val pending = getPendingDecision()) {
                is SelectCardsDecision -> selectCards(pending.options.filter { nameOf(it) == castFirst })
                is ChooseTargetsDecision -> selectTargets(listOf(player2Id))
                null -> if (state.stack.isNotEmpty()) resolveStack() else break
                else -> error("Unexpected decision (the casts are mandatory): $pending")
            }
        }
    }

    init {
        test("exiles both targets and casts both copies for free, in the chosen order") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Spelltwine")
                .withLandsOnBattlefield(1, "Island", 6)
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInGraveyard(2, "Shock")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
            val shock = game.findCardsInGraveyard(2, "Shock").single()
            game.castSpelltwine(bolt, shock).error shouldBe null
            game.resolveStack()

            val order = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            order.options.map { game.nameOf(it) } shouldContainExactlyInAnyOrder listOf("Lightning Bolt", "Shock")
            game.resolveAll(castFirst = "Shock")

            game.getLifeTotal(2) shouldBe 15
            game.isInExile(1, "Lightning Bolt") shouldBe true
            game.isInExile(2, "Shock") shouldBe true
            game.isInExile(1, "Spelltwine") shouldBe true
            game.isInGraveyard(1, "Spelltwine") shouldBe false
        }

        test("the copy of the opponent's card is the caster's spell") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Spelltwine")
                .withLandsOnBattlefield(1, "Island", 6)
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInGraveyard(2, "Divination")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val handBefore = game.handSize(1)
            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
            val divination = game.findCardsInGraveyard(2, "Divination").single()
            game.castSpelltwine(bolt, divination).error shouldBe null
            game.resolveStack()
            game.resolveAll(castFirst = "Divination")

            // Spelltwine left the hand; the Divination copy drew two for its caster.
            game.handSize(1) shouldBe handBefore - 1 + 2
            game.handSize(2) shouldBe 0
            game.getLifeTotal(2) shouldBe 17
            game.isInExile(2, "Divination") shouldBe true
        }

        test("can't be cast without a target in an opponent's graveyard") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Spelltwine")
                .withLandsOnBattlefield(1, "Island", 6)
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInGraveyard(1, "Shock")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
            val ownShock = game.findCardsInGraveyard(1, "Shock").single()
            game.castSpelltwine(bolt, ownShock).error shouldNotBe null
        }
    }
}
