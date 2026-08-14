package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ori.cards.PyromancersGoggles
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Pyromancer's Goggles (canonical ORI #236, reprinted FDN #677).
 *
 * {T}: Add {R}. When that mana is spent to cast a red instant or sorcery spell, copy that spell
 * and you may choose new targets for the copy.
 *
 * Covers the new [com.wingedsheep.sdk.scripting.effects.ManaSpellRider.CopySpellWhenSpent] rider
 * and the `riders` parameter on `AddMana`. The spells here are deliberately **targetless** so the
 * copy resolves without a retarget pause and the "did it happen twice?" assertion is unambiguous —
 * per the ruling, "Any red instant or sorcery spell you spend the mana on will be copied, not just
 * one that requires targets."
 *
 * The negative cases pin the filter down on both axes: colour (a colourless instant whose generic
 * cost the {R} really does pay, so the rider is consumed and must then no-op) and card type (a red
 * creature spell). Together with the positive case they show the rider rides along on every cast the
 * mana pays for but fires only on a match — which is what "the mana can be spent on anything, not
 * just a red instant or sorcery spell" means.
 */
class PyromancersGogglesScenarioTest : FunSpec({

    val gogglesAbilityId = PyromancersGoggles.activatedAbilities[0].id

    /** A red instant with no targets: the copy is observable purely as a second life gain. */
    val redInstant = CardDefinition(
        name = "Test Red Ember",
        manaCost = ManaCost.parse("{R}"),
        typeLine = TypeLine.parse("Instant"),
        oracleText = "You gain 2 life.",
        script = CardScript.spell(effect = Effects.GainLife(2))
    )

    /**
     * A *colourless* instant with a generic cost, so the Goggles' {R} genuinely pays for it and the
     * rider is consumed — but the spell isn't red, so the rider must no-op. This is the case that
     * pins down "the mana can be spent on anything".
     */
    val colorlessInstant = CardDefinition(
        name = "Test Grey Ripple",
        manaCost = ManaCost.parse("{1}"),
        typeLine = TypeLine.parse("Instant"),
        oracleText = "You gain 2 life.",
        script = CardScript.spell(effect = Effects.GainLife(2))
    )

    val redCreature = CardDefinition.creature(
        name = "Test Red Imp",
        manaCost = ManaCost.parse("{R}"),
        subtypes = emptySet(),
        power = 1,
        toughness = 1,
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(PyromancersGoggles, redInstant, colorlessInstant, redCreature)
        )
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    /** Tap the Goggles, putting one rider-carrying {R} into the pool. */
    fun tapGoggles(driver: GameTestDriver, you: com.wingedsheep.sdk.model.EntityId, goggles: com.wingedsheep.sdk.model.EntityId) {
        driver.submitSuccess(
            ActivateAbility(playerId = you, sourceId = goggles, abilityId = gogglesAbilityId)
        )
    }

    test("a red instant paid with the Goggles' mana is copied — the effect happens twice") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val goggles = driver.putPermanentOnBattlefield(you, "Pyromancer's Goggles")
        val ember = driver.putCardInHand(you, "Test Red Ember")
        tapGoggles(driver, you, goggles)

        driver.castSpell(you, ember).error shouldBe null
        // The copy trigger sits above the spell; drain the whole stack.
        driver.bothPass()
        driver.bothPass()
        driver.bothPass()

        // 20 + 2 (copy) + 2 (original) — the copy resolves first, then the original.
        driver.getLifeTotal(you) shouldBe 24
    }

    test("without the Goggles' mana the same spell resolves once") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ember = driver.putCardInHand(you, "Test Red Ember")
        driver.giveMana(you, Color.RED, 1)

        driver.castSpell(you, ember).error shouldBe null
        driver.bothPass()
        driver.bothPass()

        driver.getLifeTotal(you) shouldBe 22
    }

    test("the mana spends on a nonred instant, which is not copied") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val goggles = driver.putPermanentOnBattlefield(you, "Pyromancer's Goggles")
        val ripple = driver.putCardInHand(you, "Test Grey Ripple")
        tapGoggles(driver, you, goggles)

        // "The mana produced by Pyromancer's Goggles can be spent on anything, not just a red
        // instant or sorcery spell." The {R} pays this {1} cost, so the rider *is* consumed — and
        // must then no-op, because the spell isn't red.
        driver.castSpell(you, ripple).error shouldBe null
        driver.bothPass()
        driver.bothPass()

        driver.getLifeTotal(you) shouldBe 22
    }

    test("a red creature spell paid with the Goggles' mana is not copied") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val goggles = driver.putPermanentOnBattlefield(you, "Pyromancer's Goggles")
        val imp = driver.putCardInHand(you, "Test Red Imp")
        tapGoggles(driver, you, goggles)

        driver.castSpell(you, imp).error shouldBe null
        driver.bothPass()
        driver.bothPass()

        // One Imp, not two — the rider filter is instant-or-sorcery only.
        driver.getCreatures(you).count { driver.getCardName(it) == "Test Red Imp" } shouldBe 1
    }
})
