package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Feature test for the **skulk** keyword (CR 702.118).
 *
 * - CR 702.118b — a creature with skulk can't be blocked by creatures with greater power. Equal or
 *   lesser power blocks normally.
 * - Both powers are the projected values when blockers are declared: a pump on the attacker before
 *   then widens who can block it.
 * - Restrictions are checked only as blocks are declared (CR 509.1b–c), so a blocker pumped past the
 *   skulker's power afterwards stays blocking.
 * - Skulk granted until end of turn (`Effects.GrantKeyword(Keyword.SKULK, …)`) works like printed skulk.
 * - CR 702.118c — multiple instances are redundant, so a single grant on a printed skulker changes
 *   nothing.
 */
class SkulkScenarioTest : ScenarioTestBase() {

    private val skulker = card("Test Skulker") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Vampire Rogue"
        power = 2
        toughness = 2
        oracleText = "Skulk"
        keywords(Keyword.SKULK)
    }

    private val sneakyMaker = card("Grant Skulk") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Target creature gains skulk until end of turn."
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.GrantKeyword(Keyword.SKULK, t)
        }
    }

    private val pump = card("Test Brawn") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Target creature gets +2/+0 until end of turn."
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(2, 0, t)
        }
    }

    private fun attackWith(
        attacker: String,
        blocker: String,
        inHand: List<String> = emptyList(),
        defenderHand: List<String> = emptyList(),
        beforeAttack: (TestGame) -> Unit = {},
    ): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, attacker)
            .withCardOnBattlefield(2, blocker)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        inHand.forEach { builder.withCardInHand(1, it) }
        defenderHand.forEach { builder.withCardInHand(2, it) }
        val game = builder.build()
        beforeAttack(game)
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf(attacker to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        return game
    }

    init {
        listOf(skulker, sneakyMaker, pump).forEach { cardRegistry.register(it) }

        test("a creature with greater power can't block a skulker") {
            val game = attackWith("Test Skulker", "Hill Giant")
            withClue("3-power Hill Giant vs 2-power skulker (CR 702.118b)") {
                game.declareBlockers(mapOf("Hill Giant" to listOf("Test Skulker"))).error shouldNotBe null
            }
        }

        test("a creature with equal power can block a skulker") {
            val game = attackWith("Test Skulker", "Grizzly Bears")
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Test Skulker"))).error shouldBe null
        }

        test("a creature with lesser power can block a skulker") {
            val game = attackWith("Test Skulker", "Llanowar Elves")
            game.declareBlockers(mapOf("Llanowar Elves" to listOf("Test Skulker"))).error shouldBe null
        }

        test("the skulker's projected power counts: pumped before blocks, a bigger creature can block it") {
            val game = attackWith("Test Skulker", "Hill Giant", inHand = listOf("Test Brawn")) { g ->
                g.castSpell(1, "Test Brawn", g.findPermanent("Test Skulker")!!).error shouldBe null
                g.resolveStack()
            }
            withClue("4-power skulker can be blocked by 3-power Hill Giant") {
                game.declareBlockers(mapOf("Hill Giant" to listOf("Test Skulker"))).error shouldBe null
            }
        }

        test("a block declared legally stays when the blocker grows past the skulker afterwards") {
            val game = attackWith("Test Skulker", "Grizzly Bears", defenderHand = listOf("Test Brawn"))
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Test Skulker"))).error shouldBe null
            val bears = game.findPermanent("Grizzly Bears")!!
            // The defender pumps its own blocker once blocks are locked in.
            if (game.state.priorityPlayerId != game.player2Id) game.passPriority().error shouldBe null
            game.castSpell(2, "Test Brawn", bears).error shouldBe null
            game.resolveStack()
            withClue("Grizzly Bears is now 4 power, past the skulker's 2") {
                game.state.projectedState.getPower(bears) shouldBe 4
            }
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            withClue("the skulker stayed blocked, so no damage reached the defending player") {
                game.getLifeTotal(2) shouldBe 20
            }
        }

        test("skulk granted until end of turn restricts blocks like printed skulk") {
            val game = attackWith("Grizzly Bears", "Hill Giant", inHand = listOf("Grant Skulk")) { g ->
                val bears = g.findPermanent("Grizzly Bears")!!
                g.castSpell(1, "Grant Skulk", bears).error shouldBe null
                g.resolveStack()
                g.state.projectedState.hasKeyword(bears, Keyword.SKULK) shouldBe true
            }
            game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldNotBe null
        }

        test("a second instance of skulk is redundant") {
            val game = attackWith("Test Skulker", "Grizzly Bears", inHand = listOf("Grant Skulk")) { g ->
                g.castSpell(1, "Grant Skulk", g.findPermanent("Test Skulker")!!).error shouldBe null
                g.resolveStack()
            }
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Test Skulker"))).error shouldBe null
        }
    }
}
