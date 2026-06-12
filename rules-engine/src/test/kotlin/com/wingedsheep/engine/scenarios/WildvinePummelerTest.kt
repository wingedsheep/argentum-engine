package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ecl.cards.ForagingWickermaw
import com.wingedsheep.mtg.sets.definitions.ecl.cards.WildvinePummeler
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Wildvine Pummeler's Vivid cost reduction.
 *
 * Wildvine Pummeler: {6}{G}
 * Creature — Giant Berserker
 * 6/5
 * Vivid — This spell costs {1} less to cast for each color among permanents you control.
 * Reach, trample
 */
class WildvinePummelerTest : FunSpec({

    fun createDriverAndRegistry(): Pair<GameTestDriver, CardRegistry> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(WildvinePummeler)
        driver.registerCard(ForagingWickermaw)

        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(WildvinePummeler)
        registry.register(ForagingWickermaw)

        return driver to registry
    }

    test("no colored permanents - no cost reduction") {
        val (driver, registry) = createDriverAndRegistry()
        val calculator = CostCalculator(registry)

        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Lands are colorless permanents, no reduction
        val pummelerDef = registry.requireCard("Wildvine Pummeler")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, pummelerDef, activePlayer)
        effectiveCost.genericAmount shouldBe 6
        effectiveCost.cmc shouldBe 7 // {6}{G}
    }

    test("one colored permanent - reduces by 1") {
        val (driver, registry) = createDriverAndRegistry()
        val calculator = CostCalculator(registry)

        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a green creature on the battlefield (1 color = green)
        driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")

        val pummelerDef = registry.requireCard("Wildvine Pummeler")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, pummelerDef, activePlayer)
        effectiveCost.genericAmount shouldBe 5 // 6 - 1
        effectiveCost.cmc shouldBe 6 // {5}{G}
    }

    test("multiple permanents of same color - only counts once") {
        val (driver, registry) = createDriverAndRegistry()
        val calculator = CostCalculator(registry)

        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Two green creatures = still only 1 color
        driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")
        driver.putCreatureOnBattlefield(activePlayer, "Force of Nature")

        val pummelerDef = registry.requireCard("Wildvine Pummeler")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, pummelerDef, activePlayer)
        effectiveCost.genericAmount shouldBe 5 // 6 - 1 (green counted once)
        effectiveCost.cmc shouldBe 6
    }

    test("three different colors - reduces by 3") {
        val (driver, registry) = createDriverAndRegistry()
        val calculator = CostCalculator(registry)

        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Green + Red + White = 3 colors
        driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")   // Green
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Guide")      // Red
        driver.putCreatureOnBattlefield(activePlayer, "Savannah Lions")    // White

        val pummelerDef = registry.requireCard("Wildvine Pummeler")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, pummelerDef, activePlayer)
        effectiveCost.genericAmount shouldBe 3 // 6 - 3
        effectiveCost.cmc shouldBe 4 // {3}{G}
    }

    test("five colors - reduces by 5") {
        val (driver, registry) = createDriverAndRegistry()
        val calculator = CostCalculator(registry)

        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // All 5 colors
        driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")   // Green
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Guide")      // Red
        driver.putCreatureOnBattlefield(activePlayer, "Savannah Lions")    // White
        driver.putCreatureOnBattlefield(activePlayer, "Black Creature")    // Black
        driver.putCreatureOnBattlefield(activePlayer, "Island Walker")     // Blue

        val pummelerDef = registry.requireCard("Wildvine Pummeler")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, pummelerDef, activePlayer)
        effectiveCost.genericAmount shouldBe 1 // 6 - 5
        effectiveCost.cmc shouldBe 2 // {1}{G}
    }

    test("opponent's colored permanents do not count") {
        val (driver, registry) = createDriverAndRegistry()
        val calculator = CostCalculator(registry)

        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has colored permanents, active player has none
        driver.putCreatureOnBattlefield(driver.player2, "Goblin Guide")
        driver.putCreatureOnBattlefield(driver.player2, "Savannah Lions")

        val pummelerDef = registry.requireCard("Wildvine Pummeler")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, pummelerDef, activePlayer)
        effectiveCost.genericAmount shouldBe 6 // No reduction
        effectiveCost.cmc shouldBe 7
    }

    test("color-changing effect on a colorless permanent contributes to Vivid count") {
        // Foraging Wickermaw repro: an Artifact Creature that's printed colorless but
        // becomes a chosen color until end of turn via its mana ability. The Layer-5
        // color-change effect lives only in projected state; if [countColors] reads
        // base CardComponent.colors it will miss the granted color and Vivid won't
        // discount Wildvine Pummeler. Projected colors must drive the count.
        val (driver, registry) = createDriverAndRegistry()
        val calculator = CostCalculator(registry)

        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wickermaw = driver.putCreatureOnBattlefield(activePlayer, "Foraging Wickermaw")

        // Sanity: Wickermaw is printed colorless, so no Vivid reduction yet.
        val pummelerDef = registry.requireCard("Wildvine Pummeler")
        calculator.calculateEffectiveCost(driver.state, pummelerDef, activePlayer)
            .genericAmount shouldBe 6

        // Apply the same Layer-5 color-change floating effect that
        // BecomeChosenManaColorExecutor produces when the mana ability is activated.
        val ctx = com.wingedsheep.engine.handlers.EffectContext(
            sourceId = wickermaw,
            controllerId = activePlayer,
        )
        driver.replaceState(
            driver.state.addFloatingEffect(
                layer = com.wingedsheep.engine.mechanics.layers.Layer.COLOR,
                modification = com.wingedsheep.engine.mechanics.layers.SerializableModification
                    .ChangeColor(setOf(com.wingedsheep.sdk.core.Color.GREEN.name)),
                affectedEntities = setOf(wickermaw),
                duration = com.wingedsheep.sdk.scripting.Duration.EndOfTurn,
                context = ctx,
            )
        )

        // After the color change, Pummeler's Vivid should discount by 1 (one color among
        // permanents you control).
        val reduced = calculator.calculateEffectiveCost(driver.state, pummelerDef, activePlayer)
        reduced.genericAmount shouldBe 5
        reduced.cmc shouldBe 6 // {5}{G}
    }

    test("colorless permanents do not contribute to color count") {
        val (driver, registry) = createDriverAndRegistry()
        val calculator = CostCalculator(registry)

        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Artifact creature is colorless
        driver.putCreatureOnBattlefield(activePlayer, "Artifact Creature")

        val pummelerDef = registry.requireCard("Wildvine Pummeler")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, pummelerDef, activePlayer)
        effectiveCost.genericAmount shouldBe 6 // No reduction from colorless
        effectiveCost.cmc shouldBe 7
    }

    // ---------------------------------------------------------------------
    // Integration: actually cast Wildvine Pummeler through the engine.
    // These exercise CastFromZoneEnumerator / CastSpellHandler, not just the
    // CostCalculator helper.
    // ---------------------------------------------------------------------

    test("integration: cast Wildvine Pummeler at reduced cost with 3 colors on battlefield") {
        val (driver, _) = createDriverAndRegistry()

        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Three colors on the battlefield → cost should be {3}{G} (CMC 4).
        driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser") // G
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Guide")    // R
        driver.putCreatureOnBattlefield(activePlayer, "Savannah Lions")  // W

        val pummeler = driver.putCardInHand(activePlayer, "Wildvine Pummeler")
        // Exactly {3}{G} of mana — proves the reduction is applied at cast time.
        driver.giveMana(activePlayer, com.wingedsheep.sdk.core.Color.GREEN, 1)
        driver.giveColorlessMana(activePlayer, 3)

        val result = driver.castSpell(activePlayer, pummeler)
        result.error shouldBe null

        driver.bothPass() // resolve the spell
        driver.findPermanent(activePlayer, "Wildvine Pummeler") shouldNotBe null
    }

    test("integration: auto-pay with exactly the reduced number of Forests succeeds") {
        val (driver, _) = createDriverAndRegistry()

        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // 2 colors on the battlefield → cost should be {4}{G} (5 Forests).
        driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser") // G
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Guide")    // R

        // Exactly five untapped Forests — auto-pay must use the reduced cost.
        repeat(5) { driver.putLandOnBattlefield(activePlayer, "Forest") }

        val pummeler = driver.putCardInHand(activePlayer, "Wildvine Pummeler")
        val result = driver.castSpell(activePlayer, pummeler)
        result.error shouldBe null

        driver.bothPass()
        driver.findPermanent(activePlayer, "Wildvine Pummeler") shouldNotBe null
    }

    test("legal-actions: CastSpell.manaCostString reflects the reduced cost so the client renders it") {
        val (driver, registry) = createDriverAndRegistry()

        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // 2 colors → cost should be {4}{G}; the LegalAction's manaCostString must say so.
        driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser") // G
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Guide")    // R

        val pummelerId = driver.putCardInHand(activePlayer, "Wildvine Pummeler")
        // Give the player enough mana so the cast action is marked affordable
        // (legal actions are still emitted with manaCostString even when unaffordable,
        // but giving mana makes the test self-contained).
        driver.giveMana(activePlayer, com.wingedsheep.sdk.core.Color.GREEN, 1)
        driver.giveColorlessMana(activePlayer, 4)

        val enumerator = LegalActionEnumerator.create(registry)
        val actions = enumerator.enumerate(driver.state, activePlayer)
        val castAction = actions.firstOrNull {
            it.actionType == "CastSpell" &&
                (it.action as? CastSpell)?.cardId == pummelerId
        }
        castAction shouldNotBe null
        castAction!!.manaCostString shouldBe "{4}{G}"
    }

    test("integration: cannot cast Wildvine Pummeler if you don't have enough mana even after reduction") {
        val (driver, _) = createDriverAndRegistry()

        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser") // 1 color → reduces to {5}{G}

        val pummeler = driver.putCardInHand(activePlayer, "Wildvine Pummeler")
        // Only {4}{G} — one short of the reduced cost {5}{G}.
        driver.giveMana(activePlayer, com.wingedsheep.sdk.core.Color.GREEN, 1)
        driver.giveColorlessMana(activePlayer, 4)

        val result = driver.castSpell(activePlayer, pummeler)
        result.error shouldNotBe null
    }
})
