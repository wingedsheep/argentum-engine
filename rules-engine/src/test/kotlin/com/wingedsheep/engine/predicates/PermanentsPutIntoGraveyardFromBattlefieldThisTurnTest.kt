package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.TurnTracker
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

/**
 * `TurnTracker.PERMANENTS_PUT_INTO_GRAVEYARD_FROM_BATTLEFIELD` — every permanent type counts, only a
 * battlefield→graveyard move counts, and it is credited to the controller the permanent left under.
 */
class PermanentsPutIntoGraveyardFromBattlefieldThisTurnTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    val zones = ZoneTransitionService(
        com.wingedsheep.engine.registry.CardRegistry(),
        predicateEvaluator = PredicateEvaluator(cardRegistry = null),
    )

    fun GameTestDriver.move(entity: com.wingedsheep.sdk.model.EntityId, to: Zone) =
        replaceState(zones.moveToZone(state, entity, to).state)

    fun GameTestDriver.eval(amount: DynamicAmount): Int =
        PredicateEvaluator(cardRegistry = null).amounts.evaluate(state, amount, EffectContext(sourceId = null, controllerId = player1))

    test("zero when nothing has left the battlefield") {
        driver().eval(DynamicAmounts.permanentsPutIntoGraveyardFromBattlefieldThisTurn()) shouldBe 0
    }

    test("a land put into a graveyard from the battlefield counts — not only creatures and artifacts") {
        val d = driver()
        val forest = d.putLandOnBattlefield(d.player2, "Forest")
        d.move(forest, Zone.GRAVEYARD)
        d.eval(DynamicAmounts.permanentsPutIntoGraveyardFromBattlefieldThisTurn()) shouldBe 1
        d.eval(DynamicAmounts.permanentsPutIntoGraveyardFromBattlefieldThisTurn(Player.You)) shouldBe 0
        d.eval(DynamicAmounts.permanentsPutIntoGraveyardFromBattlefieldThisTurn(Player.EachOpponent)) shouldBe 1
    }

    test("leaving the battlefield for exile doesn't count") {
        val d = driver()
        val forest = d.putLandOnBattlefield(d.player1, "Forest")
        d.move(forest, Zone.EXILE)
        d.eval(DynamicAmounts.permanentsPutIntoGraveyardFromBattlefieldThisTurn()) shouldBe 0
    }

    test("a card put into a graveyard from another zone doesn't count") {
        val d = driver()
        val card = d.putCardInHand(d.player1, "Forest")
        d.move(card, Zone.GRAVEYARD)
        d.eval(DynamicAmounts.permanentsPutIntoGraveyardFromBattlefieldThisTurn()) shouldBe 0
    }

    test("round-trips through serialization") {
        val amount: DynamicAmount =
            DynamicAmount.TurnTracking(Player.Each, TurnTracker.PERMANENTS_PUT_INTO_GRAVEYARD_FROM_BATTLEFIELD)
        Json.decodeFromString(DynamicAmount.serializer(), Json.encodeToString(DynamicAmount.serializer(), amount)) shouldBe amount
    }
})
