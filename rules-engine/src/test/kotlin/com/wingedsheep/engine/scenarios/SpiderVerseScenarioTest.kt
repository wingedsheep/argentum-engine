package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Spider-Verse (SPM) — "The 'legend rule' doesn't apply to Spiders you control." Pins the new
 * `LegendRuleDoesNotApplyTo(filter)` static + its consult hook in `LegendRuleCheck`. The second
 * Spider is *cast* so the legend-rule state-based action genuinely fires as it resolves.
 *
 * Also pins the copy trigger's "Do this only once each turn" rider (`effectOncePerTurn`,
 * CR 603.2h). The ruling states both halves: "Once you choose to copy a spell with Spider-Verse's
 * last ability, that ability won't trigger again for the duration of the turn" — and, by
 * implication, declining is not choosing.
 */
class SpiderVerseScenarioTest : FunSpec({

    val legendarySpider = card("Test Legendary Spider") {
        manaCost = "{1}{G}"
        typeLine = "Legendary Creature — Spider"
        power = 1
        toughness = 1
    }

    // A flashback sorcery gives us a repeatable *non-hand* cast, which is what the copy trigger
    // watches for. Gaining life makes the copy observable: original + copy = 2 life, not 1.
    val flashbackBlessing = card("Test Flashback Blessing") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        oracleText = "You gain 1 life.\nFlashback {R}"
        spell { effect = Effects.GainLife(1) }
        keywordAbility(KeywordAbility.flashback("{R}"))
    }

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(legendarySpider, flashbackBlessing))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    /** Pass until the copy trigger resolves into its yes/no, or the stack empties. */
    fun advanceToDecision(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && driver.pendingDecision == null && driver.state.stack.isNotEmpty()) {
            driver.bothPass()
        }
    }

    test("with Spider-Verse out, casting a second same-named legendary Spider keeps both") {
        val (driver, you) = newGame()
        driver.putPermanentOnBattlefield(you, "Spider-Verse")
        val s1 = driver.putCreatureOnBattlefield(you, "Test Legendary Spider")

        driver.giveMana(you, Color.GREEN, 2)
        val s2card = driver.putCardInHand(you, "Test Legendary Spider")
        driver.castSpell(you, s2card)
        resolveStack(driver)

        driver.state.getBattlefield().contains(s1) shouldBe true
        driver.state.getBattlefield().count {
            driver.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Test Legendary Spider"
        } shouldBe 2
        (driver.pendingDecision == null) shouldBe true // no legend-rule choice — exempt
    }

    test("without Spider-Verse, casting a second same-named legendary Spider triggers the legend rule") {
        val (driver, you) = newGame()
        driver.putCreatureOnBattlefield(you, "Test Legendary Spider")

        driver.giveMana(you, Color.GREEN, 2)
        val s2card = driver.putCardInHand(you, "Test Legendary Spider")
        driver.castSpell(you, s2card)
        resolveStack(driver)

        // Legend rule applies: either it paused for a choice, or only one copy remains.
        val remaining = driver.state.getBattlefield().count {
            driver.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Test Legendary Spider"
        }
        (driver.pendingDecision != null || remaining <= 1) shouldBe true
    }

    test("declining the copy does not spend the turn — a later non-hand cast still offers it") {
        // The defect `effectOncePerTurn` fixes: under the trigger cap, declining the first
        // flashback cast burned the turn's copy and the second cast was never offered one.
        val (driver, you) = newGame()
        driver.putPermanentOnBattlefield(you, "Spider-Verse")
        val first = driver.putCardInGraveyard(you, "Test Flashback Blessing")
        val second = driver.putCardInGraveyard(you, "Test Flashback Blessing")
        driver.giveMana(you, Color.RED, 2)

        val startLife = driver.getLifeTotal(you)

        // First flashback cast: the copy trigger fires — decline it.
        driver.castSpell(you, first)
        advanceToDecision(driver)
        withClue("the first non-hand cast offers the copy") {
            (driver.pendingDecision != null) shouldBe true
        }
        driver.submitYesNo(you, false)
        resolveStack(driver)
        withClue("declined → only the original resolved") {
            driver.getLifeTotal(you) shouldBe startLife + 1
        }

        // Second flashback cast the same turn: the ability triggers again.
        driver.castSpell(you, second)
        advanceToDecision(driver)
        withClue("the ability triggers again after a decline") {
            (driver.pendingDecision != null) shouldBe true
        }
        driver.submitYesNo(you, true)
        resolveStack(driver)
        withClue("accepted → the second spell resolved alongside its copy") {
            driver.getLifeTotal(you) shouldBe startLife + 3
        }
    }
})
