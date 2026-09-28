package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scrollshift ({2}{W} instant): "Exile up to one target artifact, creature, or enchantment you
 * control, then return it to the battlefield under its owner's control. Draw a card."
 */
class ScrollshiftScenarioTest : ScenarioTestBase() {

    init {
        context("Scrollshift") {

            test("blinks a creature you control, retriggering its ETB, then draws a card") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Scrollshift")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(1, "Elvish Visionary", tapped = true)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .build()

                val visionary = game.findPermanent("Elvish Visionary")!!
                val handBefore = game.handSize(1)

                val cast = game.castSpell(1, "Scrollshift", targetId = visionary)
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val returned = game.findPermanent("Elvish Visionary")
                withClue("Elvish Visionary is back on the battlefield") { (returned != null) shouldBe true }
                withClue("The returned permanent is untapped") { game.state.getEntity(returned!!)!!.get<TappedComponent>() shouldBe null }
                withClue("Scrollshift left hand (-1), ETB draw (+1), spell draw (+1)") {
                    game.handSize(1) shouldBe handBefore - 1 + 2
                }
            }

            test("can be cast with no target and still draws a card") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Scrollshift")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .build()

                val handBefore = game.handSize(1)
                val cast = game.castSpell(1, "Scrollshift")
                withClue("Casting with no target should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.handSize(1) shouldBe handBefore
            }

            test("cannot target a creature an opponent controls") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Scrollshift")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val cast = game.castSpell(1, "Scrollshift", targetId = bears)
                cast.error shouldNotBe null
            }
        }
    }
}
