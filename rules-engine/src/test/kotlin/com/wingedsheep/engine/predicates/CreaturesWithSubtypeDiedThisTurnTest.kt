package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.player.CreatureSubtypesDiedThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

/**
 * [DynamicAmount.CreaturesWithSubtypeDiedThisTurn] reads the per-death subtype record
 * ([CreatureSubtypesDiedThisTurnComponent]), one entry per creature that died, credited to the
 * controller it died under.
 */
class CreaturesWithSubtypeDiedThisTurnTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.record(player: com.wingedsheep.sdk.model.EntityId, vararg deaths: Set<String>) =
        replaceState(state.updateEntity(player) { it.with(CreatureSubtypesDiedThisTurnComponent(deaths.toList())) })

    fun GameTestDriver.eval(amount: DynamicAmount): Int =
        PredicateEvaluator(cardRegistry = null).amounts.evaluate(state, amount, EffectContext(sourceId = null, controllerId = player1))

    val zubera = Subtype("Zubera")

    test("zero when nothing has died") {
        driver().eval(DynamicAmounts.creaturesWithSubtypeDiedThisTurn(zubera)) shouldBe 0
    }

    test("game-wide by default: sums every player's matching deaths and skips the rest") {
        val d = driver()
        d.record(d.player1, setOf("Zubera", "Spirit"), setOf("Bear"))
        d.record(d.player2, setOf("Zubera", "Spirit"))
        d.eval(DynamicAmounts.creaturesWithSubtypeDiedThisTurn(zubera)) shouldBe 2
    }

    test("a player-scoped count only reads deaths under that player's control") {
        val d = driver()
        d.record(d.player1, setOf("Zubera", "Spirit"))
        d.record(d.player2, setOf("Zubera", "Spirit"), setOf("Zubera"))
        d.eval(DynamicAmounts.creaturesWithSubtypeDiedThisTurn(zubera, Player.You)) shouldBe 1
        d.eval(DynamicAmounts.creaturesWithSubtypeDiedThisTurn(zubera, Player.EachOpponent)) shouldBe 2
    }

    test("a death carrying the subtype counts once regardless of its other subtypes") {
        val d = driver()
        d.record(d.player1, setOf("Spirit", "Zubera", "Warrior"))
        d.eval(DynamicAmounts.creaturesWithSubtypeDiedThisTurn(Subtype("Spirit"))) shouldBe 1
        d.eval(DynamicAmounts.creaturesWithSubtypeDiedThisTurn(zubera)) shouldBe 1
    }

    test("round-trips through serialization") {
        val amount: DynamicAmount = DynamicAmount.CreaturesWithSubtypeDiedThisTurn(zubera, Player.You)
        Json.decodeFromString(DynamicAmount.serializer(), Json.encodeToString(DynamicAmount.serializer(), amount)) shouldBe amount
    }
})
