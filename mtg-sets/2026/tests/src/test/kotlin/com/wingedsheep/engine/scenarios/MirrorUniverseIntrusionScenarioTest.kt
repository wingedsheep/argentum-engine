package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.trk.cards.MirrorUniverseIntrusion
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Mirror Universe Intrusion (TRK #248).
 *
 * Unkicked: one menace token copy of the targeted creature an opponent controls. Kicked: the spell
 * targets an opponent instead (no creature target) and copies each creature that player controls,
 * every copy with menace — and the copies enter under the caster's control.
 */
class MirrorUniverseIntrusionScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(MirrorUniverseIntrusion))
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.tokens(player: EntityId): List<EntityId> =
        getCreatures(player).filter { state.getEntity(it)?.has<TokenComponent>() == true }

    fun GameTestDriver.nameOf(id: EntityId): String? = state.getEntity(id)?.get<CardComponent>()?.name

    fun GameTestDriver.cast(caster: EntityId, targets: List<ChosenTarget>, kicked: Boolean) {
        val islands = if (kicked) 7 else 2
        val swamps = if (kicked) 2 else 1
        repeat(islands) { putLandOnBattlefield(caster, "Island") }
        repeat(swamps) { putLandOnBattlefield(caster, "Swamp") }
        val spell = putCardInHand(caster, "Mirror Universe Intrusion")
        val result = submit(
            CastSpell(
                playerId = caster,
                cardId = spell,
                targets = targets,
                declaredCostSlot = if (kicked) ChoiceSlot.KICKED else null,
                paymentStrategy = PaymentStrategy.AutoPay
            )
        )
        withClue("Mirror Universe Intrusion should be castable: ${result.error}") {
            result.outcome shouldBe Outcome.Done
        }
        bothPass()
    }

    test("unkicked copies only the targeted creature, with menace, under the caster's control") {
        val d = driver()
        val caster = d.activePlayer!!
        val opponent = d.getOpponent(caster)
        val courser = d.putCreatureOnBattlefield(opponent, "Centaur Courser")
        d.putCreatureOnBattlefield(opponent, "Savannah Lions")

        d.cast(caster, listOf(ChosenTarget.Permanent(courser)), kicked = false)

        val tokens = d.tokens(caster)
        tokens.size shouldBe 1
        d.nameOf(tokens.single()) shouldBe "Centaur Courser"
        d.state.projectedState.hasKeyword(tokens.single(), Keyword.MENACE) shouldBe true
        d.tokens(opponent).size shouldBe 0
    }

    test("kicked copies every creature the target opponent controls, each with menace") {
        val d = driver()
        val caster = d.activePlayer!!
        val opponent = d.getOpponent(caster)
        d.putCreatureOnBattlefield(opponent, "Centaur Courser")
        d.putCreatureOnBattlefield(opponent, "Savannah Lions")
        // The caster's own creature is not copied.
        d.putCreatureOnBattlefield(caster, "Centaur Courser")

        d.cast(caster, listOf(ChosenTarget.Player(opponent)), kicked = true)

        val tokens = d.tokens(caster)
        tokens.map { d.nameOf(it) } shouldContainExactlyInAnyOrder listOf("Centaur Courser", "Savannah Lions")
        tokens.forEach { d.state.projectedState.hasKeyword(it, Keyword.MENACE) shouldBe true }
        d.tokens(opponent).size shouldBe 0
    }
})
