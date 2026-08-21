package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.KyloxVisionaryInventor
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Kylox, Visionary Inventor — "Whenever Kylox attacks, sacrifice any number of other creatures,
 * then exile the top X cards of your library, where X is their total power. You may cast any
 * number of instant and/or sorcery spells from among the exiled cards without paying their mana
 * costs."
 *
 * The pipeline itself is Villainous Wealth's and already covered. What is new here is
 * [com.wingedsheep.sdk.scripting.values.DynamicAmount.TotalPowerSacrificedThisWay], and every test
 * below exists to pin one of its properties:
 *
 * - **It is last-known information.** By the time the gather step asks, the sacrificed creatures
 *   are in the graveyard. A live-state read would see nothing and exile zero cards, so an X that
 *   matches the creatures' power at all is the assertion (2024-02-02 ruling: "the power of the
 *   sacrificed creatures as they last existed on the battlefield").
 * - **It sums power, it doesn't count bodies.** Two creatures worth 3 and 2 must exile five cards,
 *   not two — the failure mode if it were wired to `PermanentsSacrificedThisWay`.
 * - **Zero is a legal answer.** "Any number" includes none, and X = 0 has to run the rest of the
 *   pipeline as a no-op rather than erroring or exiling the whole library.
 *
 * The fourth test covers `excludeSource`: Kylox says "other creatures", so it must not be offered
 * as fodder for its own trigger.
 */
class KyloxVisionaryInventorScenarioTest : FunSpec({

    // Free-cast fodder with no targets and no decisions of their own, so the assertions stay about
    // Kylox rather than about whatever the cast spell went on to ask.
    val freeGain = card("Test Free Gain") {
        manaCost = "{6}{U}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(4) }
    }
    val freeDraw = card("Test Free Draw") {
        manaCost = "{5}{R}"
        typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }
    // A 2-power body, so "total power" and "number of creatures" can't both explain the result.
    val bear = card("Test Two Power Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(KyloxVisionaryInventor, freeGain, freeDraw, bear))
        return driver
    }

    fun GameTestDriver.exileNames(playerId: EntityId): List<String> =
        state.getExile(playerId).mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }

    fun optionNames(driver: GameTestDriver, decision: SelectCardsDecision): Set<String> =
        decision.options.mapNotNull { driver.state.getEntity(it)?.get<CardComponent>()?.name }.toSet()

    /** Attack with [attacker], then resolve the stack until something needs a decision. */
    fun GameTestDriver.attackAndResolve(you: EntityId, attacker: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(you, listOf(attacker), getOpponent(you)).error shouldBe null
        var guard = 0
        while (!isPaused && state.stack.isNotEmpty() && guard++ < 10) bothPass()
    }

    test("X is the sacrificed creatures' total power, and instants and sorceries exile-cast for free") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kylox = driver.putCreatureOnBattlefield(you, "Kylox, Visionary Inventor")
        driver.removeSummoningSickness(kylox)
        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser") // 3/3
        val twoPower = driver.putCreatureOnBattlefield(you, "Test Two Power Bear") // 2/2

        // Top five of the library, top-most pushed last.
        driver.putCardOnTopOfLibrary(you, "Mountain")
        driver.putCardOnTopOfLibrary(you, "Mountain")
        driver.putCardOnTopOfLibrary(you, "Centaur Courser")
        driver.putCardOnTopOfLibrary(you, "Test Free Draw")
        driver.putCardOnTopOfLibrary(you, "Test Free Gain")

        val lifeBefore = driver.getLifeTotal(you)
        driver.attackAndResolve(you, kylox)

        val sacrificeDecision = driver.pendingDecision as SelectCardsDecision
        withClue("the trigger opens on the sacrifice choice") {
            optionNames(driver, sacrificeDecision) shouldBe setOf("Centaur Courser", "Test Two Power Bear")
        }
        driver.submitCardSelection(you, listOf(courser, twoPower))

        var guard = 0
        var castDecision: SelectCardsDecision? = null
        while (guard++ < 12) {
            val pending = driver.pendingDecision
            if (pending is SelectCardsDecision) {
                castDecision = pending
                break
            }
            if (driver.state.stack.isNotEmpty()) driver.bothPass() else break
        }

        withClue("3 power + 2 power = X of 5, so five cards left the library for exile") {
            driver.exileNames(you).size shouldBe 5
        }
        val castChoice = castDecision!!
        withClue("only the instant and the sorcery are castable; the creature and lands are not") {
            optionNames(driver, castChoice) shouldBe setOf("Test Free Gain", "Test Free Draw")
        }

        val gainId = castChoice.options.first {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Test Free Gain"
        }
        driver.submitCardSelection(you, listOf(gainId))
        // The effect keeps offering the rest of the pile until the controller stops; decline, then
        // let the free-cast spell resolve.
        guard = 0
        while (guard++ < 20) {
            when {
                driver.pendingDecision is SelectCardsDecision -> driver.submitCardSelection(you, emptyList())
                driver.isPaused -> driver.autoResolveDecision()
                driver.state.stack.isNotEmpty() -> driver.bothPass()
                else -> break
            }
        }

        withClue("the sorcery resolved without its {6}{U} being paid, mid-combat") {
            driver.getLifeTotal(you) shouldBe lifeBefore + 4
        }
        withClue("cards not chosen stay in exile") {
            driver.exileNames(you).contains("Test Free Draw") shouldBe true
        }
    }

    test("sacrificing nothing makes X zero and exiles nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kylox = driver.putCreatureOnBattlefield(you, "Kylox, Visionary Inventor")
        driver.removeSummoningSickness(kylox)
        driver.putCreatureOnBattlefield(you, "Centaur Courser")

        val librarySizeBefore = driver.state.getLibrary(you).size
        driver.attackAndResolve(you, kylox)

        val sacrificeDecision = driver.pendingDecision as SelectCardsDecision
        withClue("\"any number\" allows choosing none") {
            sacrificeDecision.minSelections shouldBe 0
        }
        driver.submitCardSelection(you, emptyList())

        var guard = 0
        while (!driver.isPaused && driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        withClue("X = 0, so the library is untouched") {
            driver.state.getLibrary(you).size shouldBe librarySizeBefore
        }
        driver.exileNames(you) shouldBe emptyList()
        withClue("nothing was sacrificed — the Courser is still on the battlefield") {
            (driver.findPermanent(you, "Centaur Courser") != null) shouldBe true
            driver.getGraveyardCardNames(you).contains("Centaur Courser") shouldBe false
        }
    }

    test("Kylox is not fodder for its own trigger — the clause says other creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kylox = driver.putCreatureOnBattlefield(you, "Kylox, Visionary Inventor")
        driver.removeSummoningSickness(kylox)
        driver.putCreatureOnBattlefield(you, "Centaur Courser")

        driver.attackAndResolve(you, kylox)

        val sacrificeDecision = driver.pendingDecision as SelectCardsDecision
        withClue("the attacking Kylox must not appear among its own sacrifice options") {
            sacrificeDecision.options.contains(kylox) shouldBe false
        }
        optionNames(driver, sacrificeDecision) shouldBe setOf("Centaur Courser")
    }

    test("the printed keywords are on the definition") {
        KyloxVisionaryInventor.keywords.contains(Keyword.MENACE) shouldBe true
        KyloxVisionaryInventor.keywords.contains(Keyword.HASTE) shouldBe true
        KyloxVisionaryInventor.keywordAbilities
            .filterIsInstance<com.wingedsheep.sdk.scripting.KeywordAbility.Ward>()
            .size shouldBe 1
    }
})
