package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Lita, Mechanical Engineer (J22 #6).
 *
 *   Vigilance
 *   At the beginning of your end step, untap each other artifact creature you control.
 *   {3}{W}, {T}: Create a 5/5 colorless Vehicle artifact token named Zeppelin with flying and crew 3.
 *
 * The end-step untap reaches only *other* artifact creatures *you* control — not Lita, not a
 * noncreature artifact, not a non-artifact creature, not an opponent's artifact creature. The
 * Zeppelin is a noncreature Vehicle until crewed; crew 3 needs total power 3.
 */
class LitaMechanicalEngineerScenarioTest : ScenarioTestBase() {

    private fun TestGame.isTapped(id: EntityId) = state.getEntity(id)?.has<TappedComponent>() == true

    init {
        context("Lita, Mechanical Engineer") {

            test("at your end step untaps each other artifact creature you control, and nothing else") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Lita, Mechanical Engineer", tapped = true)
                    .withCardOnBattlefield(1, "Ornithopter", tapped = true)
                    .withCardOnBattlefield(1, "Millstone", tapped = true)
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                    .withCardOnBattlefield(2, "Memnite", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lita = game.findPermanent("Lita, Mechanical Engineer")!!
                val ornithopter = game.findPermanent("Ornithopter")!!
                val millstone = game.findPermanent("Millstone")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val memnite = game.findPermanent("Memnite")!!

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Your other artifact creature untaps") { game.isTapped(ornithopter) shouldBe false }
                withClue("Lita herself stays tapped") { game.isTapped(lita) shouldBe true }
                withClue("A noncreature artifact stays tapped") { game.isTapped(millstone) shouldBe true }
                withClue("A non-artifact creature stays tapped") { game.isTapped(bears) shouldBe true }
                withClue("An opponent's artifact creature stays tapped") { game.isTapped(memnite) shouldBe true }
            }

            test("does not trigger at the opponent's end step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Lita, Mechanical Engineer")
                    .withCardOnBattlefield(1, "Ornithopter", tapped = true)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ornithopter = game.findPermanent("Ornithopter")!!

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                game.isTapped(ornithopter) shouldBe true
            }

            test("{3}{W}, {T} creates a 5/5 flying Zeppelin Vehicle that crew 3 animates") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Lita, Mechanical Engineer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lita = game.findPermanent("Lita, Mechanical Engineer")!!
                val abilityId = cardRegistry.getCard("Lita, Mechanical Engineer")!!
                    .script.activatedAbilities[0].id

                val activate = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = lita, abilityId = abilityId)
                )
                withClue("Activating Lita should succeed: ${activate.error}") { activate.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("The {T} cost taps Lita") { game.isTapped(lita) shouldBe true }

                val zeppelin = game.findPermanent("Zeppelin")
                zeppelin shouldNotBe null
                zeppelin!!
                val projected = game.state.projectedState
                withClue("Zeppelin is a token artifact Vehicle, not yet a creature") {
                    game.state.getEntity(zeppelin)?.has<TokenComponent>() shouldBe true
                    projected.hasType(zeppelin, "ARTIFACT") shouldBe true
                    projected.hasSubtype(zeppelin, "Vehicle") shouldBe true
                    projected.isCreature(zeppelin) shouldBe false
                    projected.getColors(zeppelin) shouldBe emptySet()
                }

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Crew 3 can't be paid with total power 2") {
                    game.execute(CrewVehicle(game.player1Id, zeppelin, listOf(bears))).error shouldNotBe null
                }

                val giant = game.findPermanent("Hill Giant")!!
                withClue("Crew 3 is paid with a 3-power creature") {
                    game.execute(CrewVehicle(game.player1Id, zeppelin, listOf(giant))).error shouldBe null
                }
                game.resolveStack()

                val crewed = game.state.projectedState
                withClue("Crewed Zeppelin is a 5/5 flying artifact creature") {
                    crewed.isCreature(zeppelin) shouldBe true
                    crewed.getPower(zeppelin) shouldBe 5
                    crewed.getToughness(zeppelin) shouldBe 5
                    crewed.hasKeyword(zeppelin, Keyword.FLYING) shouldBe true
                }
                game.isTapped(giant) shouldBe true
            }
        }
    }
}
