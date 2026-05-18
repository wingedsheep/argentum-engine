package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Noble Elephant and the banding mechanic (CR 702.21).
 *
 * Noble Elephant: {3}{W} 2/2 Creature — Elephant. Trample; banding.
 *
 * These tests exercise the banding rules independently of the card itself:
 * - Attack-time band declaration with at most one non-banding member.
 * - CR 702.21g: an attacking band is blocked as a group — blocking any member
 *   blocks every member.
 * - Illegal bands (two non-banding members, mixed defenders, singleton band) rejected.
 */
class NobleElephantScenarioTest : ScenarioTestBase() {

    init {
        context("Noble Elephant — banding") {

            test("declaring a band of two banding creatures stamps the same bandId") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Noble Elephant")
                    .withCardOnBattlefield(1, "Noble Elephant")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val elephants = game.findAllPermanents("Noble Elephant")
                elephants.size shouldBe 2
                val e1 = elephants[0]
                val e2 = elephants[1]

                val result = game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(e1 to game.player2Id, e2 to game.player2Id),
                        bands = listOf(setOf(e1, e2)),
                    )
                )
                withClue("Attack should succeed: ${result.error}") { result.error shouldBe null }

                val a1 = game.state.getEntity(e1)?.get<AttackingComponent>()
                val a2 = game.state.getEntity(e2)?.get<AttackingComponent>()
                a1.shouldNotBeNull()
                a2.shouldNotBeNull()
                a1.bandId.shouldNotBeNull()
                a1.bandId shouldBe a2.bandId
            }

            test("CR 702.21g: blocking one band member blocks every member") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Noble Elephant")  // 2/2 trample, banding
                    .withCardOnBattlefield(1, "Grizzly Bears")   // 2/2 no banding
                    .withCardOnBattlefield(2, "Hill Giant")      // 3/3
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val elephant = game.findPermanent("Noble Elephant")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val hg = game.findPermanent("Hill Giant")!!

                val attackResult = game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(elephant to game.player2Id, bears to game.player2Id),
                        bands = listOf(setOf(elephant, bears)),
                    )
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block ONLY Grizzly Bears with Hill Giant. By banding rules, Noble Elephant
                // should also become blocked because the entire band is blocked.
                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(hg to listOf(bears)))
                )
                withClue("Block should succeed: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }

                val elephantBlocked = game.state.getEntity(elephant)?.get<BlockedComponent>()
                val bearsBlocked = game.state.getEntity(bears)?.get<BlockedComponent>()

                withClue("Noble Elephant should be blocked via the band") {
                    elephantBlocked.shouldNotBeNull()
                    elephantBlocked.blockerIds shouldBe listOf(hg)
                }
                withClue("Grizzly Bears should be blocked directly") {
                    bearsBlocked.shouldNotBeNull()
                    bearsBlocked.blockerIds shouldBe listOf(hg)
                }
            }

            test("a band may contain any number of banding creatures (CR 702.21c)") {
                // The "at most one" restriction in CR 702.21c applies only to creatures
                // *without* banding: "one or more attacking creatures with banding and
                // up to one attacking creature without banding are all in a band". A
                // band of three banding creatures is therefore legal.
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Noble Elephant")
                    .withCardOnBattlefield(1, "Noble Elephant")
                    .withCardOnBattlefield(1, "Noble Elephant")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val elephants = game.findAllPermanents("Noble Elephant")
                elephants.size shouldBe 3
                val (e1, e2, e3) = Triple(elephants[0], elephants[1], elephants[2])

                val result = game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(e1 to game.player2Id, e2 to game.player2Id, e3 to game.player2Id),
                        bands = listOf(setOf(e1, e2, e3)),
                    )
                )
                withClue("Three banding creatures should be allowed in one band: ${result.error}") {
                    result.error shouldBe null
                }

                val bandIds = elephants.mapNotNull {
                    game.state.getEntity(it)?.get<AttackingComponent>()?.bandId
                }
                withClue("All three should share the same bandId") {
                    bandIds.size shouldBe 3
                    bandIds.toSet().size shouldBe 1
                }
            }

            test("a band with two non-banding members is rejected") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val gb = game.findPermanent("Grizzly Bears")!!
                val hg = game.findPermanent("Hill Giant")!!

                val result = game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(gb to game.player2Id, hg to game.player2Id),
                        bands = listOf(setOf(gb, hg)),
                    )
                )
                withClue("Two non-banding creatures cannot form a band") {
                    result.error shouldNotBe null
                }
            }

            test("a band of size one is rejected") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Noble Elephant")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val elephant = game.findPermanent("Noble Elephant")!!

                val result = game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(elephant to game.player2Id),
                        bands = listOf(setOf(elephant)),
                    )
                )
                withClue("A band must have at least two members") {
                    result.error shouldNotBe null
                }
            }

            test("Noble Elephant carries the Banding keyword and is recognized as banding") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Noble Elephant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elephant = game.findPermanent("Noble Elephant")!!
                val projected = game.state.projectedState
                projected.hasKeyword(elephant, com.wingedsheep.sdk.core.Keyword.BANDING) shouldBe true
                projected.hasKeyword(elephant, com.wingedsheep.sdk.core.Keyword.TRAMPLE) shouldBe true
            }
        }
    }
}
