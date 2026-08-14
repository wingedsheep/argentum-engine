package com.wingedsheep.engine.view

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.ConvokeCreatureData
import com.wingedsheep.engine.legalactions.DelveCardData
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.HarmonizeCreatureData
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.legalactions.TapForGenericPermanentData
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * [LegalActionInfo.minimumManaCostString] — the *floor* an action's cost reaches once the
 * alternative payments it already offers are spent to the maximum.
 *
 * `manaCostString` is the pre-reduction price for convoke, delve, waterbend and harmonize: the
 * enumerator folds the reduction into affordability but never into the cost it advertises, so a
 * convoke spell used to report the one number the player never actually pays. The client renders
 * both ends as a range, which only works if the floor obeys each keyword's own rule — convoke
 * matches colors, delve and waterbend are generic-only, and harmonize taps at most one creature.
 */
class LegalActionMinimumCostTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20,
            skipMulligans = true
        )
        return driver
    }

    fun enricher(driver: GameTestDriver) =
        LegalActionEnricher(ManaSolver(driver.cardRegistry), driver.cardRegistry)

    /** Enrich a single hand-built action and read back the floor the DTO advertises. */
    fun floorOf(driver: GameTestDriver, action: LegalAction): String? =
        enricher(driver).enrich(listOf(action), driver.state, driver.player1)
            .single().minimumManaCostString

    /** A bare cast action carrying [manaCostString] and nothing else. */
    fun castAction(driver: GameTestDriver, manaCostString: String?) = LegalAction(
        actionType = "CastSpell",
        description = "Cast something",
        action = CastSpell(driver.player1, EntityId.generate()),
        manaCostString = manaCostString
    )

    fun creature(name: String, vararg colors: Color) =
        ConvokeCreatureData(EntityId.generate(), name, colors.toSet())

    // --- Convoke (CR 702.51a): each tapped creature pays {1} or one pip of its own color ---

    test("convoke creatures pay pips of their own color") {
        val driver = createDriver()
        // {4}{W}{W} with two white creatures: both {W} pips are convoked away, leaving the generic.
        val action = castAction(driver, "{4}{W}{W}").copy(
            hasConvoke = true,
            convokeCreatures = listOf(creature("Celebrant", Color.WHITE), creature("Cleric", Color.WHITE))
        )
        floorOf(driver, action) shouldBe "{4}"
    }

    test("a convoke creature of the wrong color pays generic instead of a pip") {
        val driver = createDriver()
        // A lone green creature can't pay a {W} pip, so it comes off the generic: {4}{W}{W} → {3}{W}{W}.
        val action = castAction(driver, "{4}{W}{W}").copy(
            hasConvoke = true,
            convokeCreatures = listOf(creature("Sapling", Color.GREEN))
        )
        floorOf(driver, action) shouldBe "{3}{W}{W}"
    }

    test("convoke can't touch an all-colored cost with no creature of a matching color") {
        val driver = createDriver()
        // {W}{W} has no generic to spill into and no white creature to pay a pip — the cost is fixed,
        // so there is no range to show and the field stays null.
        val action = castAction(driver, "{W}{W}").copy(
            hasConvoke = true,
            convokeCreatures = listOf(creature("Sapling", Color.GREEN), creature("Bear", Color.GREEN))
        )
        floorOf(driver, action).shouldBeNull()
    }

    test("a cost convoked away entirely renders as {0}, not as the empty string") {
        val driver = createDriver()
        // {1}{W} with a white creature and a spare body reaches zero. The symbol list is empty there,
        // and the client's pip renderer would draw nothing at all for "".
        val action = castAction(driver, "{1}{W}").copy(
            hasConvoke = true,
            convokeCreatures = listOf(creature("Celebrant", Color.WHITE), creature("Sapling", Color.GREEN))
        )
        floorOf(driver, action) shouldBe "{0}"
    }

    test("a multicolored convoke creature pays a pip of either of its colors") {
        val driver = createDriver()
        val action = castAction(driver, "{2}{W}{U}").copy(
            hasConvoke = true,
            convokeCreatures = listOf(creature("Sphinx", Color.WHITE, Color.BLUE))
        )
        // One creature, one pip: whichever it pays, exactly one mana comes off the cost.
        floorOf(driver, action) shouldBe "{2}{U}"
    }

    // --- Delve (CR 702.66a): each exiled card pays one *generic* mana ---

    test("delve reduces generic mana only, floored at the colored pips") {
        val driver = createDriver()
        val graveyard = (1..3).map { DelveCardData(EntityId.generate(), "Card $it") }
        val action = castAction(driver, "{5}{U}").copy(hasDelve = true, delveCards = graveyard)
        floorOf(driver, action) shouldBe "{2}{U}"
    }

    test("delve past the generic portion leaves the colored pips standing") {
        val driver = createDriver()
        // Six cards against {2}{U}{U}: only the two generic can go, never the {U}{U}.
        val graveyard = (1..6).map { DelveCardData(EntityId.generate(), "Card $it") }
        val action = castAction(driver, "{2}{U}{U}").copy(hasDelve = true, delveCards = graveyard)
        floorOf(driver, action) shouldBe "{U}{U}"
    }

    test("an X cost's floor ignores delve, since X is unannounced at enumeration time") {
        val driver = createDriver()
        // {X}{U} has no printed generic to reduce. The X the delve would really pay doesn't exist
        // yet, so claiming a lower floor here would be inventing a number.
        val graveyard = (1..4).map { DelveCardData(EntityId.generate(), "Card $it") }
        val action = castAction(driver, "{X}{U}").copy(hasDelve = true, delveCards = graveyard)
        floorOf(driver, action).shouldBeNull()
    }

    // --- Waterbend: generic-only taps, capped at the waterbend {N} ---

    test("waterbend taps are capped at the waterbend amount, not at the number of permanents") {
        val driver = createDriver()
        val permanents = (1..5).map { TapForGenericPermanentData(EntityId.generate(), "Thing $it", isCreature = true) }
        val action = castAction(driver, "{4}{U}").copy(
            hasTapForGeneric = true,
            tapForGenericPermanents = permanents,
            tapForGenericAmount = 2
        )
        floorOf(driver, action) shouldBe "{2}{U}"
    }

    test("waterbend {X} has no cap yet, so every tappable permanent counts toward the floor") {
        val driver = createDriver()
        // A null tapForGenericAmount is the "waterbend {X}" shape — the cap is the X the player hasn't
        // chosen, so the floor is drawn against the whole board.
        val permanents = (1..3).map { TapForGenericPermanentData(EntityId.generate(), "Thing $it", isCreature = false) }
        val action = castAction(driver, "{4}{U}").copy(
            hasTapForGeneric = true,
            tapForGenericPermanents = permanents,
            tapForGenericAmount = null
        )
        floorOf(driver, action) shouldBe "{1}{U}"
    }

    // --- Harmonize: one creature taps, so the floor is the best power on offer ---

    test("harmonize uses the single best power, never the sum") {
        val driver = createDriver()
        val creatures = listOf(
            HarmonizeCreatureData(EntityId.generate(), "Runt", 1),
            HarmonizeCreatureData(EntityId.generate(), "Giant", 4),
            HarmonizeCreatureData(EntityId.generate(), "Bear", 2)
        )
        val action = castAction(driver, "{6}{G}").copy(hasHarmonize = true, harmonizeCreatures = creatures)
        // Best single tap is the 4-power Giant: {6}{G} → {2}{G}. Summing would wrongly reach {0}{G}.
        floorOf(driver, action) shouldBe "{2}{G}"
    }

    test("harmonize creatures with no power leave the cost alone") {
        val driver = createDriver()
        val creatures = listOf(HarmonizeCreatureData(EntityId.generate(), "Wall", 0))
        val action = castAction(driver, "{6}{G}").copy(hasHarmonize = true, harmonizeCreatures = creatures)
        floorOf(driver, action).shouldBeNull()
    }

    // --- Nothing to reduce ---

    test("an action with no alternative payment advertises no floor") {
        val driver = createDriver()
        floorOf(driver, castAction(driver, "{2}{G}")).shouldBeNull()
    }

    test("an action with no mana cost at all advertises no floor") {
        val driver = createDriver()
        floorOf(driver, castAction(driver, null)).shouldBeNull()
    }

    test("an empty convoke creature list is treated as no convoke at all") {
        val driver = createDriver()
        val action = castAction(driver, "{4}{W}").copy(hasConvoke = true, convokeCreatures = emptyList())
        floorOf(driver, action).shouldBeNull()
    }

    // --- End to end through the real enumerator ---

    test("a real convoke spell reaches the client with both ends of its cost") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val caster = driver.activePlayer!!

        // Six lands, so Sun-Dappled Celebrant ({4}{W}{W}, convoke) is castable without convoking
        // and the enumerator offers the cast rather than skipping it.
        repeat(2) { driver.putLandOnBattlefield(caster, "Plains") }
        repeat(4) { driver.putLandOnBattlefield(caster, "Forest") }

        // Two white bodies that could each convoke away a {W} pip.
        repeat(2) {
            val body = driver.putCreatureOnBattlefield(caster, "Sun-Dappled Celebrant")
            driver.removeSummoningSickness(body)
        }
        driver.putCardInHand(caster, "Sun-Dappled Celebrant")

        val actions = LegalActionEnumerator.create(driver.cardRegistry)
            .enumerate(driver.state, caster, EnumerationMode.FULL)
        val enriched = LegalActionEnricher(ManaSolver(driver.cardRegistry), driver.cardRegistry)
            .enrich(actions, driver.state, caster)

        val cast = enriched.single {
            it.actionType == "CastSpell" && it.description.contains("Sun-Dappled Celebrant")
        }
        cast.hasConvoke shouldBe true
        cast.manaCostString shouldBe "{4}{W}{W}"
        // Both white creatures pay a {W} pip, so the real span the player chooses within is
        // {4}{W}{W} down to {4} — not the single number the DTO used to carry.
        cast.minimumManaCostString shouldBe "{4}"
    }
})
