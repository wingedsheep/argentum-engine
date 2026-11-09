package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

/**
 * `DynamicAmount.PermanentsSacrificedThisTurn(player, filter)` / `Conditions.YouSacrificedThisTurn` —
 * "if you sacrificed three or more Clues this turn" (Briarbridge Patrol). Each sacrifice is recorded
 * by the central sacrifice hook as the permanent stood just before it was sacrificed, so a token
 * that has since ceased to exist (CR 704.5d) still counts; the record is per sacrificing player and
 * resets at the start of the next turn.
 */
class PermanentsSacrificedThisTurnTest : FunSpec({
    val makeClues = CardDefinition.sorcery(
        name = "Test Make Clues",
        manaCost = ManaCost.parse("{0}"),
        oracleText = "Create three Clue tokens and a Food token.",
        script = CardScript.spell(Effects.Composite(listOf(Effects.CreateClue(3), Effects.CreateFood()))),
    )

    // The predefined tokens resolve their definitions by name, as the set module's token cards do.
    val clueToken = card("Clue") { typeLine = "Artifact — Clue" }
    val foodToken = card("Food") { typeLine = "Artifact — Food" }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(makeClues, clueToken, foodToken))
        initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    val evaluator = PredicateEvaluator(cardRegistry = null)
    val zones = ZoneTransitionService(com.wingedsheep.engine.registry.CardRegistry(), predicateEvaluator = evaluator)
    val clue = GameObjectFilter.Artifact.withSubtype("Clue")

    fun GameTestDriver.amount(amount: DynamicAmount): Int =
        evaluator.amounts.evaluate(state, amount, EffectContext(sourceId = null, controllerId = player1))

    fun GameTestDriver.sacrificedClues(atLeast: Int): Boolean =
        evaluator.conditions.evaluate(
            state,
            Conditions.YouSacrificedThisTurn(clue, atLeast),
            EffectContext(sourceId = null, controllerId = player1),
        )

    fun GameTestDriver.named(playerId: EntityId, name: String): List<EntityId> =
        getPermanents(playerId).filter { state.getEntity(it)?.get<CardComponent>()?.name == name }

    fun GameTestDriver.makeTokens(playerId: EntityId) {
        val id = putCardInHand(playerId, "Test Make Clues")
        castSpell(playerId, id).error shouldBe null
        bothPass()
    }

    fun GameTestDriver.sacrifice(playerId: EntityId, ids: List<EntityId>) {
        var s = ZoneTransitionService.trackPermanentSacrifice(state, ids, playerId)
        for (id in ids) s = zones.moveToZone(s, id, Zone.GRAVEYARD).state
        replaceState(s)
    }

    test("counts only the sacrificed permanents that match the filter") {
        val d = driver()
        d.makeTokens(d.player1)
        val clues = d.named(d.player1, "Clue")
        clues.size shouldBe 3
        val courser = d.putPermanentOnBattlefield(d.player1, "Centaur Courser")
        d.sacrifice(d.player1, clues.take(2) + d.named(d.player1, "Food") + courser)

        d.amount(DynamicAmounts.permanentsSacrificedThisTurn(clue)) shouldBe 2
        d.amount(DynamicAmounts.permanentsSacrificedThisTurn(GameObjectFilter.Creature)) shouldBe 1
        d.amount(DynamicAmounts.permanentsSacrificedThisTurn(GameObjectFilter.Artifact)) shouldBe 3
        d.amount(DynamicAmount.PermanentsSacrificedThisTurn()) shouldBe 4
        d.sacrificedClues(atLeast = 2) shouldBe true
        d.sacrificedClues(atLeast = 3) shouldBe false
    }

    test("sacrifices accumulate across the turn, and tokens that ceased to exist still count") {
        val d = driver()
        d.makeTokens(d.player1)
        val clues = d.named(d.player1, "Clue")
        d.sacrifice(d.player1, clues.take(1))
        d.sacrifice(d.player1, clues.drop(1))
        // State-based actions remove the tokens from the graveyard (CR 704.5d) on the way to the end step.
        d.passPriorityUntil(Step.END)
        clues.forEach { d.state.getEntity(it) shouldBe null }

        d.amount(DynamicAmounts.permanentsSacrificedThisTurn(clue)) shouldBe 3
        d.sacrificedClues(atLeast = 3) shouldBe true
    }

    test("controller-scoped, summed over a multi-player scope, and reset at the next turn") {
        val d = driver()
        d.makeTokens(d.player1)
        val theirCourser = d.putPermanentOnBattlefield(d.player2, "Centaur Courser")
        d.sacrifice(d.player2, listOf(theirCourser))
        d.sacrifice(d.player1, d.named(d.player1, "Clue"))

        d.amount(DynamicAmounts.permanentsSacrificedThisTurn(GameObjectFilter.Creature)) shouldBe 0
        d.amount(DynamicAmounts.permanentsSacrificedThisTurn(GameObjectFilter.Creature, Player.EachOpponent)) shouldBe 1
        d.amount(DynamicAmounts.permanentsSacrificedThisTurn(GameObjectFilter.Any, Player.Each)) shouldBe 4

        d.passPriorityUntil(Step.UPKEEP)
        d.amount(DynamicAmounts.permanentsSacrificedThisTurn(GameObjectFilter.Any, Player.Each)) shouldBe 0
        d.sacrificedClues(atLeast = 1) shouldBe false
    }

    test("round-trips through serialization") {
        val amount: DynamicAmount = DynamicAmount.PermanentsSacrificedThisTurn(Player.EachOpponent, clue)
        Json.decodeFromString(DynamicAmount.serializer(), Json.encodeToString(DynamicAmount.serializer(), amount)) shouldBe amount
    }
})
