package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CoinFlipEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.CoinFlipModifiers
import com.wingedsheep.engine.handlers.effects.composite.FlipCoinExecutor
import com.wingedsheep.engine.handlers.effects.composite.FlipCoinsExecutor
import com.wingedsheep.engine.handlers.effects.composite.FlipTwoCoinsExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import com.wingedsheep.sdk.scripting.FlipAdditionalCoins
import com.wingedsheep.sdk.scripting.WinCoinFlips
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.FlipCoinEffect
import com.wingedsheep.sdk.scripting.effects.FlipCoinsEffect
import com.wingedsheep.sdk.scripting.effects.FlipTwoCoinsEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * [FlipAdditionalCoins] — "If you would flip a coin, instead flip two coins and ignore one"
 * (Krark's Thumb).
 *
 * The rules matrix here is taken from the card's own rulings, which are what make this a *per-coin*
 * replacement rather than a per-instruction one:
 *
 * - "If an effect tells you to flip more than one coin at once, this replaces each individual coin
 *   flip … you flip two coins, flip two coins, and then ignore one flip from each pair."
 * - "You will know the results of all simultaneous flips before choosing which to ignore."
 * - Two Thumbs flip four coins per flip and ignore three (the replacement applies to each coin the
 *   previous one produced, so instances multiply).
 *
 * Plus the interaction with the other coin-flip replacement the engine has: a
 * [WinCoinFlips] forced win makes every coin heads, which leaves nothing to ignore.
 */
class FlipAdditionalCoinsTest : FunSpec({

    /** A Krark's Thumb stand-in: the static under test and nothing else. */
    val thumb = card("Test Thumb") {
        typeLine = "Legendary Artifact"
        manaCost = "{2}"
        oracleText = "If you would flip a coin, instead flip two coins and ignore one."
        staticAbility { ability = FlipAdditionalCoins(coinsPerFlip = 2) }
    }

    /** "Flip three coins and ignore two" — proves the count is a parameter, not a constant. */
    val tripleThumb = card("Test Triple Thumb") {
        typeLine = "Legendary Artifact"
        manaCost = "{2}"
        oracleText = "If you would flip a coin, instead flip three coins and ignore two."
        staticAbility { ability = FlipAdditionalCoins(coinsPerFlip = 3) }
    }

    /** Edgar's Two-Headed Coin, so the two replacements can be tested together. */
    val luckyCoin = card("Test Lucky Coin") {
        typeLine = "Artifact"
        manaCost = "{2}"
        oracleText = "You win all coin flips."
        staticAbility { ability = WinCoinFlips() }
    }

    /** A spell whose two halves are far enough apart in life to identify which one ran. */
    val coinToss = card("Test Coin Toss") {
        typeLine = "Instant"
        manaCost = "{R}"
        oracleText = "Flip a coin. If you win the flip, you gain 10 life. If you lose the flip, you lose 3 life."
        spell {
            effect = FlipCoinEffect(
                wonEffect = Effects.GainLife(10),
                lostEffect = Effects.LoseLife(3, EffectTarget.Controller)
            )
        }
    }

    val extraCards = listOf(thumb, tripleThumb, luckyCoin, coinToss)

    fun driverWith(vararg battlefield: String): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        extraCards.forEach { driver.registerCard(it) }
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        battlefield.forEach { driver.putPermanentOnBattlefield(player, it) }
        return driver to player
    }

    /**
     * Run [effect] on a seeded state and hand back the result.
     *
     * Seeds are searched rather than hard-coded because what the test cares about is *which shape*
     * of batch came up (mixed or unanimous), not any particular coin.
     */
    fun flipWithSeed(
        driver: GameTestDriver,
        player: EntityId,
        effect: Effect,
        seed: Long
    ): EffectResult {
        val seeded = driver.state.copy(rng = GameRng.seeded(seed))
        val context = EffectContext(sourceId = null, controllerId = player)
        val noSubEffects: (GameState, Effect, EffectContext) -> EffectResult =
            { s, _, _ -> EffectResult.success(s) }
        return when (effect) {
            is FlipCoinEffect ->
                FlipCoinExecutor(driver.cardRegistry, noSubEffects).execute(seeded, effect, context)
            is FlipTwoCoinsEffect ->
                FlipTwoCoinsExecutor(driver.cardRegistry, noSubEffects).execute(seeded, effect, context)
            is FlipCoinsEffect ->
                FlipCoinsExecutor(driver.cardRegistry).execute(seeded, effect, context)
            else -> error("not a flip effect")
        }
    }

    /** The first seed in [range] whose flip of [effect] paused (i.e. produced a mixed batch). */
    fun seedThatPauses(driver: GameTestDriver, player: EntityId, effect: Effect, range: LongRange) =
        range.firstOrNull { flipWithSeed(driver, player, effect, it).isPaused }

    /** The first seed in [range] whose flip of [effect] did not pause (a unanimous batch). */
    fun seedThatSettles(driver: GameTestDriver, player: EntityId, effect: Effect, range: LongRange) =
        range.firstOrNull { !flipWithSeed(driver, player, effect, it).isPaused }

    // ── How many coins are flipped ───────────────────────────────────────

    test("without the replacement a single flip is one coin and never asks anything") {
        val (driver, player) = driverWith()

        CoinFlipModifiers.coinsPerFlip(driver.state, driver.cardRegistry, player) shouldBe 1

        (1L..40L).forEach { seed ->
            val result = flipWithSeed(driver, player, FlipCoinEffect(), seed)
            result.isPaused shouldBe false
            val flips = result.events.filterIsInstance<CoinFlipEvent>()
            flips.size shouldBe 1
            flips.single().ignored shouldBe false
        }
    }

    test("one Thumb flips two coins for one flip and ignores exactly one of them") {
        val (driver, player) = driverWith("Test Thumb")

        CoinFlipModifiers.coinsPerFlip(driver.state, driver.cardRegistry, player) shouldBe 2

        val seed = seedThatSettles(driver, player, FlipCoinEffect(), 1L..200L)
        seed.shouldNotBeNull()
        val flips = flipWithSeed(driver, player, FlipCoinEffect(), seed)
            .events.filterIsInstance<CoinFlipEvent>()

        flips.size shouldBe 2
        flips.count { !it.ignored } shouldBe 1
    }

    test("instances multiply: two Thumbs flip four coins per flip and ignore three") {
        val (driver, player) = driverWith("Test Thumb", "Test Thumb")

        CoinFlipModifiers.coinsPerFlip(driver.state, driver.cardRegistry, player) shouldBe 4

        val seed = seedThatSettles(driver, player, FlipCoinEffect(), 1L..400L)
        seed.shouldNotBeNull()
        val flips = flipWithSeed(driver, player, FlipCoinEffect(), seed)
            .events.filterIsInstance<CoinFlipEvent>()

        flips.size shouldBe 4
        flips.count { !it.ignored } shouldBe 1
    }

    test("the coins-per-flip count is a parameter, not a baked-in two") {
        val (driver, player) = driverWith("Test Triple Thumb")
        CoinFlipModifiers.coinsPerFlip(driver.state, driver.cardRegistry, player) shouldBe 3
    }

    test("a Thumb its owner does not control does nothing for them") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        extraCards.forEach { driver.registerCard(it) }
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.putPermanentOnBattlefield(opponent, "Test Thumb")

        CoinFlipModifiers.coinsPerFlip(driver.state, driver.cardRegistry, player) shouldBe 1
        CoinFlipModifiers.coinsPerFlip(driver.state, driver.cardRegistry, opponent) shouldBe 2
    }

    // ── Which coin decides the flip ──────────────────────────────────────

    test("a unanimous batch decides itself — no question is raised and the kept coin is that result") {
        val (driver, player) = driverWith("Test Thumb")

        val seed = seedThatSettles(driver, player, FlipCoinEffect(), 1L..200L)
        seed.shouldNotBeNull()
        val flips = flipWithSeed(driver, player, FlipCoinEffect(), seed)
            .events.filterIsInstance<CoinFlipEvent>()

        // Unanimous is exactly why nothing was asked.
        flips.map { it.won }.distinct().size shouldBe 1
        flips.single { !it.ignored }.won shouldBe flips.first().won
    }

    test("a mixed batch asks the flipper which result to keep") {
        val (driver, player) = driverWith("Test Thumb")

        val seed = seedThatPauses(driver, player, FlipCoinEffect(), 1L..200L)
        seed.shouldNotBeNull()
        val result = flipWithSeed(driver, player, FlipCoinEffect(), seed)

        val decision = result.pendingDecision as? YesNoDecision
        decision.shouldNotBeNull()
        decision.playerId shouldBe player
        decision.yesText shouldBe "Keep heads"
        decision.noText shouldBe "Keep tails"

        // Both coins were rolled before the question was asked — the ruling's "you will know the
        // results of all simultaneous flips before choosing which to ignore".
        decision.prompt shouldBe "You flipped 1 heads and 1 tails. Ignore all but one — which result do you keep?"
    }

    // ── Interaction with the result-dictating replacement ────────────────

    test("a forced win makes every coin heads, so the flipper is never asked") {
        val (driver, player) = driverWith("Test Thumb", "Test Lucky Coin")

        (1L..40L).forEach { seed ->
            val result = flipWithSeed(driver, player, FlipCoinEffect(), seed)
            result.isPaused shouldBe false
            val flips = result.events.filterIsInstance<CoinFlipEvent>()
            flips.size shouldBe 2
            flips.all { it.won }.shouldBeTrue()
        }
    }

    // ── Multi-coin instructions ──────────────────────────────────────────

    test("flip five coins under a Thumb is five pairs, not ten coins with any five ignored") {
        val (driver, player) = driverWith("Test Thumb")

        // Ten real flips either way; what the ruling pins down is that they are *paired*, so exactly
        // five survive — one per coin the instruction asked for.
        val seed = seedThatSettles(driver, player, FlipCoinsEffect(5, "heads"), 1L..4000L)
        seed.shouldNotBeNull()
        val result = flipWithSeed(driver, player, FlipCoinsEffect(5, "heads"), seed)
        val flips = result.events.filterIsInstance<CoinFlipEvent>()

        flips.size shouldBe 10
        flips.count { !it.ignored } shouldBe 5
        // The published tally counts the kept coins only.
        result.updatedStoredNumbers["heads"] shouldBe flips.count { !it.ignored && it.won }
    }

    test("flipping zero coins under a Thumb still flips nothing") {
        val (driver, player) = driverWith("Test Thumb")
        val result = flipWithSeed(driver, player, FlipCoinsEffect(0, "heads"), 1L)

        result.events.filterIsInstance<CoinFlipEvent>().size shouldBe 0
        result.updatedStoredNumbers["heads"] shouldBe 0
    }

    test("flip two coins under a Thumb replaces each of the two independently") {
        val (driver, player) = driverWith("Test Thumb")

        val seed = seedThatSettles(driver, player, FlipTwoCoinsEffect(), 1L..2000L)
        seed.shouldNotBeNull()
        val flips = flipWithSeed(driver, player, FlipTwoCoinsEffect(), seed)
            .events.filterIsInstance<CoinFlipEvent>()

        flips.size shouldBe 4
        flips.count { !it.ignored } shouldBe 2
    }

    // ── The full pause / resume path, through the engine ─────────────────

    /**
     * Cast the coin-toss spell on a seeded game and, if that seed produced a mixed batch, answer
     * the Krark's Thumb question with [keepHeads]. Returns the life change, or null when the seed
     * happened to produce a unanimous batch and no question was asked.
     *
     * Seeds are searched rather than hard-coded so the resume path is genuinely exercised instead
     * of silently skipped on a run where both coins agreed.
     */
    fun lifeChangeAfterKeeping(keepHeads: Boolean, seed: Long): Int? {
        val (driver, player) = driverWith("Test Thumb")
        driver.replaceState(driver.state.copy(rng = GameRng.seeded(seed)))
        val startingLife = driver.getLifeTotal(player)

        val card = driver.putCardInHand(player, "Test Coin Toss")
        driver.giveMana(player, com.wingedsheep.sdk.core.Color.RED, 1)
        driver.castSpell(player, card)
        driver.bothPass()

        val asked = driver.pendingDecision as? YesNoDecision ?: return null
        asked.yesText shouldBe "Keep heads"
        driver.submitYesNo(player, choice = keepHeads)
        return driver.getLifeTotal(player) - startingLife
    }

    test("keeping the heads coin runs the won half of the spell") {
        val change = (1L..80L).firstNotNullOfOrNull { lifeChangeAfterKeeping(keepHeads = true, seed = it) }
        change.shouldNotBeNull()
        change shouldBe 10
    }

    test("keeping the tails coin runs the lost half of the spell") {
        val change = (1L..80L).firstNotNullOfOrNull { lifeChangeAfterKeeping(keepHeads = false, seed = it) }
        change.shouldNotBeNull()
        change shouldBe -3
    }

    test("a flip nobody had to choose about still runs the half its coins settled on") {
        // The unanimous case goes straight through without a prompt, and must reach the same halves.
        val changes = (1L..80L).mapNotNull { seed ->
            val (driver, player) = driverWith("Test Thumb")
            driver.replaceState(driver.state.copy(rng = GameRng.seeded(seed)))
            val startingLife = driver.getLifeTotal(player)

            val card = driver.putCardInHand(player, "Test Coin Toss")
            driver.giveMana(player, com.wingedsheep.sdk.core.Color.RED, 1)
            driver.castSpell(player, card)
            driver.bothPass()

            if (driver.pendingDecision != null) null
            else driver.getLifeTotal(player) - startingLife
        }
        changes.isNotEmpty().shouldBeTrue()
        changes.all { it == 10 || it == -3 }.shouldBeTrue()
    }

    test("the batch's ignored coins are reported, so the log shows what was really flipped") {
        val (driver, player) = driverWith("Test Thumb")

        val seed = seedThatSettles(driver, player, FlipCoinsEffect(3, "heads"), 1L..2000L)
        seed.shouldNotBeNull()
        val flips = flipWithSeed(driver, player, FlipCoinsEffect(3, "heads"), seed)
            .events.filterIsInstance<CoinFlipEvent>()

        flips.count { it.ignored } shouldBe 3
        flips.size shouldBeGreaterThan flips.count { !it.ignored }
    }
})
