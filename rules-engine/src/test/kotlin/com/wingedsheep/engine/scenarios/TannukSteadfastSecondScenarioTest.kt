package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.WarpedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.WarpExiledComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Tannuk, Steadfast Second (EOE #162) — {2}{R}{R} Legendary Kavu Pilot 3/5.
 *
 * "Other creatures you control have haste."
 * "Artifact cards and red creature cards in your hand have warp {2}{R}."
 *
 * Covers the haste anthem (granted to other creatures, not Tannuk itself) and the new
 * `GrantWarpToCardsInHand` SDK static — that matching cards in the controller's hand pick up
 * warp legally castable at the granted cost, non-matching cards do not, the granted-warp cast
 * resolves with the same warp end-step exile trigger as a printed warp, and removing Tannuk
 * from the battlefield cancels the grant.
 */
class TannukSteadfastSecondScenarioTest : ScenarioTestBase() {

    init {
        context("Tannuk haste anthem") {

            test("other creatures you control have haste; Tannuk itself does not") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tannuk, Steadfast Second", summoningSickness = false)
                    .withCardOnBattlefield(1, "Goblin Guide", summoningSickness = true)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tannuk = game.findPermanent("Tannuk, Steadfast Second")!!
                val goblin = game.findPermanent("Goblin Guide")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState

                withClue("Tannuk's anthem grants haste to other creatures the controller owns") {
                    projected.hasKeyword(goblin, Keyword.HASTE) shouldBe true
                    projected.hasKeyword(bears, Keyword.HASTE) shouldBe true
                }
                withClue("'Other creatures' excludes Tannuk itself") {
                    projected.hasKeyword(tannuk, Keyword.HASTE) shouldBe false
                }
            }

            test("opponent creatures do not gain haste from Tannuk") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tannuk, Steadfast Second", summoningSickness = false)
                    .withCardOnBattlefield(2, "Goblin Guide", summoningSickness = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentGoblin = game.state.getBattlefield(game.player2Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Goblin Guide"
                }
                game.state.projectedState.hasKeyword(opponentGoblin, Keyword.HASTE) shouldBe false
            }
        }

        context("Tannuk warp grant") {

            test("a red creature card in hand has warp {2}{R} as a legal cast") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tannuk, Steadfast Second", summoningSickness = false)
                    .withCardInHand(1, "Goblin Guide")
                    .withLandsOnBattlefield(1, "Mountain", 3) // enough for {2}{R} warp
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(2, "Mountain") }
                val game = builder.build()

                val warpAction = game.getLegalActions(1).firstOrNull { info ->
                    info.actionType == "CastWithWarp" &&
                        (info.action as? CastSpell)?.let { cast ->
                            game.state.getEntity(cast.cardId)?.get<CardComponent>()?.name == "Goblin Guide"
                        } == true
                }
                withClue("Goblin Guide picks up warp from Tannuk and surfaces a CastWithWarp action") {
                    warpAction shouldNotBe null
                }
            }

            test("an artifact card in hand has warp {2}{R} as a legal cast") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tannuk, Steadfast Second", summoningSickness = false)
                    .withCardInHand(1, "Frogmite")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(2, "Mountain") }
                val game = builder.build()

                val warpAction = game.getLegalActions(1).firstOrNull { info ->
                    info.actionType == "CastWithWarp" &&
                        (info.action as? CastSpell)?.let { cast ->
                            game.state.getEntity(cast.cardId)?.get<CardComponent>()?.name == "Frogmite"
                        } == true
                }
                withClue("Frogmite (artifact card) picks up warp from Tannuk") {
                    warpAction shouldNotBe null
                }
            }

            test("a non-matching card (green creature, no artifact type) does NOT gain warp") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tannuk, Steadfast Second", summoningSickness = false)
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(2, "Mountain") }
                val game = builder.build()

                val warpAction = game.getLegalActions(1).firstOrNull { info ->
                    info.actionType == "CastWithWarp" &&
                        (info.action as? CastSpell)?.let { cast ->
                            game.state.getEntity(cast.cardId)?.get<CardComponent>()?.name == "Grizzly Bears"
                        } == true
                }
                withClue("Grizzly Bears is neither artifact nor red — Tannuk's grant must not apply") {
                    warpAction shouldBe null
                }
            }

            test("casting via the granted warp cost resolves into a warped permanent that is exiled at end of turn") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tannuk, Steadfast Second", summoningSickness = false)
                    .withCardInHand(1, "Goblin Guide")
                    .withLandsOnBattlefield(1, "Mountain", 3) // warp cost is {2}{R}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Mountain") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Mountain") }
                val game = builder.build()

                val castResult = game.castSpellWithAlternativeCost(1, "Goblin Guide")
                castResult.error shouldBe null
                game.resolveStack()

                val goblin = game.findPermanent("Goblin Guide")
                withClue("Granted-warp cast resolves into a real permanent") {
                    goblin shouldNotBe null
                }
                withClue("Warp-cast permanents carry WarpedComponent for the end-step exile trigger") {
                    game.state.getEntity(goblin!!)?.has<WarpedComponent>() shouldBe true
                }
                withClue("spellWarpedThisTurn flips true so void payoffs see the warp") {
                    game.state.spellWarpedThisTurn shouldBe true
                }

                // Advance to end step; the delayed warp trigger fires and exiles the permanent.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Warped permanent is exiled at the next end step (CR 702.185b)") {
                    game.findPermanent("Goblin Guide") shouldBe null
                }
                val exiled = game.state.getExile(game.player1Id).firstOrNull { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Goblin Guide"
                }
                withClue("Exiled warped card carries WarpExiledComponent for the recast-from-exile permission") {
                    exiled shouldNotBe null
                    game.state.getEntity(exiled!!)?.has<WarpExiledComponent>() shouldBe true
                }
                withClue(
                    "A granted-warp card with no printed warp (Goblin Guide) still gets a " +
                        "MayPlayPermission for the recast-from-exile path — CR 702.185a says the " +
                        "recast pays the regular mana cost, so the printed warp's absence on the " +
                        "card definition does not block re-casting."
                ) {
                    game.state.mayPlayPermissions.any { permission ->
                        permission.controllerId == game.player1Id && exiled in permission.cardIds
                    } shouldBe true
                }
            }

            test("when Tannuk leaves the battlefield, the warp grant on cards in hand ends") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tannuk, Steadfast Second", summoningSickness = false)
                    .withCardInHand(1, "Goblin Guide")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(2, "Mountain") }
                val game = builder.build()

                // Sanity: Goblin Guide gets the granted warp action while Tannuk is on the field.
                val before = game.getLegalActions(1).firstOrNull { info ->
                    info.actionType == "CastWithWarp" &&
                        (info.action as? CastSpell)?.let { cast ->
                            game.state.getEntity(cast.cardId)?.get<CardComponent>()?.name == "Goblin Guide"
                        } == true
                }
                before shouldNotBe null

                // Send Tannuk to the graveyard out-of-band — the grant must drop with it.
                val tannukId = game.findPermanent("Tannuk, Steadfast Second")!!
                val transition = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
                    state = game.state,
                    entityId = tannukId,
                    destinationZone = com.wingedsheep.sdk.core.Zone.GRAVEYARD
                )
                game.state = transition.state

                val after = game.getLegalActions(1).firstOrNull { info ->
                    info.actionType == "CastWithWarp" &&
                        (info.action as? CastSpell)?.let { cast ->
                            game.state.getEntity(cast.cardId)?.get<CardComponent>()?.name == "Goblin Guide"
                        } == true
                }
                withClue("Granted warp must be off as soon as Tannuk leaves the battlefield") {
                    after shouldBe null
                }
            }
        }
    }
}
