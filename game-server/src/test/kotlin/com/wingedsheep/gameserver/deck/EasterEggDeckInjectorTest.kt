package com.wingedsheep.gameserver.deck

import com.wingedsheep.gameserver.config.GameProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe

/**
 * Unit tests for [EasterEggDeckInjector] — Rick's Selesnya deck gets a house guest.
 */
class EasterEggDeckInjectorTest : FunSpec({

    val sekshaas = "Sekshaas, Early Sleeper"

    test("Rick's Forest + Plains deck gets Sekshaas") {
        val deck = mapOf("Forest" to 8, "Plains" to 8, "Llanowar Elves" to 4)

        EasterEggDeckInjector.maybeInjectEasterEggs("Rick", deck, enabled = true) shouldBe deck + (sekshaas to 1)
    }

    test("the player name matches case-insensitively") {
        val deck = mapOf("Forest" to 8, "Plains" to 8)

        EasterEggDeckInjector.maybeInjectEasterEggs("rICk", deck, enabled = true) shouldContainKey sekshaas
    }

    // BoosterGenerator.withBasicLandArt rewrites basics to `Name#SetCode-CollectorNumber` before the
    // injector sees the deck, so matching raw keys silently disabled the egg for every limited game.
    test("basics carrying a printing suffix still count as Forest and Plains") {
        val deck = mapOf("Forest#FDN-278" to 8, "Plains#FDN-272" to 8)

        EasterEggDeckInjector.maybeInjectEasterEggs("Rick", deck, enabled = true) shouldContainKey sekshaas
    }

    test("a deck missing either colour of basic is left alone") {
        val monoGreen = mapOf("Forest#FDN-278" to 17, "Llanowar Elves" to 4)

        EasterEggDeckInjector.maybeInjectEasterEggs("Rick", monoGreen, enabled = true) shouldBe monoGreen
    }

    test("nobody else gets the easter egg") {
        val deck = mapOf("Forest" to 8, "Plains" to 8)

        EasterEggDeckInjector.maybeInjectEasterEggs("Vincent", deck, enabled = true) shouldBe deck
    }

    test("a card whose name merely starts with a basic land name does not count") {
        val deck = mapOf("Forestwalker" to 4, "Plains" to 8)

        EasterEggDeckInjector.maybeInjectEasterEggs("Rick", deck, enabled = true) shouldBe deck
    }

    test("the kill switch leaves an otherwise-qualifying deck alone") {
        val deck = mapOf("Forest" to 8, "Plains" to 8, "Llanowar Elves" to 4)

        EasterEggDeckInjector.maybeInjectEasterEggs("Rick", deck, enabled = false) shouldBe deck
    }

    // Production passes no GAME_EASTER_EGGS_ENABLED, so the property default is what keeps the eggs
    // off there — a default flip would silently re-enable them on the next deploy.
    test("easter eggs are off unless configuration opts in") {
        GameProperties().easterEggs.enabled shouldBe false
    }
})
