package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Lander tokens.
 *
 * Lander tokens are artifacts with:
 * "{2}, {T}, Sacrifice this token: Search your library for a basic land card,
 * put it onto the battlefield tapped, then shuffle."
 */
class LanderTokenScenarioTest : ScenarioTestBase() {

    // Test card that creates a Lander token on ETB
    private val landerMaker = card("Lander Maker") {
        manaCost = "{G}"
        typeLine = "Creature - Scout"
        power = 1
        toughness = 1

        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            effect = Effects.CreateLander()
        }
    }

    init {
        cardRegistry.register(landerMaker)

        test("ETB creates a Lander token on the battlefield") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Lander Maker")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            // Cast Lander Maker
            game.castSpell(1, "Lander Maker")
            game.resolveStack()

            // Lander token should exist on battlefield
            withClue("Lander token should be on the battlefield") {
                game.isOnBattlefield("Lander") shouldBe true
            }
        }

        test("Lander token can activate to search for a basic land") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Lander Maker")
                .withLandsOnBattlefield(1, "Forest", 3) // need {G} to cast + {2} to activate
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Plains")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            // Cast Lander Maker
            game.castSpell(1, "Lander Maker")
            game.resolveStack()

            // Lander token should exist
            val landerId = game.findPermanent("Lander")
            landerId.shouldNotBeNull()

            // Look up the Lander's activated ability
            val landerDef = cardRegistry.getCard("Lander")!!
            val ability = landerDef.script.activatedAbilities.first()

            // Activate the Lander: {2}, {T}, Sacrifice
            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = landerId,
                    abilityId = ability.id
                )
            )

            withClue("Lander ability should activate successfully: ${result.error}") {
                result.error shouldBe null
            }

            // Lander should be sacrificed (moved to graveyard)
            withClue("Lander should be sacrificed") {
                game.isOnBattlefield("Lander") shouldBe false
            }

            // Resolve the ability on the stack
            game.resolveStack()

            // Should have a library search decision
            withClue("Should have pending library search decision") {
                game.hasPendingDecision() shouldBe true
            }

            val decision = game.getPendingDecision()!! as SelectCardsDecision

            // Both Swamp and Forest should be available (both are basic lands)
            withClue("Basic lands should be available in search") {
                decision.cardInfo!!.values.any { it.name == "Swamp" } shouldBe true
                decision.cardInfo!!.values.any { it.name == "Forest" } shouldBe true
            }

            // Select the Swamp
            val swampId = decision.cardInfo!!.entries.first { it.value.name == "Swamp" }.key
            game.selectCards(listOf(swampId))

            // Swamp should be on the battlefield (tapped, per Lander rules)
            withClue("Swamp should be on the battlefield") {
                game.isOnBattlefield("Swamp") shouldBe true
            }
        }

        test("Lander token has summoning sickness and cannot activate immediately") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Lander Maker")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            // Cast Lander Maker
            game.castSpell(1, "Lander Maker")
            game.resolveStack()

            val landerId = game.findPermanent("Lander")
            landerId.shouldNotBeNull()

            // Lander is an artifact (not a creature), so it should NOT have summoning sickness
            // and should be activatable immediately — the tap cost should be payable.
            val landerDef = cardRegistry.getCard("Lander")!!
            val ability = landerDef.script.activatedAbilities.first()

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = landerId,
                    abilityId = ability.id
                )
            )

            // Artifacts don't have summoning sickness, so this should succeed
            withClue("Lander (artifact) should be activatable immediately: ${result.error}") {
                result.error shouldBe null
            }
        }
    }
}
