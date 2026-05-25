package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * End-to-end scenario tests for banding (CR 702.22) on **defense**, using the real
 * registered Noble Elephant (2/2 trample + banding, Mirage).
 *
 * The defining quirk of banding — and the reason it's both infamous and powerful — is the
 * chooser inversion when blocking (CR 702.22j): "if an attacker is blocked by a creature with
 * banding, the defending player divides that attacker's combat damage among its blockers."
 * Combined with the fact that over-assignment is legal once banding lifts the damage-assignment
 * order, a single banding blocker lets the defender route all of an attacker's damage onto a
 * disposable chump — including all of a trampler's damage, so nothing tramples through.
 *
 * The engine's CombatResolutionBoardTest already covers the board *shape* and the *offensive*
 * band-cooperation matrix (CR 702.22k over-assignment onto one band member). These tests cover
 * the full-stack *defensive* payoffs, which are the ones that make banding strong on defense.
 * The non-banding multi-blocked-trample baseline (attacker chooses, 3 tramples through) lives in
 * BlockerOrderScenarioTest, so the contrast is intentionally not re-asserted here.
 */
class BandingCombatScenarioTest : ScenarioTestBase() {

    init {
        context("Banding on defense (CR 702.22)") {

            test("a banding blocker negates trample by routing all the attacker's damage onto a chump") {
                // Blistering Firecat (7/1 trample) attacks. Defender gang-blocks with Noble Elephant
                // (2/2 banding) and Grizzly Bears (2/2). Because a banding creature is blocking, the
                // DEFENDING player divides the Firecat's 7 damage (CR 702.22j) with the assignment
                // order lifted — so they dump all 7 onto the Bears and let nothing trample through.
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Blistering Firecat") // 7/1 trample haste
                    .withCardOnBattlefield(2, "Noble Elephant")     // 2/2 trample + banding
                    .withCardOnBattlefield(2, "Grizzly Bears")      // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val firecatId = game.findPermanent("Blistering Firecat")!!
                val elephantId = game.findPermanent("Noble Elephant")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!
                val startingLife = game.getLifeTotal(2)

                game.execute(DeclareAttackers(game.player1Id, mapOf(firecatId to game.player2Id)))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.execute(
                    DeclareBlockers(game.player2Id, mapOf(
                        elephantId to listOf(firecatId),
                        bearsId to listOf(firecatId),
                    ))
                )

                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                val decision = game.getPendingDecision()
                withClue("A banding blocker should surface the combat resolution board") {
                    decision shouldNotBe null
                    decision.shouldBeInstanceOf<CombatResolutionDecision>()
                }
                decision as CombatResolutionDecision
                withClue("CR 702.22j flips the chooser of the attacker's damage to the DEFENDING player") {
                    decision.playerId shouldBe game.player2Id
                }
                val firecatBlockerEdges = decision.edges.filter {
                    it.sourceId == firecatId && it.direction == DamageEdgeDirection.ATTACKER_TO_BLOCKER
                }
                withClue("Banding lifts the lethal-first assignment order on the attacker's edges") {
                    firecatBlockerEdges.map { it.targetId }.toSet() shouldBe setOf(elephantId, bearsId)
                    firecatBlockerEdges.all { !it.orderConstrained } shouldBe true
                }

                // Defender's choice: all 7 onto the Bears, none on the Elephant, none trampling through.
                game.submitCombatDamage(
                    mapOf(
                        (firecatId to bearsId) to 7,
                        (firecatId to elephantId) to 0,
                        (firecatId to game.player2Id) to 0,
                    )
                )

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Banding routed the trampler's damage onto the chump, so it dies") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("The banding blocker took no damage and survives") {
                    game.findPermanent("Noble Elephant") shouldNotBe null
                }
                withClue("Banding negated trample entirely — the defender takes 0 damage") {
                    game.getLifeTotal(2) shouldBe startingLife
                }
                withClue("The blockers still dealt 4 combat damage to the 7/1 Firecat, killing it") {
                    game.findPermanent("Blistering Firecat") shouldBe null
                }
            }

            test("a banding gang-block trades a chump up for a bigger non-trample attacker") {
                // Hill Giant (3/3, no trample) attacks. Defender gang-blocks with Noble Elephant
                // (2/2 banding) and Grizzly Bears (2/2). The band deals 4 to the Giant (lethal), while
                // banding lets the defender (CR 702.22j) shove all of the Giant's 3 damage onto the
                // Bears. Net: the defender trades a 2/2 for a 3/3 and keeps the Elephant unscathed.
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")     // 3/3, no trample
                    .withCardOnBattlefield(2, "Noble Elephant") // 2/2 trample + banding
                    .withCardOnBattlefield(2, "Grizzly Bears")  // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val giantId = game.findPermanent("Hill Giant")!!
                val elephantId = game.findPermanent("Noble Elephant")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!
                val startingLife = game.getLifeTotal(2)

                game.execute(DeclareAttackers(game.player1Id, mapOf(giantId to game.player2Id)))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.execute(
                    DeclareBlockers(game.player2Id, mapOf(
                        elephantId to listOf(giantId),
                        bearsId to listOf(giantId),
                    ))
                )

                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                val decision = game.getPendingDecision()
                withClue("Even with no trample, a banding blocker on 2+ blockers surfaces the board") {
                    decision shouldNotBe null
                    decision.shouldBeInstanceOf<CombatResolutionDecision>()
                }
                decision as CombatResolutionDecision
                withClue("CR 702.22j hands the Giant's damage division to the defending player") {
                    decision.playerId shouldBe game.player2Id
                }

                // Defender shoves all 3 of the Giant's damage onto the disposable Bears.
                game.submitCombatDamage(
                    mapOf(
                        (giantId to bearsId) to 3,
                        (giantId to elephantId) to 0,
                    )
                )

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("The band's 4 combined power is lethal to the 3/3 Giant") {
                    game.findPermanent("Hill Giant") shouldBe null
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                }
                withClue("The defender's chump soaked all of the Giant's damage and died") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
                withClue("The banding blocker took no damage and survives the trade") {
                    game.findPermanent("Noble Elephant") shouldNotBe null
                }
                withClue("No trample, so the defender's life is untouched") {
                    game.getLifeTotal(2) shouldBe startingLife
                }
            }
        }

