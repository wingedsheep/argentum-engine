package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mkm.cards.AftermathAnalyst
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Aftermath Analyst (MKM #148) — {1}{G} 1/3 Creature — Elf Detective.
 *
 * "When this creature enters, mill three cards."
 * "{3}{G}, Sacrifice this creature: Return all land cards from your graveyard to the battlefield
 *  tapped."
 *
 * The two halves are tested separately, plus the thing a naive implementation gets wrong: the
 * return is filtered to *land cards* and enters *tapped*, and the Analyst itself — already in the
 * graveyard by the time the ability resolves, because sacrifice is a cost — must not come back.
 */
class AftermathAnalystScenarioTest : ScenarioTestBase() {

    private val recursionAbility = AftermathAnalyst.activatedAbilities.first().id

    init {
        context("Aftermath Analyst") {

            test("entering mills three cards") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Aftermath Analyst")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .build()

                game.castSpell(1, "Aftermath Analyst").error shouldBe null
                game.resolveStack()

                withClue("three cards off the top, one left in the library") {
                    game.graveyardSize(1) shouldBe 3
                    game.librarySize(1) shouldBe 1
                }
            }

            test("the sacrifice ability returns every land card from the graveyard, tapped") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Aftermath Analyst")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInGraveyard(1, "Mountain")
                    .withCardInGraveyard(1, "Swamp")
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .build()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Aftermath Analyst")!!,
                        abilityId = recursionAbility,
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("both lands returned, and each one entered tapped") {
                    val returned = game.findAllPermanents("Mountain") + game.findAllPermanents("Swamp")
                    returned.size shouldBe 2
                    returned.all {
                        game.state.getEntity(it)?.get<TappedComponent>() != null
                    } shouldBe true
                }
                withClue("the filter is land cards — the Bolt stays put") {
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                }
                withClue("sacrifice is a cost, so the Elf is in the graveyard and is not a land") {
                    game.isInGraveyard(1, "Aftermath Analyst") shouldBe true
                    game.isOnBattlefield("Aftermath Analyst") shouldBe false
                }
            }

            test("the ability is happy to return nothing when no lands are in the graveyard") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Aftermath Analyst")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .build()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Aftermath Analyst")!!,
                        abilityId = recursionAbility,
                    )
                ).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                game.isInGraveyard(1, "Aftermath Analyst") shouldBe true
            }
        }
    }
}
