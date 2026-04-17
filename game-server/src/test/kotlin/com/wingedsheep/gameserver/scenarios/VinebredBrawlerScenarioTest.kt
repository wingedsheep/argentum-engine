package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * Scenario tests for Vinebred Brawler's "must be blocked if able" ability.
 *
 * Card reference:
 * - Vinebred Brawler ({2}{G}): 4/2 Creature - Elf Berserker
 *   "This creature must be blocked if able."
 *   "Whenever this creature attacks, another target Elf you control gets +2/+1 until end of turn."
 *
 * Relevant rulings (via Scryfall for Gaea's Protector, same effect):
 * - Only one creature is required to block. Other creatures may also block it,
 *   or block other attackers, or not block at all.
 * - The defending player chooses which creature blocks.
 * - If no creature the defending player controls can block (all tapped, or
 *   no creatures at all), the attacker isn't required to be blocked.
 */
class VinebredBrawlerScenarioTest : ScenarioTestBase() {

    init {
        context("Vinebred Brawler - must be blocked if able") {

            test("declaring no blockers fails when the defender has an untapped creature") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Vinebred Brawler")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val brawlerId = game.findPermanent("Vinebred Brawler")!!
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(brawlerId to game.player2Id))
                )
                withClue("Attackers declared: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val noBlockResult = game.declareNoBlockers()
                withClue("Declaring no blockers must fail when Grizzly Bears can block") {
                    noBlockResult.error shouldNotBe null
                    noBlockResult.error!! shouldContain "must be blocked"
                }
            }

            test("one blocker is enough — other creatures are free to stay back") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Vinebred Brawler")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val brawlerId = game.findPermanent("Vinebred Brawler")!!
                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(brawlerId to game.player2Id))
                )
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.declareBlockers(mapOf(
                    "Grizzly Bears" to listOf("Vinebred Brawler")
                ))
                withClue("One blocker should satisfy must-be-blocked-if-able: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }

            test("defender chooses which creature blocks — any single creature satisfies") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Vinebred Brawler")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val brawlerId = game.findPermanent("Vinebred Brawler")!!
                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(brawlerId to game.player2Id))
                )
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.declareBlockers(mapOf(
                    "Hill Giant" to listOf("Vinebred Brawler")
                ))
                withClue("Defender's choice of blocker should be accepted: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }

            test("no blockers is legal when all defender's creatures are tapped") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Vinebred Brawler")
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val brawlerId = game.findPermanent("Vinebred Brawler")!!
                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(brawlerId to game.player2Id))
                )
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val noBlockResult = game.declareNoBlockers()
                withClue("No blockers should be legal when no creature can block: ${noBlockResult.error}") {
                    noBlockResult.error shouldBe null
                }
            }

            test("no blockers is legal when defender has no creatures at all") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Vinebred Brawler")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val brawlerId = game.findPermanent("Vinebred Brawler")!!
                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(brawlerId to game.player2Id))
                )
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val noBlockResult = game.declareNoBlockers()
                withClue("No blockers should be legal when defender has no creatures: ${noBlockResult.error}") {
                    noBlockResult.error shouldBe null
                }
            }

            test("Vinebred Brawler must be blocked even with another attacker — others pass through") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Vinebred Brawler")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val brawlerId = game.findPermanent("Vinebred Brawler")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(
                            brawlerId to game.player2Id,
                            bearsId to game.player2Id
                        )
                    )
                )
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Blocking Grizzly Bears while leaving Vinebred Brawler unblocked must fail
                val wrongBlockResult = game.declareBlockers(mapOf(
                    "Hill Giant" to listOf("Grizzly Bears")
                ))
                withClue("Letting Vinebred Brawler through should fail") {
                    wrongBlockResult.error shouldNotBe null
                    wrongBlockResult.error!! shouldContain "must be blocked"
                }

                // Blocking Vinebred Brawler and letting Grizzly Bears through is legal
                val correctBlockResult = game.declareBlockers(mapOf(
                    "Hill Giant" to listOf("Vinebred Brawler")
                ))
                withClue("Blocking Vinebred Brawler should succeed: ${correctBlockResult.error}") {
                    correctBlockResult.error shouldBe null
                }
            }
        }
    }
}
