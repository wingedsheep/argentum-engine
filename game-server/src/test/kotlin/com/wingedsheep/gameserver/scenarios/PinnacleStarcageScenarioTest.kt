package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Pinnacle Starcage.
 *
 * Card reference:
 * - Pinnacle Starcage ({1}{W}{W}): Artifact
 *   "When this artifact enters, exile all artifacts and creatures with mana value 2 or less
 *    until this artifact leaves the battlefield."
 *   "{6}{W}{W}: Put each card exiled with this artifact into its owner's graveyard, then
 *    create a 2/2 colorless Robot artifact creature token for each card put into a graveyard
 *    this way. Sacrifice this artifact."
 */
class PinnacleStarcageScenarioTest : ScenarioTestBase() {

    init {
        context("Pinnacle Starcage ETB — exiles artifacts and creatures with mana value 2 or less") {

            test("exiles a low-MV creature and a low-MV artifact, leaves high-MV creatures and lands alone") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Pinnacle Starcage")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(2, "Hullcarver")          // {B} 1/1 artifact creature (MV 1) — exiled
                    .withCardOnBattlefield(2, "Cryogen Relic")       // {1}{U} artifact (MV 2)          — exiled
                    .withCardOnBattlefield(2, "Hill Giant")          // {3}{R} 3/3 creature (MV 4)      — NOT exiled
                    .withLandsOnBattlefield(2, "Forest", 1)          // Land (MV 0)                     — NOT exiled (filter requires artifact|creature)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Pinnacle Starcage")
                withClue("Casting Pinnacle Starcage should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Pinnacle Starcage should be on the battlefield") {
                    game.isOnBattlefield("Pinnacle Starcage") shouldBe true
                }
                withClue("Hullcarver should be exiled (artifact creature, MV 1)") {
                    game.isOnBattlefield("Hullcarver") shouldBe false
                    isInExile(game, "Hullcarver") shouldBe true
                }
                withClue("Cryogen Relic should be exiled (artifact, MV 2)") {
                    game.isOnBattlefield("Cryogen Relic") shouldBe false
                    isInExile(game, "Cryogen Relic") shouldBe true
                }
                withClue("Hill Giant should NOT be exiled (MV 4)") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
                withClue("Forest should NOT be exiled (lands are excluded by the artifact|creature filter)") {
                    game.isOnBattlefield("Forest") shouldBe true
                }
            }
        }

        context("Pinnacle Starcage activated ability — graveyard routing, token creation, sacrifice") {

            test("each exiled card goes to its owner's graveyard, a 2/2 Robot is created per card, and Starcage is sacrificed") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Pinnacle Starcage")
                    .withLandsOnBattlefield(1, "Plains", 11)          // 3 to cast + 8 to activate ({6}{W}{W})
                    .withCardOnBattlefield(2, "Hullcarver")           // {B} artifact creature, owned by P2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Starcage; ETB exiles P2's Hullcarver.
                game.castSpell(1, "Pinnacle Starcage").error shouldBe null
                game.resolveStack()
                withClue("Hullcarver should be exiled by Starcage's ETB") {
                    isInExile(game, "Hullcarver") shouldBe true
                }

                // Activate {6}{W}{W}: empty linked exile to graveyards, create Robots, sacrifice Starcage.
                val starcageId = game.findPermanent("Pinnacle Starcage")!!
                val activatedAbility = cardRegistry.getCard("Pinnacle Starcage")!!
                    .script.activatedAbilities.first()
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = starcageId,
                        abilityId = activatedAbility.id
                    )
                )
                withClue("Activating Starcage's {6}{W}{W} ability should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Hullcarver should be in its OWNER's graveyard (P2), not the activator's") {
                    game.isInGraveyard(2, "Hullcarver") shouldBe true
                    game.isInGraveyard(1, "Hullcarver") shouldBe false
                }
                withClue("Exactly one 2/2 Robot token should be on the activator's battlefield (one per card put into a graveyard)") {
                    game.findAllPermanents("Robot Token").size shouldBe 1
                }
                withClue("Pinnacle Starcage should be sacrificed (in its owner's graveyard, not on the battlefield)") {
                    game.isOnBattlefield("Pinnacle Starcage") shouldBe false
                    game.isInGraveyard(1, "Pinnacle Starcage") shouldBe true
                }
            }
        }
    }

    private fun isInExile(game: TestGame, cardName: String): Boolean {
        return (game.state.getExile(game.player1Id) + game.state.getExile(game.player2Id))
            .any { id -> game.state.getEntity(id)?.get<CardComponent>()?.name == cardName }
    }
}
