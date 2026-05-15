package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Interdimensional Web Watch.
 *
 * Card reference (SPM):
 * "When Interdimensional Web Watch enters, exile the top two cards of your library.
 *  You may play those cards until the end of your next turn."
 * (Activated tap ability for mana spendable only on spells from exile handled separately.)
 */
class InterdimensionalWebWatchScenarioTest : ScenarioTestBase() {

    init {
        context("Interdimensional Web Watch — tap for restricted mana") {

            test("tap ability adds two mana of any color restricted to casting spells from exile") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "Interdimensional Web Watch")
                    .withCardInExile(1, "Grizzly Bears")
                    .withCardInExile(1, "Grizzly Bears")
                    .withCardInHand(1, "Glory Seeker")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val watchId = game.findPermanent("Interdimensional Web Watch")!!
                val cardDef = cardRegistry.getCard("Interdimensional Web Watch")!!
                val manaAbility = cardDef.script.activatedAbilities.first()

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = watchId,
                        abilityId = manaAbility.id
                    )
                )
                withClue("Tap ability should pause for the first pip's color choice: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                withClue("Interdimensional Web Watch should be tapped after activation") {
                    game.state.getEntity(watchId)?.has<TappedComponent>() shouldBe true
                }

                // Two pips: pick the same color twice — sanity-checks the loop without changing colors.
                repeat(2) {
                    val decision = game.getPendingDecision()
                    decision.shouldBeInstanceOf<ChooseColorDecision>()
                    game.submitDecision(ColorChosenResponse(decision.id, Color.GREEN))
                }

                val manaPool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Mana pool should contain two restricted mana entries") {
                    manaPool?.restrictedMana?.size shouldBe 2
                }

                val restriction = manaPool?.restrictedMana?.firstOrNull()?.restriction
                withClue("Restricted mana should be limited to casting spells from exile") {
                    restriction shouldBe ManaRestriction.CastFromExileOnly
                }
            }
        }

        context("Interdimensional Web Watch — play permission expiry") {

            test("play permission granted by ETB expires at end of controller's next turn") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Interdimensional Web Watch")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Interdimensional Web Watch")
                withClue("Casting should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                val exile = game.state.getExile(game.player1Id)
                withClue("ETB should exile two cards") { exile.size shouldBe 2 }

                val exiledId = exile.first()
                fun hasMayPlayFor(id: EntityId): Boolean =
                    game.state.mayPlayPermissions.any { id in it.cardIds }

                withClue("Permission should be active immediately after ETB") {
                    hasMayPlayFor(exiledId) shouldBe true
                }

                // Step through two full turns using alternating waypoints so no call is a no-op.
                // [1] no-op (already at PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                // [2] P1's PRECOMBAT_MAIN → P2's UPKEEP (through P1's end + cleanup round 1)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                // [3] P2's UPKEEP → P2's PRECOMBAT_MAIN (through P2's draw)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                // [4] P2's PRECOMBAT_MAIN → P1's UPKEEP (through P2's end + cleanup round 1, P1's untap round 2)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                // [5] P1's UPKEEP → P1's PRECOMBAT_MAIN (turn 2, through P1's draw)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                withClue("Permission should still be active during controller's next turn") {
                    hasMayPlayFor(exiledId) shouldBe true
                }

                // [6] P1's PRECOMBAT_MAIN (turn 2) → P2's UPKEEP (through P1's end + cleanup round 2)
                //     P1's cleanup runs with turnNumber=2, expiresAfterTurn=2 → permission removed
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                withClue("Permission must be removed after the controller's next turn ends") {
                    hasMayPlayFor(exiledId) shouldBe false
                }
            }
        }

        context("Interdimensional Web Watch — ETB exile and play permission") {

            test("entering the battlefield exiles the top two library cards and grants play permission until end of controller's next turn") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Interdimensional Web Watch")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialLibrarySize = game.librarySize(1)

                val castResult = game.castSpell(1, "Interdimensional Web Watch")
                withClue("Casting Interdimensional Web Watch should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Interdimensional Web Watch should be on the battlefield") {
                    game.isOnBattlefield("Interdimensional Web Watch") shouldBe true
                }
                withClue("Library should have two fewer cards after the ETB exile") {
                    game.librarySize(1) shouldBe initialLibrarySize - 2
                }

                val exile = game.state.getExile(game.player1Id)
                withClue("Two cards should be in exile after ETB resolves") {
                    exile.size shouldBe 2
                }

                exile.forEach { cardId ->
                    val cardName = game.state.getEntity(cardId)?.get<CardComponent>()?.name
                    val permission = game.state.mayPlayPermissions.firstOrNull { cardId in it.cardIds }
                    withClue("Exiled card '$cardName' should be tagged with play permission for the controller") {
                        permission shouldNotBe null
                    }
                    withClue("Play permission on '$cardName' should be granted to player 1 (the controller)") {
                        permission!!.controllerId shouldBe game.player1Id
                    }
                }
            }
        }

        context("Interdimensional Web Watch — restricted mana spending") {

            test("restricted mana pays for an exile-cast spell but is refused for the same spell in hand") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "Interdimensional Web Watch")
                    .withCardInExile(1, "Grizzly Bears")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val watchId = game.findPermanent("Interdimensional Web Watch")!!
                val exiledBears = game.state.getExile(game.player1Id).first()
                val handBears = game.state.getHand(game.player1Id).first()

                // Manually grant cast-from-exile permission on the exiled Bears so this test
                // exercises the mana restriction without depending on the ETB pipeline.
                game.state = game.state.copy(
                    mayPlayPermissions = game.state.mayPlayPermissions + MayPlayPermission(
                        id = EntityId.generate(),
                        cardIds = setOf(exiledBears),
                        controllerId = game.player1Id,
                        sourceId = watchId,
                        permanent = true,
                        timestamp = game.state.timestamp
                    )
                )

                val cardDef = cardRegistry.getCard("Interdimensional Web Watch")!!
                val manaAbility = cardDef.script.activatedAbilities.first()
                val activate = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = watchId,
                        abilityId = manaAbility.id
                    )
                )
                withClue("Tap ability should activate: ${activate.error}") {
                    activate.error shouldBe null
                }

                repeat(2) {
                    val decision = game.getPendingDecision()
                    decision.shouldBeInstanceOf<ChooseColorDecision>()
                    game.submitDecision(ColorChosenResponse(decision.id, Color.GREEN))
                }

                val manaPool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Two restricted mana entries should be in the pool") {
                    manaPool?.restrictedMana?.size shouldBe 2
                }

                // The hand copy must NOT be castable with the restricted mana.
                val handCast = game.execute(CastSpell(game.player1Id, handBears))
                withClue("Casting from hand with cast-from-exile-restricted mana must fail") {
                    handCast.error shouldNotBe null
                    handCast.error!!.shouldContain("mana")
                }

                // The exile copy must be castable with the restricted mana.
                val exileCast = game.execute(CastSpell(game.player1Id, exiledBears))
                withClue("Casting from exile with cast-from-exile-restricted mana must succeed: ${exileCast.error}") {
                    exileCast.error shouldBe null
                }
                game.resolveStack()
                withClue("Grizzly Bears cast from exile should land on the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("two mana picked as separate colors can pay a {U}{R} spell cast from exile") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "Interdimensional Web Watch")
                    .withCardInExile(1, "Stormcatch Mentor")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val watchId = game.findPermanent("Interdimensional Web Watch")!!
                val exiledMentor = game.state.getExile(game.player1Id).first()

                game.state = game.state.copy(
                    mayPlayPermissions = game.state.mayPlayPermissions + MayPlayPermission(
                        id = EntityId.generate(),
                        cardIds = setOf(exiledMentor),
                        controllerId = game.player1Id,
                        sourceId = watchId,
                        permanent = true,
                        timestamp = game.state.timestamp
                    )
                )

                val cardDef = cardRegistry.getCard("Interdimensional Web Watch")!!
                val manaAbility = cardDef.script.activatedAbilities.first()
                val activate = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = watchId,
                        abilityId = manaAbility.id
                    )
                )
                withClue("Tap ability should activate: ${activate.error}") {
                    activate.error shouldBe null
                }

                // Pip 1 → BLUE, pip 2 → RED.
                val firstDecision = game.getPendingDecision()
                firstDecision.shouldBeInstanceOf<ChooseColorDecision>()
                game.submitDecision(ColorChosenResponse(firstDecision.id, Color.BLUE))

                val secondDecision = game.getPendingDecision()
                secondDecision.shouldBeInstanceOf<ChooseColorDecision>()
                game.submitDecision(ColorChosenResponse(secondDecision.id, Color.RED))

                val manaPool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Two restricted mana entries should be in the pool — one blue, one red") {
                    manaPool?.restrictedMana?.size shouldBe 2
                }
                withClue("Pool should hold one blue and one red restricted mana") {
                    manaPool?.restrictedMana?.count { it.color == Color.BLUE } shouldBe 1
                    manaPool?.restrictedMana?.count { it.color == Color.RED } shouldBe 1
                }

                val cast = game.execute(CastSpell(game.player1Id, exiledMentor))
                withClue("Casting {U}{R} Stormcatch Mentor from exile must succeed with mixed restricted mana: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()
                withClue("Stormcatch Mentor should resolve onto the battlefield") {
                    game.isOnBattlefield("Stormcatch Mentor") shouldBe true
                }
            }
        }
    }
}
