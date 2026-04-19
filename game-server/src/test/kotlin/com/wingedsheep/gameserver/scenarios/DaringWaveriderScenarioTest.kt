package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Daring Waverider.
 *
 * Card reference:
 * - Daring Waverider (4UU): Creature — Otter Wizard 4/4
 *   "When this creature enters, you may cast target instant or sorcery card with mana value
 *   4 or less from your graveyard without paying its mana cost. If that spell would be put
 *   into your graveyard, exile it instead."
 */
class DaringWaveriderScenarioTest : ScenarioTestBase() {

    init {
        context("Daring Waverider") {
            test("ETB exiles target sorcery from graveyard and grants free cast") {
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardInHand(1, "Daring Waverider")
                    .withCardInGraveyard(1, "Volcanic Hammer")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Daring Waverider
                game.castSpell(1, "Daring Waverider")
                game.resolveStack()

                // ETB trigger fires — need to select target from graveyard
                val hammerIds = game.findCardsInGraveyard(1, "Volcanic Hammer")
                game.selectTargets(hammerIds)
                game.resolveStack()

                // The trigger resolved: Volcanic Hammer should now be in exile
                val exileZone = game.state.getExile(game.player1Id)
                val hammerInExile = exileZone.any { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Volcanic Hammer"
                }
                hammerInExile shouldBe true

                // Volcanic Hammer should no longer be in graveyard
                game.isInGraveyard(1, "Volcanic Hammer") shouldBe false
            }

            test("cast spell from exile for free, spell goes to exile after resolution") {
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardInHand(1, "Daring Waverider")
                    .withCardInGraveyard(1, "Volcanic Hammer")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Daring Waverider
                game.castSpell(1, "Daring Waverider")
                game.resolveStack()

                // ETB trigger - select Volcanic Hammer from graveyard
                val hammerIds = game.findCardsInGraveyard(1, "Volcanic Hammer")
                game.selectTargets(hammerIds)
                game.resolveStack()

                // Now cast Volcanic Hammer from exile for free, targeting opponent's creature
                val bearsId = game.findPermanent("Grizzly Bears")!!
                game.castSpellFromExile(1, "Volcanic Hammer", bearsId)
                game.resolveStack()

                // Grizzly Bears should be dead (3 damage to a 2/2)
                game.isOnBattlefield("Grizzly Bears") shouldBe false

                // Volcanic Hammer should be in exile (not graveyard) due to ExileAfterResolveComponent
                val exileZone = game.state.getExile(game.player1Id)
                val hammerInExile = exileZone.any { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Volcanic Hammer"
                }
                hammerInExile shouldBe true
                game.isInGraveyard(1, "Volcanic Hammer") shouldBe false
            }

            test("free-cast permission is consumed when the granted spell is cast (cannot be re-cast if it returns to exile)") {
                // Regression: previously, the MayPlay/PlayWithoutPayingCost components on the
                // exiled card were only stripped when the spell resolved cleanly. If the spell
                // was countered (or otherwise didn't resolve), it would land back in exile via
                // ExileAfterResolveComponent with the free-cast permission still attached, and
                // the controller could re-cast the same card again — and again — for free.
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardInHand(1, "Daring Waverider")
                    .withCardInGraveyard(1, "Volcanic Hammer")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Daring Waverider")
                game.resolveStack()

                val hammerIds = game.findCardsInGraveyard(1, "Volcanic Hammer")
                game.selectTargets(hammerIds)
                game.resolveStack()

                val hammerId = game.state.getExile(game.player1Id).single { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Volcanic Hammer"
                }
                withClue("Permission components should be granted while the card sits in exile") {
                    game.state.getEntity(hammerId)?.get<MayPlayFromExileComponent>() shouldBe
                        MayPlayFromExileComponent(controllerId = game.player1Id)
                    game.state.getEntity(hammerId)?.get<PlayWithoutPayingCostComponent>() shouldBe
                        PlayWithoutPayingCostComponent(controllerId = game.player1Id)
                }

                val bearsId = game.findPermanent("Grizzly Bears")!!
                game.castSpellFromExile(1, "Volcanic Hammer", bearsId)

                // Volcanic Hammer is now on the stack — the free-cast permission has been used.
                // The components must be stripped immediately so that if the spell is countered
                // and ExileAfterResolveComponent sends it back to exile, it cannot be cast again.
                withClue("Free-cast permission must be consumed at cast time, not at resolve time") {
                    game.state.getEntity(hammerId)?.get<MayPlayFromExileComponent>() shouldBe null
                    game.state.getEntity(hammerId)?.get<PlayWithoutPayingCostComponent>() shouldBe null
                }

                game.resolveStack()

                // After resolution the spell exiles itself (ExileAfterResolveComponent) and the
                // permission components are still gone — the controller cannot re-cast it.
                val hammerInExile = game.state.getExile(game.player1Id).any { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Volcanic Hammer"
                }
                hammerInExile shouldBe true
                game.state.getEntity(hammerId)?.get<MayPlayFromExileComponent>() shouldBe null
                game.state.getEntity(hammerId)?.get<PlayWithoutPayingCostComponent>() shouldBe null
            }
        }
    }
}
