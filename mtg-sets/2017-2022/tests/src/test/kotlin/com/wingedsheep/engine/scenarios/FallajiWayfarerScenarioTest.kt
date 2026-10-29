package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fallaji Wayfarer (DMC #11) — {2}{G} Creature — Human Scout 2/4.
 *
 * "Fallaji Wayfarer is all colors. This ability doesn't affect its color identity.
 * Multicolored spells you cast have convoke."
 *
 * Being all colors, the Wayfarer can itself convoke for any colored mana symbol. The grant reads
 * the spell's colors: a multicolored spell gets convoke, a monocolored one doesn't.
 */
class FallajiWayfarerScenarioTest : ScenarioTestBase() {

    private fun board(spell: String, land: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, spell)
        .withCardOnBattlefield(1, "Fallaji Wayfarer")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, land, 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Fallaji Wayfarer") {

            test("is all five colors, with a green color identity") {
                val game = board("Woolly Thoctar", "Mountain")
                val wayfarer = game.findPermanent("Fallaji Wayfarer")!!

                game.state.projectedState.getColors(wayfarer).size shouldBe 5
                cardRegistry.requireCard("Fallaji Wayfarer").colorIdentity shouldBe setOf(Color.GREEN)
            }

            test("a multicolored spell has convoke, and the Wayfarer pays a white symbol") {
                val game = board("Woolly Thoctar", "Mountain")
                val wayfarer = game.findPermanent("Fallaji Wayfarer")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Woolly Thoctar").single(),
                        alternativePayment = AlternativePaymentChoice(
                            convokedCreatures = mapOf(
                                wayfarer to ConvokePayment(color = Color.WHITE),
                                bears to ConvokePayment(color = Color.GREEN)
                            )
                        )
                    )
                )
                withClue("Mountain pays {R}, the convokers pay {G}{W}: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.findPermanent("Woolly Thoctar") shouldNotBe null
            }

            test("a monocolored spell does not have convoke") {
                val game = board("Centaur Courser", "Forest")
                val wayfarer = game.findPermanent("Fallaji Wayfarer")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Centaur Courser").single(),
                        alternativePayment = AlternativePaymentChoice(
                            convokedCreatures = mapOf(
                                wayfarer to ConvokePayment(color = null),
                                bears to ConvokePayment(color = null)
                            )
                        )
                    )
                )
                withClue("Centaur Courser is mono-green, so it can't be convoked") { cast.error shouldNotBe null }
            }
        }
    }
}
