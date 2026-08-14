package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * How [CardAdvantage] prices a land in hand — the two flags that have answered it, in order.
 *
 * `AiProfile.landDropIsNotCardLoss` came first: a land drop relocates a card, it does not spend one,
 * so hold one land per land drop back from the count and the drop comes out neutral. It works, and
 * it buys that neutrality at the wrong end — the land is priced at **zero**, so a hand of one Forest
 * scores what an empty hand scores and an opponent's Duress on it is free.
 *
 * `AiProfile.priceLandsInHandAsMana` replaces it with the model stated plainly: *a land on the
 * battlefield is worth more than a land in your hand, a land in your hand is still worth something,
 * and it is worth more when you are short of mana than when you are already rich.* The land drop is
 * then positive by construction and no earmark is needed. Both are pinned here, the older one first,
 * because the second is only interesting against what the first got wrong.
 *
 * Asserted on [CardAdvantage] alone wherever possible, because the claim is about this one feature
 * and a composite would let [Tempo] and `BoardPresence` cover a wrong number here with a right one
 * there. The one exception is marked: "playing a land is a gain" is a claim *about* two features
 * disagreeing in the right direction, so it has to be made through the whole evaluator.
 *
 * That these move the *decision* is `PuzzleSuiteTest`'s `sequencing-02`.
 */
