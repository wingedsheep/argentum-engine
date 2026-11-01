package com.wingedsheep.ai.draftsim

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private data class HCard(
    override val name: String,
    override val manaCost: String,
    override val typeLine: String = "Creature — Beast",
    override val colors: List<String> = emptyList(),
    override val colorIdentity: List<String> = emptyList(),
    override val cmc: Double = DraftsimMana.cmc(manaCost),
    override val rarity: String? = "common",
) : ScorerCard

/**
 * Hate drafting ([DraftsimScorer.scoreBoosterAuto] with a pod size): once the pool is committed,
 * the AI takes a bomb it won't play instead of passing it, but only when the card it would
 * otherwise take adds little to its deck. A smaller pod raises the stakes, because the drafter
 * you pass to is more likely to be your opponent.
 */
class DraftsimHateDraftTest : FunSpec({

    val ratings = HashMap<String, Double>()
    fun card(name: String, cost: String, rating: Double): HCard {
        ratings[DraftsimData.nameKey(name)] = rating
        return HCard(name, cost)
    }

    // A committed red-green pool: 16 nonland cards, past the scorer's flexible stage.
    val committedPool = (1..8).map { card("Red Creature $it", "{1}{R}", 2.7) } +
        (1..8).map { card("Green Creature $it", "{2}{G}", 2.7) }
    val earlyPool = committedPool.take(5)

    val greenPlayable = card("Green Playable", "{2}{G}", 3.0)
    val greenGood = card("Green Good", "{2}{G}", 3.8)
    val greenFiller = card("Green Filler", "{2}{G}", 2.3)
    val greenBomb = card("Green Bomb", "{3}{G}", 4.2)
    val blackBomb = card("Black Bomb", "{2}{B}{B}", 4.5)
    val goldBomb = card("Gold Bomb", "{1}{U}{B}", 4.5)

    val scorer = DraftsimScorer(DraftsimSetTables(ratings, emptySet(), emptyMap()))

    fun pick(pack: List<ScorerCard>, pool: List<ScorerCard>, podSize: Int?): String {
        val scores = scorer.scoreBoosterAuto(pack, pool, podSize)
        return scorer.argmax(pack, scores)!!.name
    }

    test("without a pod size the scorer is unchanged: it takes the on-color playable") {
        pick(listOf(greenPlayable, blackBomb), committedPool, podSize = null) shouldBe "Green Playable"
    }

    test("in a two-player pod it keeps an off-color bomb from its only opponent over a playable") {
        val pack = listOf(greenPlayable, blackBomb)
        val scores = scorer.scoreBoosterAuto(pack, committedPool, podSize = 2)
        scorer.argmax(pack, scores)!!.name shouldBe "Black Bomb"
        scores.getValue("Black Bomb").summary!! shouldContain "Hate draft"
    }

    test("in a two-player pod it still takes a card that is good for its own deck") {
        pick(listOf(greenGood, blackBomb), committedPool, podSize = 2) shouldBe "Green Good"
    }

    test("in a full pod the same playable is worth more than denying the bomb") {
        pick(listOf(greenPlayable, blackBomb), committedPool, podSize = 8) shouldBe "Green Playable"
    }

    test("in a full pod it denies a bomb when the alternative is filler") {
        pick(listOf(greenFiller, goldBomb), committedPool, podSize = 8) shouldBe "Gold Bomb"
    }

    test("an unknown pod size is treated as a full pod") {
        pick(listOf(greenPlayable, blackBomb), committedPool, podSize = 0) shouldBe "Green Playable"
        pick(listOf(greenFiller, goldBomb), committedPool, podSize = 0) shouldBe "Gold Bomb"
    }

    test("early in the draft the color logic owns off-color bombs; no denial bonus") {
        val pack = listOf(greenPlayable, goldBomb)
        scorer.scoreBoosterAuto(pack, earlyPool, podSize = 2) shouldBe scorer.scoreBoosterAuto(pack, earlyPool, podSize = null)
    }

    test("it never trades an on-color bomb for denial") {
        pick(listOf(greenBomb, blackBomb), committedPool, podSize = 2) shouldBe "Green Bomb"
    }
})
