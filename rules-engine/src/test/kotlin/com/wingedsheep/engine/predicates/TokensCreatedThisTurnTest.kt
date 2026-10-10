package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.TurnTracker
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

/**
 * `TurnTracker.TOKENS_CREATED` / `Conditions.CreatedTokenThisTurn` — "if you created a token this
 * turn" (Idol of Oblivion).
 *
 * The player who creates a token is its owner and it enters under that player's control (CR 111.2),
 * so the tracker is the token slice of the per-player entry log: a nontoken entry doesn't count, a
 * token another player creates is theirs, and the count is turn history — the token leaving doesn't
 * undo it, and it resets at end of turn.
 */
class TokensCreatedThisTurnTest : FunSpec({
    val makeToken = CardDefinition.sorcery(
        name = "Test Make Token",
        manaCost = ManaCost.parse("{0}"),
        oracleText = "Create a 1/1 Soldier creature token.",
        script = CardScript.spell(Effects.CreateToken(power = 1, toughness = 1, creatureTypes = setOf("Soldier"))),
    )
    val giveToken = CardDefinition.sorcery(
        name = "Test Give Token",
        manaCost = ManaCost.parse("{0}"),
        oracleText = "Target opponent creates a 1/1 Soldier creature token.",
        script = CardScript.spell(
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                creatureTypes = setOf("Soldier"),
                controller = EffectTarget.PlayerRef(Player.EachOpponent),
            )
        ),
    )

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(makeToken, giveToken))
        initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    val evaluator = PredicateEvaluator(cardRegistry = null)
    val zones = ZoneTransitionService(com.wingedsheep.engine.registry.CardRegistry(), predicateEvaluator = evaluator)

    fun GameTestDriver.created(player: Player): Int =
        evaluator.amounts.evaluate(
            state,
            DynamicAmount.TurnTracking(player, TurnTracker.TOKENS_CREATED),
            EffectContext(sourceId = null, controllerId = player1),
        )

    fun GameTestDriver.holds(condition: Condition): Boolean =
        evaluator.conditions.evaluate(state, condition, EffectContext(sourceId = null, controllerId = player1))

    fun GameTestDriver.cast(card: String) {
        val id = putCardInHand(player1, card)
        castSpell(player1, id).error shouldBe null
        bothPass()
    }

    fun GameTestDriver.tokensOf(playerId: EntityId) =
        getPermanents(playerId).filter { state.getEntity(it)?.has<TokenComponent>() == true }

    test("nothing created yet — the condition is false") {
        val d = driver()
        d.created(Player.You) shouldBe 0
        d.holds(Conditions.CreatedTokenThisTurn()) shouldBe false
    }

    test("creating a token counts for its creator, not for the opponent") {
        val d = driver()
        d.cast("Test Make Token")
        d.tokensOf(d.player1).size shouldBe 1
        d.created(Player.You) shouldBe 1
        d.created(Player.EachOpponent) shouldBe 0
        d.holds(Conditions.CreatedTokenThisTurn()) shouldBe true
        d.holds(Conditions.CreatedTokenThisTurn(atLeast = 2)) shouldBe false
    }

    test("a nontoken permanent entering doesn't count") {
        val d = driver()
        val courser = d.putCardInHand(d.player1, "Centaur Courser")
        d.replaceState(zones.moveToZone(d.state, courser, Zone.BATTLEFIELD).state)
        d.created(Player.You) shouldBe 0
    }

    test("a token another player creates is theirs (CR 111.2), even when your spell made it") {
        val d = driver()
        d.cast("Test Give Token")
        d.tokensOf(d.player2).size shouldBe 1
        d.created(Player.You) shouldBe 0
        d.created(Player.EachOpponent) shouldBe 1
        d.holds(Conditions.CreatedTokenThisTurn()) shouldBe false
    }

    test("turn history — the token leaving the battlefield doesn't undo it") {
        val d = driver()
        d.cast("Test Make Token")
        val token = d.tokensOf(d.player1).single()
        d.replaceState(zones.moveToZone(d.state, token, Zone.GRAVEYARD).state)
        d.tokensOf(d.player1).size shouldBe 0
        d.holds(Conditions.CreatedTokenThisTurn()) shouldBe true
    }

    test("resets at end of turn") {
        val d = driver()
        d.cast("Test Make Token")
        d.passPriorityUntil(Step.UPKEEP)
        d.created(Player.You) shouldBe 0
        d.created(Player.Each) shouldBe 0
    }

    test("round-trips through serialization") {
        val amount: DynamicAmount = DynamicAmount.TurnTracking(Player.You, TurnTracker.TOKENS_CREATED)
        Json.decodeFromString(DynamicAmount.serializer(), Json.encodeToString(DynamicAmount.serializer(), amount)) shouldBe amount
    }
})
