package com.wingedsheep.engine.legalactions.support

import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck

/**
 * Reusable starting states for legal-action enumeration tests.
 *
 * Each factory returns a fully-initialized [EnumerationTestDriver]. Tests then
 * configure further via `driver.game` (play lands, cast spells, advance phases)
 * before calling `enumerateFor(...)`.
 *
 * Keep this file small and additive: only add a fixture once two or more tests
 * need the same setup. Single-use setups belong in the test that uses them.
 */
object EnumerationFixtures {

    /**
     * A standard mirror-match: both players run 20 Forest + 20 Grizzly Bears.
     *
     * Game starts on Player 1's UNTAP step, all of [TestCards.all] registered.
     * Useful as the cheapest "real game state" baseline — has mana, has spells,
     * exercises the full enumerator pipeline including auto-tap.
     */
    fun forestAndBearsMirror(): EnumerationTestDriver {
        val driver = EnumerationTestDriver()
        driver.registerCards(TestCards.all)
        driver.game.initMirrorMatch(
            deck = Deck.of(
                "Forest" to 20,
                "Grizzly Bears" to 20
            ),
            skipMulligans = true
        )
        return driver
    }

    /**
     * Same as [forestAndBearsMirror] but advanced to Player 1's first
     * pre-combat main phase, with priority on Player 1.
     *
     * The most common starting point for "can the active player do X?" tests.
     */
    fun mainPhaseOfActivePlayer(): EnumerationTestDriver {
        val driver = forestAndBearsMirror()
        driver.game.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /**
     * A deck of pure Forests for both players. The opening hand is always
     * 7 Forests, so tests can deterministically reason about the count of
     * lands in hand without depending on shuffle luck.
     */
    fun allForestsMirror(): EnumerationTestDriver {
        val driver = EnumerationTestDriver()
        driver.registerCards(TestCards.all)
        driver.game.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            skipMulligans = true
        )
        return driver
    }

    /** [allForestsMirror] advanced to the active player's pre-combat main phase. */
    fun allForestsMainPhase(): EnumerationTestDriver {
        val driver = allForestsMirror()
        driver.game.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }
}
