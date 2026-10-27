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
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.TurnTracker
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

/**
 * The nontoken slices of two per-turn trackers:
 *  - `TurnTracker.NONTOKEN_CREATURES_ENTERED_UNDER_CONTROL` — "the number of nontoken creatures you
 *    had enter the battlefield under your control this turn" (Gyome, Master Chef). Counted per entry
 *    and kept after the creature leaves (Gyome's 2021-04-16 ruling); tokens and noncreatures don't
 *    count.
 *  - `TurnTracker.NONTOKEN_PERMANENTS_SACRIFICED` / `Conditions.YouSacrificedNontokenPermanentsThisTurn`
 *    — "as long as you've sacrificed a nontoken permanent this turn" (Evendo Brushrazer). Recorded by
 *    the central sacrifice hook, controller-scoped, reset at end of turn.
 */
class NontokenTurnTrackersTest : FunSpec({
    val makeToken = CardDefinition.sorcery(
        name = "Test Make Token",
        manaCost = ManaCost.parse("{0}"),
        oracleText = "Create a 1/1 Soldier creature token.",
        script = CardScript.spell(Effects.CreateToken(power = 1, toughness = 1, creatureTypes = setOf("Soldier"))),
    )

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(makeToken))
        initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    val evaluator = PredicateEvaluator(cardRegistry = null)
    val zones = ZoneTransitionService(com.wingedsheep.engine.registry.CardRegistry(), predicateEvaluator = evaluator)

    fun GameTestDriver.amount(amount: DynamicAmount): Int =
        evaluator.amounts.evaluate(state, amount, EffectContext(sourceId = null, controllerId = player1))

    fun GameTestDriver.sacrificedNontoken(atLeast: Int = 1): Boolean =
        evaluator.conditions.evaluate(
            state,
            Conditions.YouSacrificedNontokenPermanentsThisTurn(atLeast),
            EffectContext(sourceId = null, controllerId = player1),
        )

    fun GameTestDriver.enter(playerId: EntityId, card: String): EntityId {
        val id = putCardInHand(playerId, card)
        replaceState(zones.moveToZone(state, id, Zone.BATTLEFIELD).state)
        return id
    }

    fun GameTestDriver.makeToken(): EntityId {
        val id = putCardInHand(player1, "Test Make Token")
        castSpell(player1, id).error shouldBe null
        bothPass()
        return getPermanents(player1).single { state.getEntity(it)?.has<TokenComponent>() == true }
    }

    fun GameTestDriver.sacrifice(playerId: EntityId, ids: List<EntityId>) {
        var s = ZoneTransitionService.trackPermanentSacrifice(state, ids, playerId)
        for (id in ids) s = zones.moveToZone(s, id, Zone.GRAVEYARD).state
        replaceState(s)
    }

    val nontokenEntered = DynamicAmounts.nonTokenCreaturesEnteredThisTurn()
    val nontokenSacrificed = DynamicAmount.TurnTracking(Player.You, TurnTracker.NONTOKEN_PERMANENTS_SACRIFICED)
    val allSacrificed = DynamicAmount.TurnTracking(Player.You, TurnTracker.PERMANENTS_SACRIFICED)

    context("nontoken creatures entered") {
        test("counts nontoken creatures only — tokens and noncreature permanents don't count") {
            val d = driver()
            d.enter(d.player1, "Centaur Courser")
            d.enter(d.player1, "Forest")
            d.makeToken()
            d.amount(nontokenEntered) shouldBe 1
            d.amount(DynamicAmount.TurnTracking(Player.You, TurnTracker.CREATURES_ENTERED_UNDER_CONTROL)) shouldBe 2
        }

        test("still counted after the creature leaves; a re-entry counts again (CR 400.7)") {
            val d = driver()
            val courser = d.enter(d.player1, "Centaur Courser")
            d.replaceState(zones.moveToZone(d.state, courser, Zone.HAND).state)
            d.amount(nontokenEntered) shouldBe 1
            d.replaceState(zones.moveToZone(d.state, courser, Zone.BATTLEFIELD).state)
            d.amount(nontokenEntered) shouldBe 2
        }

        test("controller-scoped and reset at end of turn") {
            val d = driver()
            d.enter(d.player2, "Centaur Courser")
            d.amount(nontokenEntered) shouldBe 0
            d.amount(DynamicAmounts.nonTokenCreaturesEnteredThisTurn(Player.EachOpponent)) shouldBe 1
            d.enter(d.player1, "Centaur Courser")
            d.passPriorityUntil(Step.UPKEEP)
            d.amount(DynamicAmounts.nonTokenCreaturesEnteredThisTurn(Player.Each)) shouldBe 0
        }
    }

    context("nontoken permanents sacrificed") {
        test("nothing sacrificed — the condition is false") {
            val d = driver()
            d.sacrificedNontoken() shouldBe false
        }

        test("sacrificing a token counts toward all sacrifices but not the nontoken slice") {
            val d = driver()
            val token = d.makeToken()
            d.sacrifice(d.player1, listOf(token))
            d.amount(allSacrificed) shouldBe 1
            d.amount(nontokenSacrificed) shouldBe 0
            d.sacrificedNontoken() shouldBe false
        }

        test("sacrificing nontoken permanents of any type counts each one") {
            val d = driver()
            val token = d.makeToken()
            val courser = d.enter(d.player1, "Centaur Courser")
            val forest = d.enter(d.player1, "Forest")
            d.sacrifice(d.player1, listOf(token, courser, forest))
            d.amount(allSacrificed) shouldBe 3
            d.amount(nontokenSacrificed) shouldBe 2
            d.sacrificedNontoken(atLeast = 2) shouldBe true
            d.sacrificedNontoken(atLeast = 3) shouldBe false
        }

        test("an opponent's sacrifice is theirs, and the record resets at end of turn") {
            val d = driver()
            val theirs = d.enter(d.player2, "Centaur Courser")
            d.sacrifice(d.player2, listOf(theirs))
            d.sacrificedNontoken() shouldBe false
            d.amount(DynamicAmount.TurnTracking(Player.EachOpponent, TurnTracker.NONTOKEN_PERMANENTS_SACRIFICED)) shouldBe 1
            val mine = d.enter(d.player1, "Centaur Courser")
            d.sacrifice(d.player1, listOf(mine))
            d.sacrificedNontoken() shouldBe true
            d.passPriorityUntil(Step.UPKEEP)
            d.sacrificedNontoken() shouldBe false
        }
    }

    test("round-trips through serialization") {
        for (tracker in listOf(TurnTracker.NONTOKEN_CREATURES_ENTERED_UNDER_CONTROL, TurnTracker.NONTOKEN_PERMANENTS_SACRIFICED)) {
            val amount: DynamicAmount = DynamicAmount.TurnTracking(Player.You, tracker)
            Json.decodeFromString(DynamicAmount.serializer(), Json.encodeToString(DynamicAmount.serializer(), amount)) shouldBe amount
        }
    }
})
