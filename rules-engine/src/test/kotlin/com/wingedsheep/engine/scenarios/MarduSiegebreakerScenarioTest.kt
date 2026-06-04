package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Mardu Siegebreaker (TDM #206, {1}{R}{W}{B}, 4/4).
 *
 * Deathtouch, haste.
 * When this creature enters, exile up to one other target creature you control until this
 * creature leaves the battlefield.
 * Whenever this creature attacks, for each opponent, create a tapped token that's a copy of the
 * exiled card attacking that opponent. At the beginning of your next end step, sacrifice those tokens.
 *
 * The novel piece is the attack trigger: it gathers the linked-exiled card and creates a tapped,
 * attacking token copy of it via [com.wingedsheep.sdk.dsl.Effects.CreateTokenCopyOfTarget] with
 * `sacrificeAtStep = Step.END` (your next end step). These tests pin the ETB exile/link, the token
 * copy creation, and the end-step sacrifice.
 */
class MarduSiegebreakerScenarioTest : ScenarioTestBase() {

    init {
        context("Mardu Siegebreaker") {

            fun newGameWithExiledBears() = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Mardu Siegebreaker")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = false, summoningSickness = false)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("ETB exiles the chosen other creature you control, linked to Mardu") {
                val game = newGameWithExiledBears()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Mardu Siegebreaker")
                game.resolveStack() // Mardu enters → ETB trigger needs a target
                game.selectTargets(listOf(bears))
                game.resolveStack() // resolve ETB → bears exiled and linked

                withClue("Grizzly Bears is no longer on the battlefield") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
                withClue("Grizzly Bears is in player 1's exile") {
                    game.state.getExile(game.player1Id).any { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
                    } shouldBe true
                }
                val mardu = game.findPermanent("Mardu Siegebreaker")!!
                val linked = game.state.getEntity(mardu)?.get<LinkedExileComponent>()
                withClue("Mardu has the bears linked in exile") {
                    (linked?.exiledIds?.contains(bears)) shouldBe true
                }
            }

            test("attacking creates a tapped, attacking token copy of the exiled creature") {
                val game = newGameWithExiledBears()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Mardu Siegebreaker")
                game.resolveStack()
                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("No Grizzly Bears on the battlefield before the attack (original is exiled)") {
                    game.findPermanents("Grizzly Bears").size shouldBe 0
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                // Mardu has haste, so it can attack the turn it entered.
                val attack = game.declareAttackers(mapOf("Mardu Siegebreaker" to 2))
                withClue("Declaring Mardu as an attacker should succeed: ${attack.error}") {
                    attack.error shouldBe null
                }
                game.resolveStack() // resolve the attack trigger → token copy of the bears

                val tokens = game.findPermanents("Grizzly Bears")
                withClue("A token copy of the exiled Grizzly Bears was created") {
                    tokens.size shouldBe 1
                }
                withClue("The token copy is tapped and attacking") {
                    val token = tokens.first()
                    game.state.getEntity(token)?.has<TappedComponent>() shouldBe true
                    game.state.getEntity(token)?.has<AttackingComponent>() shouldBe true
                }
            }

            test("the token copy is sacrificed at your next end step") {
                val game = newGameWithExiledBears()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Mardu Siegebreaker")
                game.resolveStack()
                game.selectTargets(listOf(bears))
                game.resolveStack()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Mardu Siegebreaker" to 2))
                game.resolveStack()

                withClue("The token copy exists during combat") {
                    game.findPermanents("Grizzly Bears").size shouldBe 1
                }

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                withClue("The token copy is sacrificed by the next end step") {
                    game.findPermanents("Grizzly Bears").size shouldBe 0
                }
            }

            test("declining the optional ETB target creates no token on attack") {
                val game = newGameWithExiledBears()

                game.castSpell(1, "Mardu Siegebreaker")
                game.resolveStack()
                // "up to one" — decline by selecting no targets.
                game.selectTargets(emptyList())
                game.resolveStack()

                withClue("Grizzly Bears stays on the battlefield when the ETB target is declined") {
                    game.findPermanent("Grizzly Bears") shouldNotBe null
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Mardu Siegebreaker" to 2))
                game.resolveStack()

                withClue("No extra token copy is created when nothing was exiled") {
                    // Only the original Grizzly Bears remains; no token copy.
                    game.findPermanents("Grizzly Bears").size shouldBe 1
                }
            }
        }
    }
}
