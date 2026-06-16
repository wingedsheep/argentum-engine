package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.ParadigmComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Mechanic-level tests for Paradigm (Secrets of Strixhaven), driven by the [ParadigmComponent]
 * marker the engine attaches as a Paradigm spell lands in exile on its own resolution. Each of the
 * owner's precombat (first) main phases, the engine synthesizes a triggered ability
 * ([com.wingedsheep.sdk.scripting.Paradigm.recastAbility]) that lets the owner cast a **copy** of
 * the exiled card for free. The original never leaves exile (the recurrence) and each copy is a
 * phantom that ceases to exist (CR 707.10a / 112.3b).
 *
 * The test Lesson gains 2 life so every cast/recast is observable on the life total.
 */
class ParadigmMechanicTest : FunSpec({

    // "Research Seminar" — Sorcery — Lesson: gain 2 life, then exile + recur via Paradigm.
    val researchSeminar = card("Research Seminar") {
        manaCost = "{1}{U}"
        typeLine = "Sorcery — Lesson"
        spell {
            effect = Effects.GainLife(2)
            paradigm()
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(researchSeminar))
        return driver
    }

    /** Cast Research Seminar from hand on the owner's current precombat main and resolve it. */
    fun castSeminar(driver: GameTestDriver, player: EntityId): EntityId {
        driver.giveMana(player, com.wingedsheep.sdk.core.Color.BLUE, 2) // pays {1}{U} from pool
        val cardId = driver.putCardInHand(player, "Research Seminar")
        driver.castSpell(player, cardId) // FromPool — pool has mana
        driver.bothPass() // resolve the spell
        return cardId
    }

    /**
     * Advance to the owner's *next* precombat main phase, passing through any intervening turns,
     * and stop the instant it begins — with the Paradigm recast trigger already on the stack but
     * not yet resolved. Steps off the current precombat main (via postcombat main) first so
     * consecutive calls keep moving forward.
     */
    fun advanceToNextOwnerPrecombatMain(driver: GameTestDriver, owner: EntityId) {
        do {
            driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        } while (driver.activePlayer != owner)
    }

    fun seminarsInExile(driver: GameTestDriver, player: EntityId): Int =
        driver.getExile(player).count { driver.getCardName(it) == "Research Seminar" }

    test("the card definition carries the Lesson subtype and the Paradigm keyword") {
        researchSeminar.typeLine.hasSubtype(Subtype.LESSON).shouldBeTrue()
        researchSeminar.keywords.contains(Keyword.PARADIGM).shouldBeTrue()
        researchSeminar.script.paradigm.shouldBeTrue()
        // Paradigm implies self-exile-on-resolve.
        researchSeminar.script.selfExileOnResolve.shouldBeTrue()
    }

    test("casting a Paradigm spell does its effect, exiles itself, and gets the marker") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.activePlayer!!

        val lifeBefore = driver.getLifeTotal(player)
        val cardId = castSeminar(driver, player)

        driver.getLifeTotal(player) shouldBe lifeBefore + 2
        // Exiled, NOT in the graveyard.
        driver.getExile(player).contains(cardId).shouldBeTrue()
        driver.getGraveyard(player).contains(cardId).shouldBeFalse()
        driver.state.getEntity(cardId)?.has<ParadigmComponent>().shouldBe(true)
    }

    test("it does NOT recast on the turn it was cast (the precombat-main trigger already passed)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.activePlayer!!

        val lifeBefore = driver.getLifeTotal(player)
        castSeminar(driver, player)
        // Move through the rest of this turn; no recast trigger should fire again this turn.
        driver.passPriorityUntil(Step.END)
        driver.getLifeTotal(player) shouldBe lifeBefore + 2 // only the original cast
    }

    test("at the owner's next precombat main, the recast offers a free copy that gains life again") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.activePlayer!!

        val cardId = castSeminar(driver, player)
        val lifeAfterCast = driver.getLifeTotal(player)

        advanceToNextOwnerPrecombatMain(driver, player)
        driver.bothPass()                 // resolve the recast trigger -> "may cast a copy?"
        driver.submitYesNo(player, true)  // yes
        driver.bothPass()                 // resolve the copy -> +2 life

        driver.getLifeTotal(player) shouldBe lifeAfterCast + 2
        // The original stays in exile, still marked, and there is exactly ONE copy of it in exile
        // (the phantom copy ceased to exist — no exponential growth).
        driver.getExile(player).contains(cardId).shouldBeTrue()
        driver.state.getEntity(cardId)?.has<ParadigmComponent>().shouldBe(true)
        seminarsInExile(driver, player) shouldBe 1
        // The copy did not fall into the graveyard either.
        driver.getGraveyard(player).any { driver.getCardName(it) == "Research Seminar" }.shouldBeFalse()
    }

    test("declining the recast leaves no copy and keeps the original recurring") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.activePlayer!!

        val cardId = castSeminar(driver, player)
        val lifeAfterCast = driver.getLifeTotal(player)

        advanceToNextOwnerPrecombatMain(driver, player)
        driver.bothPass()                  // resolve the recast trigger
        driver.submitYesNo(player, false)  // decline

        driver.getLifeTotal(player) shouldBe lifeAfterCast // no extra life
        // No leftover phantom copy in exile — only the original remains.
        seminarsInExile(driver, player) shouldBe 1
        driver.getExile(player).contains(cardId).shouldBeTrue()
        driver.state.getEntity(cardId)?.has<ParadigmComponent>().shouldBe(true)
    }

    test("the recast recurs every one of the owner's turns") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.activePlayer!!

        val cardId = castSeminar(driver, player)
        var life = driver.getLifeTotal(player)

        repeat(2) {
            advanceToNextOwnerPrecombatMain(driver, player)
            driver.bothPass()
            driver.submitYesNo(player, true)
            driver.bothPass()
            life += 2
            driver.getLifeTotal(player) shouldBe life
        }
        // Still exactly one original in exile after multiple recasts.
        seminarsInExile(driver, player) shouldBe 1
        driver.state.getEntity(cardId)?.has<ParadigmComponent>().shouldBe(true)
    }

    test("the recast does NOT fire on an opponent's precombat main (Player.You = owner)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        castSeminar(driver, player)
        val lifeBefore = driver.getLifeTotal(player)

        // Advance to the opponent's precombat main — the owner's recast must not trigger.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.activePlayer shouldBe opponent

        driver.getTopOfStack().shouldBe(null) // no recast trigger on the stack
        driver.state.pendingDecision.shouldBe(null)
        driver.getLifeTotal(player) shouldBe lifeBefore
    }

    test("a Lesson exiled without the Paradigm marker never recurs") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.activePlayer!!

        // Put Research Seminar into exile by a non-Paradigm path (no marker attached).
        val cardId = driver.putCardInGraveyard(player, "Research Seminar")
        driver.replaceState(
            driver.state
                .removeFromZone(ZoneKey(player, Zone.GRAVEYARD), cardId)
                .addToZone(ZoneKey(player, Zone.EXILE), cardId)
        )
        driver.state.getEntity(cardId)?.has<ParadigmComponent>().shouldBe(false)

        val lifeBefore = driver.getLifeTotal(player)
        advanceToNextOwnerPrecombatMain(driver, player)

        // No trigger fired: nothing on the stack, no decision, life unchanged.
        driver.getTopOfStack().shouldBe(null)
        driver.state.pendingDecision.shouldBe(null)
        driver.getLifeTotal(player) shouldBe lifeBefore
    }
})
