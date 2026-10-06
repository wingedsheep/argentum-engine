package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ram Through (IKO #170) — {1}{G} Instant.
 *
 *   Target creature you control deals damage equal to its power to target creature you don't
 *   control. If the creature you control has trample, excess damage is dealt to that creature's
 *   controller instead.
 *
 * The bite is sourced from the creature you control, and the excess-to-controller routing is
 * gated on that creature having trample as the spell resolves.
 */
class RamThroughScenarioTest : ScenarioTestBase() {

    private val deathtouchTrampler = card("Test Deathtouch Trampler") {
        manaCost = "{3}{G}"
        typeLine = "Creature — Beast"
        power = 4
        toughness = 4
        keywords(Keyword.DEATHTOUCH, Keyword.TRAMPLE)
    }

    private val lifelinker = card("Test Lifelinker") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Beast"
        power = 3
        toughness = 3
        keywords(Keyword.LIFELINK)
    }

    private fun cast(game: TestGame, mine: String, theirs: String) {
        val spell = game.findCardsInHand(1, "Ram Through").first()
        game.execute(
            CastSpell(
                game.player1Id,
                spell,
                listOf(
                    ChosenTarget.Permanent(game.findPermanent(mine)!!),
                    ChosenTarget.Permanent(game.findPermanent(theirs)!!)
                )
            )
        ).error shouldBe null
        game.resolveStack()
    }

    private fun board(mine: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Ram Through")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withCardOnBattlefield(1, mine)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        cardRegistry.register(deathtouchTrampler)
        cardRegistry.register(lifelinker)

        context("Ram Through") {

            test("a trampler deals the excess past lethal to the bitten creature's controller") {
                val game = board("Colossal Dreadmaw") // 6/6 trample
                cast(game, "Colossal Dreadmaw", "Grizzly Bears")

                withClue("the 2/2 takes lethal and dies") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("6 power minus 2 lethal = 4 excess to its controller") {
                    game.getLifeTotal(2) shouldBe 16
                }
                game.getLifeTotal(1) shouldBe 20
            }

            test("a deathtouch trampler needs only 1 damage for lethal, so the rest is excess") {
                val game = board("Test Deathtouch Trampler") // 4/4 deathtouch, trample
                cast(game, "Test Deathtouch Trampler", "Grizzly Bears")

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                withClue("the creature is the source: 1 lethal (deathtouch), 3 excess to its controller") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("without trample all the damage goes to the creature") {
                val game = board("Craw Wurm") // 6/4, no trample
                cast(game, "Craw Wurm", "Grizzly Bears")

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                withClue("no excess is routed to the controller without trample") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("the damage is dealt by the creature, so its lifelink applies") {
                val game = board("Test Lifelinker") // 3/3 lifelink
                cast(game, "Test Lifelinker", "Grizzly Bears")

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                withClue("lifelink on the biting creature gains its controller 3") {
                    game.getLifeTotal(1) shouldBe 23
                }
            }
        }
    }
}