class CardAdvantageLandDropTest : FunSpec({

    val registry = CardRegistry().apply { register(TestCards.all) }
    val penalty = -2.0 // what `concave-hand-2`, and therefore the live profile, charges

    /** Every constant here is a sum of tenths; the tolerance is about binary floats, not slack. */
    val EPSILON = 1e-9

    fun boot(): GameState = GameInitializer(registry).initializeGame(
        GameConfig(
            players = (1..2).map { PlayerConfig("P$it", Deck.of("Forest" to 30, "Grizzly Bears" to 10)) },
            skipMulligans = true,
            startingPlayerIndex = 0,
            seed = 424242L,
        )
    ).state

    /** Empty both hands, so a test's own [draw]s are the whole hand. */
    fun GameState.withEmptyHands(): GameState = turnOrder.fold(this) { state, playerId ->
        state.getZone(playerId, Zone.HAND).fold(state) { s, cardId ->
            s.removeFromZone(ZoneKey(playerId, Zone.HAND), cardId)
                .addToZone(ZoneKey(playerId, Zone.LIBRARY), cardId)
        }
    }

    /** Move one [cardName] from [playerId]'s library into their hand. */
    fun GameState.draw(playerId: EntityId, cardName: String): GameState {
        val cardId = getZone(playerId, Zone.LIBRARY)
            .first { getEntity(it)?.get<CardComponent>()?.name == cardName }
        return removeFromZone(ZoneKey(playerId, Zone.LIBRARY), cardId)
            .addToZone(ZoneKey(playerId, Zone.HAND), cardId)
    }

    /** Play [playerId]'s first land from hand, spending the land drop — what `PlayLandHandler` does. */
    fun GameState.playLand(playerId: EntityId): GameState {
        val cardId = getZone(playerId, Zone.HAND)
            .first { getEntity(it)?.get<CardComponent>()?.isLand == true }
        return removeFromZone(ZoneKey(playerId, Zone.HAND), cardId)
            .addToZone(ZoneKey(playerId, Zone.BATTLEFIELD), cardId)
            .updateEntity(cardId) { it.with(ControllerComponent(playerId)) }
            .updateEntity(playerId) { it.with(it.get<LandDropsComponent>()!!.use()) }
    }

    fun GameState.cards(playerId: EntityId, landDropIsNotCardLoss: Boolean): Double =
        CardAdvantage.score(this, projectedState, playerId, penalty, landDropIsNotCardLoss)

    test("off, a land drop is charged as card loss — the historical behaviour") {
        val before = boot().withEmptyHands().let { it.draw(it.turnOrder[0], "Forest") }
        val me = before.turnOrder[0]
        val after = before.playLand(me)

        withClue("hand 1 -> 0 costs the whole first-card marginal, which is the cliff") {
            before.cards(me, landDropIsNotCardLoss = false) shouldBe 3.0
            after.cards(me, landDropIsNotCardLoss = false) shouldBe 0.0
        }
    }

    test("on, a land drop is exactly card-neutral") {
        val before = boot().withEmptyHands().let { it.draw(it.turnOrder[0], "Forest") }
        val me = before.turnOrder[0]
        val after = before.playLand(me)

        after.cards(me, landDropIsNotCardLoss = true) shouldBe before.cards(me, landDropIsNotCardLoss = true)
    }

    test("on, only one land is held back — the second is a card again") {
        // Two lands in hand and one drop: the drop earmarks one of them, so the *other* is worth a
        // card. Without the cap this would read a hand of nothing but lands as topdeck mode.
        val base = boot().withEmptyHands()
        val me = base.turnOrder[0]
        val oneLand = base.draw(me, "Forest")
        val twoLands = oneLand.draw(me, "Forest")

        twoLands.cards(me, landDropIsNotCardLoss = true) shouldBeGreaterThan
            oneLand.cards(me, landDropIsNotCardLoss = true)

        withClue("after the drop is spent, the land left in hand counts like any other card") {
            val played = twoLands.playLand(me)
            played.cards(me, landDropIsNotCardLoss = true) shouldBe
                oneLand.cards(me, landDropIsNotCardLoss = false)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // `AiProfile.priceLandsInHandAsMana` — the model that replaces the earmark
    // ═══════════════════════════════════════════════════════════════════════
    //
    // "A land on the battlefield is worth more than a land in your hand, a land in your hand is
    // still worth something, and it is worth more when you are short of mana than when you are
    // already rich." The earmark above got the land drop to come out neutral by pricing the land at
    // *zero*, flat. This prices it on a curve and lets the land drop be positive on its own.

    fun GameState.hand(playerId: EntityId): Double =
        CardAdvantage.score(this, projectedState, playerId, penalty, priceLandsInHandAsMana = true)

    /** Put [count] Mountains onto [playerId]'s battlefield without spending a land drop. */
    fun GameState.withLandsInPlay(playerId: EntityId, count: Int): GameState =
        (1..count).fold(this) { s, _ ->
            val cardId = s.getZone(playerId, Zone.LIBRARY)
                .first { s.getEntity(it)?.get<CardComponent>()?.isLand == true }
            s.removeFromZone(ZoneKey(playerId, Zone.LIBRARY), cardId)
                .addToZone(ZoneKey(playerId, Zone.BATTLEFIELD), cardId)
                .updateEntity(cardId) { it.with(ControllerComponent(playerId)) }
        }

    test("a land in hand is worth something — an opponent's Duress is not free") {
        // The claim the earmark gets wrong, stated as the case that proves it. Under
        // `landDropIsNotCardLoss` a hand of one Forest scores exactly what an empty hand scores, so
        // stripping it costs its victim nothing at all.
        val base = boot().withEmptyHands()
        val me = base.turnOrder[0]
        val oneLand = base.draw(me, "Forest")

        withClue("the old model prices the Duress at zero") {
            oneLand.cards(me, landDropIsNotCardLoss = true) shouldBe base.cards(me, landDropIsNotCardLoss = true)
        }
        withClue("the new one prices it at a real, positive amount") {
            oneLand.hand(me) shouldBeGreaterThan base.hand(me)
        }
    }

    test("a land is worth more when you are short of mana than when you are rich") {
        // Vincent's refinement, and the reason this is a schedule rather than a constant. Same card,
        // same hand, different amount of mana already on the battlefield.
        val base = boot().withEmptyHands()
        val me = base.turnOrder[0]

        fun landWorthAt(landsInPlay: Int): Double {
            val board = base.withLandsInPlay(me, landsInPlay)
            return board.draw(me, "Forest").hand(me) - board.hand(me)
        }

        withClue("mana-screwed -> mid-game -> mana-rich is strictly decreasing") {
            landWorthAt(1) shouldBeGreaterThan landWorthAt(4)
            landWorthAt(4) shouldBeGreaterThan landWorthAt(8)
        }
        withClue("and never reaches zero, however rich — it is still a card") {
            landWorthAt(12) shouldBeGreaterThan 0.0
        }
    }

    test("every rung stays below what the same land is worth on the battlefield") {
        // The property that makes the land drop positive by construction, so nothing has to force
        // it. Checked against `Tempo` and `BoardPresence`'s own numbers rather than against a
        // remembered constant, because that is where the bound actually comes from.
        val weights = EvaluationWeights(topdeckPenalty = penalty)
        listOf(0, 2, 3, 5, 6, 10).forEach { landsInPlay ->
            val fieldGain = weights.boardPresence * 0.6 +
                weights.tempo * (Tempo.landValueAt(landsInPlay + 1) - Tempo.landValueAt(landsInPlay))
            withClue("$landsInPlay lands in play") {
                weights.cardAdvantage * CardAdvantage.landInHandValueAt(landsInPlay) shouldBeLessThan fieldGain
            }
        }
    }

    test("a land in hand is worth less than a spell in hand, once the curve is filled out") {
        // Not asserted at zero lands on purpose: with no mana at all a land really is the better
        // card to be holding, and a model that claimed otherwise would be wrong rather than safe.
        val base = boot().withEmptyHands().let { it.withLandsInPlay(it.turnOrder[0], 4) }
        val me = base.turnOrder[0]

        base.draw(me, "Forest").hand(me) shouldBeLessThan base.draw(me, "Grizzly Bears").hand(me)
    }

    test("playing a land is a strict gain across the whole evaluator") {
        // The two sides of the trade live in different features, which is the entire point of the
        // model — so this is the one assertion that has to be made through a composite.
        val base = boot().withEmptyHands()
        val me = base.turnOrder[0]
        val held = base.draw(me, "Forest")
        val played = held.playLand(me)
        val evaluator = EvaluationWeights(topdeckPenalty = penalty).toEvaluator(priceLandsInHandAsMana = true)

        evaluator.evaluate(played, played.projectedState, me) shouldBeGreaterThan
            evaluator.evaluate(held, held.projectedState, me)
    }

    test("seven lands in hand is a flooded hand, not an excellent one") {
        // What the old model cannot see. Past the one earmarked land it counts lands as full cards,
        // so a hand of nothing but lands rides the same curve as a hand full of business. Pricing
        // each land at the count it would *arrive* at is what makes the seventh cheap.
        val base = boot().withEmptyHands()
        val me = base.turnOrder[0]
        val flooded = (1..7).fold(base) { s, _ -> s.draw(me, "Forest") }
        val business = (1..7).fold(base) { s, _ -> s.draw(me, "Grizzly Bears") }

        withClue("old: the flood scores within one card of a hand full of business — 8.4 vs 9.2") {
            business.cards(me, landDropIsNotCardLoss = false) -
                flooded.cards(me, landDropIsNotCardLoss = true) shouldBeLessThan 1.0
        }
        withClue("new: it loses more than half of that — 4.4 vs 9.2") {
            business.hand(me) - flooded.hand(me) shouldBeGreaterThan 4.0
        }
        withClue("and the seventh land is worth far less than the first") {
            val sixth = (1..6).fold(base) { s, _ -> s.draw(me, "Forest") }
            val fifth = (1..5).fold(base) { s, _ -> s.draw(me, "Forest") }
            (flooded.hand(me) - sixth.hand(me)) shouldBeLessThan
                (base.draw(me, "Forest").hand(me) - base.hand(me))
            sixth.hand(me) shouldBeGreaterThan fifth.hand(me)
        }
    }

    test("business is still priced on the curve — lands do not dilute it") {
        // The negative control. Adding lands to a hand must not change what the spells in it are
        // worth, or this becomes a general discount on holding cards.
        val base = boot().withEmptyHands()
        val me = base.turnOrder[0]
        val spells = (1..3).fold(base) { s, _ -> s.draw(me, "Grizzly Bears") }
        val spellsAndLands = (1..3).fold(spells) { s, _ -> s.draw(me, "Forest") }
        val landsOnly = (1..3).fold(base) { s, _ -> s.draw(me, "Forest") }

        withClue("the lands add exactly what they add on their own — no interaction with the curve") {
            spellsAndLands.hand(me) - spells.hand(me) shouldBe
                (landsOnly.hand(me) - base.hand(me) plusOrMinus EPSILON)
        }
    }

    test("the two flags do not stack — the model supersedes the earmark") {
        // `landDropIsNotCardLoss` stays set on the promotion candidate and must simply not be read,
        // so that reverting is one line and the diff against the baseline is one flag.
        val base = boot().withEmptyHands()
        val me = base.turnOrder[0]
        val held = base.draw(me, "Forest")

        CardAdvantage.score(
            held, held.projectedState, me, penalty,
            landDropIsNotCardLoss = true, priceLandsInHandAsMana = true,
        ) shouldBe CardAdvantage.score(
            held, held.projectedState, me, penalty,
            landDropIsNotCardLoss = false, priceLandsInHandAsMana = true,
        )
    }

    test("on, a spell is still a card — casting one still costs card advantage") {
        val base = boot().withEmptyHands()
        val me = base.turnOrder[0]
        val withSpell = base.draw(me, "Grizzly Bears")

        withClue("the earmark is for lands only; a 2/2 in hand is not mana") {
            withSpell.cards(me, landDropIsNotCardLoss = true) shouldBe
                withSpell.cards(me, landDropIsNotCardLoss = false)
        }
        withClue("and spending it still steps off the cliff") {
            base.cards(me, landDropIsNotCardLoss = true) shouldBeLessThan
                withSpell.cards(me, landDropIsNotCardLoss = true)
        }
    }
})
