package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Dragon's Fire — {1}{R} Instant (AFR #139).
 *
 * "As an additional cost to cast this spell, you may reveal a Dragon card from your hand or choose a
 * Dragon you control. Dragon's Fire deals 3 damage to target creature or planeswalker. If you
 * revealed a Dragon card or chose a Dragon as you cast this spell, Dragon's Fire deals damage equal
 * to the power of that card or creature instead."
 *
 * Colossal Dreadmaw (6/6) is the target in every test so the exact damage stays readable.
 */
class DragonsFireScenarioTest : ScenarioTestBase() {

    private fun TestGame.damageOn(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    private fun TestGame.castFireChoosing(dragon: EntityId, target: EntityId): ExecutionResult {
        val fire = state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Dragon's Fire" }
        return execute(
            CastSpell(
                playerId = player1Id,
                cardId = fire,
                targets = listOf(ChosenTarget.Permanent(target)),
                declaredCostSlot = ChoiceSlot.ADDITIONAL_COST_PAID,
                additionalCostPayment = AdditionalCostPayment(beheldCards = listOf(dragon)),
            )
        )
    }

    private fun board() = scenario()
        .withPlayers("Caster", "Opponent")
        .withCardInHand(1, "Dragon's Fire")
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardOnBattlefield(2, "Colossal Dreadmaw")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("without a Dragon it deals 3 damage") {
            val game = board()
            val dreadmaw = game.findPermanent("Colossal Dreadmaw").shouldNotBeNull()

            game.castSpell(1, "Dragon's Fire", dreadmaw).error shouldBe null
            game.resolveStack()

            game.damageOn(dreadmaw) shouldBe 3
        }

        test("revealing a Dragon card from hand deals damage equal to its power, and the card stays in hand") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Dragon's Fire")
                .withCardInHand(1, "Shivan Dragon")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(2, "Colossal Dreadmaw")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val dreadmaw = game.findPermanent("Colossal Dreadmaw").shouldNotBeNull()
            val shivan = game.findCardsInHand(1, "Shivan Dragon").single()

            game.castFireChoosing(shivan, dreadmaw).error shouldBe null
            game.resolveStack()

            game.damageOn(dreadmaw) shouldBe 5
            game.isInHand(1, "Shivan Dragon") shouldBe true
        }

        test("choosing a Dragon you control uses its power even when that is less than 3") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Dragon's Fire")
                .withCardOnBattlefield(1, "Dragon Whelp")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(2, "Colossal Dreadmaw")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val dreadmaw = game.findPermanent("Colossal Dreadmaw").shouldNotBeNull()
            val whelp = game.findPermanent("Dragon Whelp").shouldNotBeNull()

            game.castFireChoosing(whelp, dreadmaw).error shouldBe null
            game.resolveStack()

            game.damageOn(dreadmaw) shouldBe 2
        }

        test("a chosen Dragon that dies in response still lends its last-known power") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Dragon's Fire")
                .withCardOnBattlefield(1, "Dragon Whelp")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardOnBattlefield(2, "Colossal Dreadmaw")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val dreadmaw = game.findPermanent("Colossal Dreadmaw").shouldNotBeNull()
            val whelp = game.findPermanent("Dragon Whelp").shouldNotBeNull()

            // Firebreathing once: a 3/3 on the battlefield, a printed 2/3 in the graveyard.
            val firebreathing = cardRegistry.getCard("Dragon Whelp")!!.activatedAbilities.single().id
            game.execute(ActivateAbility(game.player1Id, whelp, firebreathing)).error shouldBe null
            game.resolveStack()

            game.castFireChoosing(whelp, dreadmaw).error shouldBe null
            game.passPriority()
            game.castSpell(2, "Lightning Bolt", whelp).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Dragon Whelp") shouldBe true
            game.damageOn(dreadmaw) shouldBe 3
        }

        test("the behold variant is affordable only when you have a Dragon") {
            board().getLegalActions(1).filter { it.actionType == "CastWithKicker" }.all { !it.isAffordable } shouldBe true

            val withDragon = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Dragon's Fire")
                .withCardInHand(1, "Shivan Dragon")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(2, "Colossal Dreadmaw")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            withDragon.getLegalActions(1).filter { it.actionType == "CastWithKicker" }.any { it.isAffordable } shouldBe true
        }

        test("a non-Dragon can't pay the additional cost") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Dragon's Fire")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(2, "Colossal Dreadmaw")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val dreadmaw = game.findPermanent("Colossal Dreadmaw").shouldNotBeNull()
            val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

            game.castFireChoosing(bears, dreadmaw).error.shouldNotBeNull()
        }
    }
}
