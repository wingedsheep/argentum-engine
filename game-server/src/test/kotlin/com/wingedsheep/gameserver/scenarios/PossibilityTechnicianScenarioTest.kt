package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Possibility Technician.
 *
 * Card reference:
 * - Possibility Technician ({2}{R}): 3/3 Creature — Kavu Artificer
 *   "Whenever this creature or another Kavu you control enters, exile the top card of your
 *    library. For as long as that card remains exiled, you may play it if you control a Kavu.
 *    Warp {1}{R}"
 *
 * Exercises the new conditional gate on `GrantMayPlayFromExileEffect.condition` /
 * `MayPlayFromExileComponent.condition` — the play permission persists with the card in
 * exile, but is only honored while the controller has a Kavu in play.
 */
class PossibilityTechnicianScenarioTest : ScenarioTestBase() {

    init {
        context("Possibility Technician — conditional impulse-draw") {

            test("ETB exiles top of library and may-play permission is granted with Kavu condition") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Possibility Technician")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Shock") // top of library — gets exiled
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Possibility Technician")
                withClue("Cast should succeed: ${castResult.error}") { castResult.error shouldBe null }
                game.resolveStack() // resolve cast
                game.resolveStack() // resolve ETB trigger

                withClue("Possibility Technician should be on the battlefield") {
                    game.isOnBattlefield("Possibility Technician") shouldBe true
                }
                val shockId = game.state.getExile(game.player1Id).first()
                withClue("Shock should be in exile with may-play permission") {
                    val mayPlay = game.state.getEntity(shockId)!!.get<MayPlayFromExileComponent>()
                    mayPlay shouldNotBe null
                    mayPlay!!.controllerId shouldBe game.player1Id
                    mayPlay.permanent shouldBe true
                    mayPlay.condition shouldNotBe null
                }
            }

            test("can cast exiled card while a Kavu (the Technician itself) is in play") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Possibility Technician")
                    .withLandsOnBattlefield(1, "Mountain", 4) // 3 to cast Tech, 1 to cast Shock
                    .withCardInLibrary(1, "Shock")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardOnBattlefield(2, "Glory Seeker") // Shock target
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Possibility Technician")
                game.resolveStack()
                game.resolveStack() // ETB trigger resolves, Shock exiled

                val seekerId = game.findPermanent("Glory Seeker")!!
                val shockResult = game.castSpellFromExile(1, "Shock", seekerId)
                withClue("Should be able to cast Shock from exile while Kavu is in play: ${shockResult.error}") {
                    shockResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Glory Seeker (2/2) should be dead from 2 Shock damage") {
                    game.isInGraveyard(2, "Glory Seeker") shouldBe true
                }
            }

            test("cannot cast exiled card after the only Kavu leaves play") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Possibility Technician")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Shock")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardOnBattlefield(2, "Glory Seeker") // hypothetical Shock target
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Possibility Technician")
                game.resolveStack()
                game.resolveStack() // ETB trigger resolves, Shock exiled

                val techId = game.findPermanent("Possibility Technician")!!
                // Move Possibility Technician to graveyard — no Kavu remains in play.
                game.state = game.state.moveToZone(
                    techId,
                    ZoneKey(game.player1Id, Zone.BATTLEFIELD),
                    ZoneKey(game.player1Id, Zone.GRAVEYARD)
                )

                withClue("Possibility Technician should be in graveyard") {
                    game.isInGraveyard(1, "Possibility Technician") shouldBe true
                }

                val seekerId = game.findPermanent("Glory Seeker")!!
                val shockResult = game.castSpellFromExile(1, "Shock", seekerId)
                withClue("Should NOT be able to cast Shock from exile without a Kavu in play") {
                    shockResult.error shouldNotBe null
                }
            }

            test("ETB of another Kavu also triggers — exercises the 'or another Kavu' filter half") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Possibility Technician") // existing Kavu
                    .withCardInHand(1, "Possibility Technician")        // a second Kavu to cast
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Shock")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val exileBefore = game.state.getExile(game.player1Id).size
                game.castSpell(1, "Possibility Technician")
                game.resolveStack() // resolve the spell
                game.resolveStack() // resolve first triggered ability
                game.resolveStack() // resolve second triggered ability if any

                val exileAfter = game.state.getExile(game.player1Id).size
                // When the new Tech enters, the existing Tech's ETB ability also triggers
                // ("Whenever this creature OR another Kavu you control enters"), so two
                // exile-and-may-play triggers fire — one for the entering creature, one
                // for the existing creature observing another Kavu's entry.
                withClue("Both Kavus' triggers should fire — exile two cards (1 from each trigger)") {
                    (exileAfter - exileBefore) shouldBe 2
                }
            }
        }
    }
}
