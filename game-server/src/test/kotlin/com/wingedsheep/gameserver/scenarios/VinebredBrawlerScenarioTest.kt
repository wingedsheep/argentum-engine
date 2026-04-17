package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.PassPriority
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

        context("Vinebred Brawler + Taunting Elf — overlapping blocking requirements") {

            // When both attack together, MTG CR 509.1c says the defender must pick a block
            // assignment that maximizes the total number of requirements satisfied without
            // breaking restrictions.
            //   - Taunting Elf: N requirements (one "this blocker must block TE" per able blocker)
            //   - Vinebred Brawler: 1 requirement (at least one blocker must block it)
            //
            // With 3 able blockers, max = 3: either all-on-TE, or 2-on-TE + 1-on-VB. Both legal.
            //
            // KNOWN ENGINE LIMITATION: the current validator does NOT implement CR 509.1c's
            // maximize-requirements rule. It instead enforces two stricter local rules:
            //   (a) every potential blocker that can block a must-be-blocked-by-all attacker
            //       (Taunting Elf) MUST be assigned to one.
            //   (b) every must-be-blocked-if-able attacker (Vinebred Brawler) MUST have at least
            //       one blocker whenever any potential blocker could block it.
            // Together these two local rules are jointly unsatisfiable when Taunting Elf and
            // Vinebred Brawler both attack with ≥1 able blocker — every assignment fails one
            // check or the other. The tests below document the current behavior and mark the
            // MTG-correct outcome with TODO.

            test("TODO engine bug: 1 blocker + both attacking — no legal block exists (should allow either)") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Vinebred Brawler")
                    .withCardOnBattlefield(1, "Taunting Elf")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val brawlerId = game.findPermanent("Vinebred Brawler")!!
                val tauntingId = game.findPermanent("Taunting Elf")!!

                game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(brawlerId to game.player2Id, tauntingId to game.player2Id)
                    )
                )
                drainAttackTriggers(game, boostTarget = tauntingId)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Per MTG CR 509.1c, defender should be allowed to block *either* attacker
                // (both choices satisfy 1 requirement — a tie). Engine rejects both.
                val blockTaunting = game.declareBlockers(mapOf("Hill Giant" to listOf("Taunting Elf")))
                val blockBrawler = game.declareBlockers(mapOf("Hill Giant" to listOf("Vinebred Brawler")))
                withClue("Engine currently rejects 'block Taunting Elf' — MTG rule says it should be legal") {
                    blockTaunting.error shouldNotBe null
                    blockTaunting.error!! shouldContain "must be blocked"
                }
                withClue("Engine currently rejects 'block Brawler' — MTG rule says it should be legal") {
                    blockBrawler.error shouldNotBe null
                    blockBrawler.error!! shouldContain "must block"
                }
            }

            test("TODO engine bug: 3 blockers — all-on-Taunting-Elf should be legal per CR 509.1c tie") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Vinebred Brawler")
                    .withCardOnBattlefield(1, "Taunting Elf")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Devoted Hero")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val brawlerId = game.findPermanent("Vinebred Brawler")!!
                val tauntingId = game.findPermanent("Taunting Elf")!!

                game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(brawlerId to game.player2Id, tauntingId to game.player2Id)
                    )
                )
                drainAttackTriggers(game, boostTarget = tauntingId)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // All 3 on Taunting Elf satisfies 3 TE requirements + 0 VB = 3. Ties the
                // "2 TE + 1 VB" alternative, so per MTG this should be a legal choice.
                val allOnTaunting = game.declareBlockers(mapOf(
                    "Grizzly Bears" to listOf("Taunting Elf"),
                    "Hill Giant" to listOf("Taunting Elf"),
                    "Devoted Hero" to listOf("Taunting Elf")
                ))
                withClue("Engine currently rejects all-on-Taunting-Elf with 'VB must be blocked if able'") {
                    allOnTaunting.error shouldNotBe null
                    allOnTaunting.error!! shouldContain "Vinebred Brawler must be blocked"
                }
            }

            test("TODO engine bug: 3 blockers — 2 on Taunting Elf + 1 on Vinebred Brawler should be legal") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Vinebred Brawler")
                    .withCardOnBattlefield(1, "Taunting Elf")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Devoted Hero")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val brawlerId = game.findPermanent("Vinebred Brawler")!!
                val tauntingId = game.findPermanent("Taunting Elf")!!

                game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(brawlerId to game.player2Id, tauntingId to game.player2Id)
                    )
                )
                drainAttackTriggers(game, boostTarget = tauntingId)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // 2 TE + 1 VB = 3 satisfied. MTG-correct; engine rejects because one
                // potential blocker is not assigned to Taunting Elf.
                val split = game.declareBlockers(mapOf(
                    "Grizzly Bears" to listOf("Vinebred Brawler"),
                    "Hill Giant" to listOf("Taunting Elf"),
                    "Devoted Hero" to listOf("Taunting Elf")
                ))
                withClue("Engine currently rejects this with 'X must block Taunting Elf'") {
                    split.error shouldNotBe null
                    split.error!! shouldContain "must block Taunting Elf"
                }
            }

            test("three blockers — 2 on Vinebred Brawler + 1 on Taunting Elf is illegal") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Vinebred Brawler")
                    .withCardOnBattlefield(1, "Taunting Elf")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Devoted Hero")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val brawlerId = game.findPermanent("Vinebred Brawler")!!
                val tauntingId = game.findPermanent("Taunting Elf")!!

                game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(brawlerId to game.player2Id, tauntingId to game.player2Id)
                    )
                )

                drainAttackTriggers(game, boostTarget = tauntingId)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // 2 on VB + 1 on TE: 1 TE requirement + 1 VB requirement = 2 satisfied.
                // This violates CR 509.1c because 3 was achievable.
                val underBlockResult = game.declareBlockers(mapOf(
                    "Grizzly Bears" to listOf("Vinebred Brawler"),
                    "Hill Giant" to listOf("Vinebred Brawler"),
                    "Devoted Hero" to listOf("Taunting Elf")
                ))
                withClue("Blocking that satisfies only 2 requirements when 3 were achievable should fail") {
                    underBlockResult.error shouldNotBe null
                }
            }
        }
    }

    /**
     * After DeclareAttackers, Vinebred Brawler's "boost another Elf" trigger pauses
     * for target selection. Select the given target, then let remaining triggers
     * resolve by passing priority until the stack clears.
     */
    private fun drainAttackTriggers(
        game: TestGame,
        boostTarget: com.wingedsheep.sdk.model.EntityId
    ) {
        var safety = 0
        while (safety++ < 50) {
            val decision = game.state.pendingDecision
            if (decision is ChooseTargetsDecision) {
                game.selectTargets(listOf(boostTarget))
                continue
            }
            // No decision pending — if we're still in DECLARE_ATTACKERS, pass priority
            // to let attack triggers continue resolving; otherwise we're done.
            if (game.state.step != Step.DECLARE_ATTACKERS) return
            val priorityPlayer = game.state.priorityPlayerId ?: return
            game.execute(PassPriority(priorityPlayer))
        }
        error("drainAttackTriggers: safety limit reached")
    }
}
