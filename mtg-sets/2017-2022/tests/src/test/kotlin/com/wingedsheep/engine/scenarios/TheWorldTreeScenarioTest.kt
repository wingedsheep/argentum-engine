package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The World Tree (KHM #275) — Land
 *
 *   This land enters tapped.
 *   {T}: Add {G}.
 *   As long as you control six or more lands, lands you control have "{T}: Add one mana of any color."
 *   {W}{W}{U}{U}{B}{B}{R}{R}{G}{G}, {T}, Sacrifice this land: Search your library for any number of
 *   God cards, put them onto the battlefield, then shuffle.
 *
 * The conditional grant has to reach the auto-payer, not just the manual mana-ability list: with
 * only Forests and the Tree, a blue spell is castable only once the sixth land is in play.
 */
class TheWorldTreeScenarioTest : ScenarioTestBase() {

    private fun board(forests: Int) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "The World Tree")
        .withLandsOnBattlefield(1, "Forest", forests)
        .withCardInHand(1, "Divination")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("The World Tree — six-land any-color grant") {

            test("with six lands, the auto-payer taps Forests for blue") {
                val game = board(forests = 5)

                game.castSpell(1, "Divination").error shouldBe null
                game.resolveStack()

                withClue("Divination resolved off Forests granted any-color mana") {
                    game.isInGraveyard(1, "Divination") shouldBe true
                }
            }

            test("with five lands, the grant is off and blue can't be paid") {
                val game = board(forests = 4)

                game.castSpell(1, "Divination").error shouldNotBe null
                withClue("the spell stays in hand") {
                    game.isInGraveyard(1, "Divination") shouldBe false
                }
            }
        }

        context("The World Tree — sacrifice to search for Gods") {

            test("ten granted lands pay WWUUBBRRGG; any number of Gods go onto the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "The World Tree")
                    .withLandsOnBattlefield(1, "Forest", 10)
                    .withCardInLibrary(1, "Nylea, God of the Hunt")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tree = game.findPermanent("The World Tree")!!
                val abilityId = cardRegistry.getCard("The World Tree")!!.script.activatedAbilities[1].id

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = tree, abilityId = abilityId)
                ).error shouldBe null
                withClue("the Tree is sacrificed as part of the cost") {
                    game.isInGraveyard(1, "The World Tree") shouldBe true
                }
                game.resolveStack()

                val nylea = game.state.getLibrary(game.player1Id).firstOrNull {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Nylea, God of the Hunt"
                }.shouldNotBeNull()
                game.selectCards(listOf(nylea))
                game.resolveStack()

                withClue("the God is on the battlefield; the non-God stays in the library") {
                    game.findPermanent("Nylea, God of the Hunt") shouldNotBe null
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
            }
        }
    }
}
