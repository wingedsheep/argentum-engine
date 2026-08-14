package com.wingedsheep.engine.mana

import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.atq.cards.AshnodsAltar
import com.wingedsheep.mtg.sets.definitions.lrw.cards.SpringleafDrum
import com.wingedsheep.mtg.sets.definitions.ons.cards.BirchloreRangers
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [ManaSolver.calculateExplicitActivationBonusMana] — the fourth "extras" helper.
 *
 * `findAvailableManaSources` models exactly three cost shapes (a bare `{T}`, a bare pay-life, and a
 * `{T}`-bearing composite of pay-life/mana parts) and skips everything else. Ward, "counter unless
 * you pay" and "you may pay {N}" all gate the *prompt* on `canPay`, so a player whose only mana
 * source had an unmodelled cost shape was never asked to pay at all.
 *
 * The counting must stay exact in both directions: the new helper has to see Ashnod's Altar, and it
 * must not re-count the sources the other three extras helpers already cover.
 */
class ExplicitActivationManaSourcesTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                AshnodsAltar, BirchloreRangers, SpringleafDrum, PredefinedTokens.Treasure,
            )
        )
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        return driver
    }

    fun solver(driver: GameTestDriver) = ManaSolver(driver.cardRegistry)

    test("Ashnod's Altar makes a cost affordable even though the solver can't tap it") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Ashnod's Altar")
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")

        val s = solver(driver)
        // No lands, so the auto-tap solver finds nothing…
        s.solve(driver.state, player, ManaCost.parse("{2}")) shouldBe null
        // …but "Sacrifice a creature: Add {C}{C}" is a real payment the player can make.
        s.canPay(driver.state, player, ManaCost.parse("{2}")) shouldBe true
        s.calculateExplicitActivationBonusMana(driver.state, player).totalMana shouldBe 2
    }

    test("an altar with nothing to sacrifice pays for nothing") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Ashnod's Altar")

        val s = solver(driver)
        s.calculateExplicitActivationBonusMana(driver.state, player).totalMana shouldBe 0
        s.canPay(driver.state, player, ManaCost.parse("{2}")) shouldBe false
    }

    test("colorless production does not make a colored cost affordable") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Ashnod's Altar")
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")

        val s = solver(driver)
        s.canPay(driver.state, player, ManaCost.parse("{G}")) shouldBe false
        s.canPay(driver.state, player, ManaCost.parse("{1}")) shouldBe true
    }

    test("a tapped altar still works — its cost has no {T}") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val altar = driver.putPermanentOnBattlefield(player, "Ashnod's Altar")
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.tapPermanent(altar)

        solver(driver).calculateExplicitActivationBonusMana(driver.state, player).totalMana shouldBe 2
    }

    context("no double-counting with the other extras helpers") {
        test("Treasure is counted only by the tap+SacrificeSelf helper") {
            val driver = createDriver()
            val player = driver.activePlayer!!
            driver.putPermanentOnBattlefield(player, "Treasure")

            val s = solver(driver)
            s.calculateExplicitActivationBonusMana(driver.state, player).totalMana shouldBe 0
            s.getAvailableManaCount(driver.state, player) shouldBe 1
        }

        test("Birchlore Rangers is counted only by the TapPermanents helper") {
            val driver = createDriver()
            val player = driver.activePlayer!!
            driver.putCreatureOnBattlefield(player, "Birchlore Rangers")
            driver.putCreatureOnBattlefield(player, "Llanowar Elves")

            solver(driver).calculateExplicitActivationBonusMana(driver.state, player)
                .totalMana shouldBe 0
        }

        test("Springleaf Drum is counted only by the composite TapPermanents helper") {
            val driver = createDriver()
            val player = driver.activePlayer!!
            driver.putPermanentOnBattlefield(player, "Springleaf Drum")
            driver.putCreatureOnBattlefield(player, "Grizzly Bears")

            solver(driver).calculateExplicitActivationBonusMana(driver.state, player)
                .totalMana shouldBe 0
        }

        test("plain lands and mana dorks are counted only as ordinary sources") {
            val driver = createDriver()
            val player = driver.activePlayer!!
            driver.putPermanentOnBattlefield(player, "Forest")
            driver.removeSummoningSickness(driver.putCreatureOnBattlefield(player, "Llanowar Elves"))
            driver.removeSummoningSickness(driver.putCreatureOnBattlefield(player, "Birds of Paradise"))

            val s = solver(driver)
            s.calculateExplicitActivationBonusMana(driver.state, player).totalMana shouldBe 0
            s.getAvailableManaCount(driver.state, player) shouldBe 3
        }
    }

    test("floating mana plus an altar covers a cost neither covers alone") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Ashnod's Altar")
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.giveMana(player, Color.GREEN, 1)

        solver(driver).canPay(driver.state, player, ManaCost.parse("{2}{G}")) shouldBe true
    }
})
