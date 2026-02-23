package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for mana sources that produce multiple mana per tap (e.g., Elvish Aberration {T}: Add {G}{G}{G}).
 * Verifies that auto-pay correctly accounts for multi-mana production.
 */
class MultiManaSourceTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    context("multi-mana source auto-pay") {

        test("Elvish Aberration produces 3 green mana for auto-pay") {
            val driver = createDriver()
            driver.initMirrorMatch(
                deck = Deck.of("Forest" to 20),
                skipMulligans = true
            )

            val activePlayer = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            // Put Elvish Aberration on battlefield without summoning sickness
            val aberration = driver.putCreatureOnBattlefield(activePlayer, "Elvish Aberration")
            driver.removeSummoningSickness(aberration)

            // Put 2 forests on battlefield
            repeat(2) {
                driver.putLandOnBattlefield(activePlayer, "Forest")
            }

            // Put Force of Nature ({3}{G}{G} = 5 CMC) in hand
            val force = driver.putCardInHand(activePlayer, "Force of Nature")

            // Elvish Aberration provides 3 green + 2 forests = 5 total mana
            // Without the fix, Aberration would only count as 1, giving 3 total (not enough)
            val castResult = driver.submit(
                CastSpell(
                    playerId = activePlayer,
                    cardId = force,
                    paymentStrategy = PaymentStrategy.AutoPay
                )
            )

            castResult.isSuccess shouldBe true
            driver.getTopOfStackName() shouldBe "Force of Nature"
        }

        test("Elvish Aberration + 4 forests can cast a 7-CMC spell") {
            val driver = createDriver()
            driver.initMirrorMatch(
                deck = Deck.of("Forest" to 20),
                skipMulligans = true
            )

            val activePlayer = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            // Put Elvish Aberration on battlefield without summoning sickness
            val aberration = driver.putCreatureOnBattlefield(activePlayer, "Elvish Aberration")
            driver.removeSummoningSickness(aberration)

            // Put 4 more forests on battlefield (3 from Aberration + 4 from forests = 7)
            repeat(4) {
                driver.putLandOnBattlefield(activePlayer, "Forest")
            }

            // Elvish Aberration itself costs {5}{G} — cast another from hand
            val aberration2 = driver.putCardInHand(activePlayer, "Elvish Aberration")

            val castResult = driver.submit(
                CastSpell(
                    playerId = activePlayer,
                    cardId = aberration2,
                    paymentStrategy = PaymentStrategy.AutoPay
                )
            )

            // 3 (Aberration) + 4 (Forests) = 7 mana, Elvish Aberration costs {5}{G} = 6 CMC
            castResult.isSuccess shouldBe true
            driver.getTopOfStackName() shouldBe "Elvish Aberration"
        }

        test("auto-pay fails when multi-mana source is not enough") {
            val driver = createDriver()
            driver.initMirrorMatch(
                deck = Deck.of("Forest" to 20),
                skipMulligans = true
            )

            val activePlayer = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            // Only Elvish Aberration (3 mana) — no forests
            val aberration = driver.putCreatureOnBattlefield(activePlayer, "Elvish Aberration")
            driver.removeSummoningSickness(aberration)

            // Force of Nature costs {3}{G}{G} = 5 CMC, but we only have 3 green
            val force = driver.putCardInHand(activePlayer, "Force of Nature")

            val castResult = driver.submit(
                CastSpell(
                    playerId = activePlayer,
                    cardId = force,
                    paymentStrategy = PaymentStrategy.AutoPay
                )
            )

            castResult.isSuccess shouldBe false
        }
    }

    context("ManaSolver multi-mana source correctness") {

        test("4 forests + Elvish Aberration taps Aberration + 2 forests for 5 CMC") {
            val driver = createDriver()
            driver.initMirrorMatch(
                deck = Deck.of("Forest" to 20),
                skipMulligans = true
            )

            val activePlayer = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            // 4 forests + Elvish Aberration = 4 + 3 = 7 available mana, need 5
            // Solver should tap Aberration (3) + 2 forests (2) = 3 sources
            // NOT all 4 forests + Aberration = 5 sources (wasteful)
            val aberration = driver.putCreatureOnBattlefield(activePlayer, "Elvish Aberration")
            driver.removeSummoningSickness(aberration)
            repeat(4) {
                driver.putLandOnBattlefield(activePlayer, "Forest")
            }

            val registry = CardRegistry()
            registry.register(TestCards.all)
            val solver = ManaSolver(cardRegistry = registry)
            val solution = solver.solve(driver.state, activePlayer, ManaCost.parse("{3}{G}{G}"))

            solution shouldNotBe null
            solution!!.sources.size shouldBe 3
        }

        test("5 forests + Elvish Aberration taps 5 forests to preserve creature for attacks") {
            val driver = createDriver()
            driver.initMirrorMatch(
                deck = Deck.of("Forest" to 20),
                skipMulligans = true
            )

            val activePlayer = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            // 5 forests are enough for 5 CMC — Aberration should stay untapped for attacks
            val aberration = driver.putCreatureOnBattlefield(activePlayer, "Elvish Aberration")
            driver.removeSummoningSickness(aberration)
            repeat(5) {
                driver.putLandOnBattlefield(activePlayer, "Forest")
            }

            val registry = CardRegistry()
            registry.register(TestCards.all)
            val solver = ManaSolver(cardRegistry = registry)
            val solution = solver.solve(driver.state, activePlayer, ManaCost.parse("{3}{G}{G}"))

            solution shouldNotBe null
            solution!!.sources.size shouldBe 5
            // Aberration should not be in the tapped sources
            solution.sources.none { it.entityId == aberration } shouldBe true
        }

        test("getAvailableManaCount includes multi-mana production") {
            val driver = createDriver()
            driver.initMirrorMatch(
                deck = Deck.of("Forest" to 20),
                skipMulligans = true
            )

            val activePlayer = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            // Put Elvish Aberration (3 green) + 2 forests on battlefield
            val aberration = driver.putCreatureOnBattlefield(activePlayer, "Elvish Aberration")
            driver.removeSummoningSickness(aberration)
            repeat(2) {
                driver.putLandOnBattlefield(activePlayer, "Forest")
            }

            val registry = CardRegistry()
            registry.register(TestCards.all)
            val solver = ManaSolver(cardRegistry = registry)
            val count = solver.getAvailableManaCount(driver.state, activePlayer)

            // 3 (Aberration) + 2 (Forests) = 5
            count shouldBe 5
        }

        test("Palladium Myr producing 2 colorless counts correctly") {
            val driver = createDriver()
            driver.initMirrorMatch(
                deck = Deck.of("Forest" to 20),
                skipMulligans = true
            )

            val activePlayer = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val myr = driver.putCreatureOnBattlefield(activePlayer, "Palladium Myr")
            driver.removeSummoningSickness(myr)

            val registry = CardRegistry()
            registry.register(TestCards.all)
            val solver = ManaSolver(cardRegistry = registry)
            val count = solver.getAvailableManaCount(driver.state, activePlayer)

            // Palladium Myr produces 2 colorless
            count shouldBe 2
        }
    }
})