        context("Blocking a band (CR 702.22h)") {

            test("blocking one member of an attacking band blocks the whole band") {
                // Player 1 attacks with a band: Noble Elephant (2/2 banding) + Grizzly Bears (2/2
                // vanilla — CR 702.22c allows one non-banding member). Player 2 blocks ONLY the
                // Elephant with a single Hill Giant. Per CR 702.22h ("bands are blocked as a group"),
                // blocking any band member blocks the entire band, so the engine treats the Giant as
                // blocking the un-declared Grizzly Bears too — neither connects with the player.
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Noble Elephant") // 2/2 trample + banding
                    .withCardOnBattlefield(1, "Grizzly Bears")  // 2/2 vanilla band partner
                    .withCardOnBattlefield(2, "Hill Giant")     // 3/3 lone blocker
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val elephantId = game.findPermanent("Noble Elephant")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!
                val giantId = game.findPermanent("Hill Giant")!!
                val startingLife = game.getLifeTotal(2)

                // Declare both attackers as a single band (CR 702.22c).
                game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(elephantId to game.player2Id, bearsId to game.player2Id),
                        bands = listOf(setOf(elephantId, bearsId)),
                    )
                )
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block ONLY the Elephant — the engine should expand this to block the whole band.
                game.execute(DeclareBlockers(game.player2Id, mapOf(giantId to listOf(elephantId))))

                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                // A banding attacker whose blocker faces two band members surfaces the damage board.
                // Assign both band members' full power to the Giant and explicitly trample nothing,
                // so the "defender takes 0" assertion isolates the block-as-a-group rule rather than
                // any trample leak (Noble Elephant has trample).
                if (game.hasPendingDecision()) {
                    game.submitCombatDamage(
                        mapOf(
                            (elephantId to giantId) to 2,
                            (bearsId to giantId) to 2,
                            (elephantId to game.player2Id) to 0,
                        )
                    )
                }

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("The un-declared band member (Grizzly Bears) was blocked too, so the defender takes 0") {
                    game.getLifeTotal(2) shouldBe startingLife
                }
                withClue("The lone blocker faced the whole band's 4 combined power and died") {
                    game.findPermanent("Hill Giant") shouldBe null
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
            }
        }
    }
}
