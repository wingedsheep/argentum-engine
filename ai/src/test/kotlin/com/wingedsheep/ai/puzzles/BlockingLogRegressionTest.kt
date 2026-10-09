package com.wingedsheep.ai.puzzles

import com.wingedsheep.ai.engine.AIPlayer
import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe

/**
 * Blocking misplays read out of the 2026-10-09 AI-vs-AI game logs, rebuilt as the smallest board
 * that still shows them. Each test names the game and turn it came from.
 *
 * Seat 1 attacks, seat 2 is the AI declaring blockers.
 */
class BlockingLogRegressionTest : ScenarioTestBase() {

    private companion object {
        /** `ScenarioBuilder` seeds itself from the clock otherwise; a regression test cannot. */
        const val SCENARIO_SEED = 20261009L
    }

    /** One permanent of the board: its name, which seat controls it, and whether it is tapped. */
    private data class Piece(val name: String, val seat: Int, val tapped: Boolean = false)

    /**
     * Build the board, have seat 1 attack with [attackers] (names, each resolved to a distinct
     * permanent so two copies of a card can both attack), and return what [profile] declares as
     * seat 2's blocks, keyed by name.
     */
    private fun blocksFor(
        profile: AiProfile,
        board: List<Piece>,
        attackers: List<String>,
        defenderLife: Int,
        attackerLife: Int = 20,
    ): Map<String, List<String>> {
        var builder = scenario().withRngSeed(SCENARIO_SEED).withPlayers()
        board.forEach { builder = builder.withCardOnBattlefield(it.seat, it.name, tapped = it.tapped) }
        val game = builder.withLifeTotal(2, defenderLife).withLifeTotal(1, attackerLife).build()
            .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)

        val remaining = attackers.groupingBy { it }.eachCount().toMutableMap()
        val attackerIds = remaining.keys.flatMap { name ->
            game.findPermanents(name)
                .filter { game.state.projectedState.getController(it) == game.seatId(1) }
                .take(remaining.getValue(name))
        }
        game.execute(DeclareAttackers(game.seatId(1), attackerIds.associateWith { game.seatId(2) }))
        game.advanceToDeclaration(2, Step.DECLARE_BLOCKERS)

        val action = AIPlayer.create(cardRegistry, game.seatId(2), profile).chooseAction(game.state)
        val blocks = (action as? DeclareBlockers)?.blockers.orEmpty()
        val name = { id: EntityId -> game.state.getEntity(id)!!.get<com.wingedsheep.engine.state.components.identity.CardComponent>()!!.name }
        return blocks.filterValues { it.isNotEmpty() }
            .entries.associate { (blocker, blocked) -> name(blocker) to blocked.map(name) }
    }

    init {
        // ── Chump blocks at high life (flag: chumpOnlyWhenInDanger) ──────────────────────────

        test("game 2 turn 20: a 0/3 wall is not thrown in front of a 3/3 at 18 life") {
            val board = listOf(
                Piece("Phyrexian Slayer", 1),
                Piece("Shivan Zombie", 1),
                Piece("Trench Wurm", 1),
                Piece("Wall of Wood", 2),
            )
            val blocks = blocksFor(
                AiProfile.PRODUCTION_CANDIDATE_CHUMPGATE, board,
                attackers = listOf("Phyrexian Slayer", "Trench Wurm"), defenderLife = 18, attackerLife = 21,
            )
            withClue("blocks were $blocks") { blocks shouldNotContainKey "Wall of Wood" }
        }

        test("game 21 turn 20: a 1/1 does not chump a 2/3 at 25 life") {
            // Most of the attack is in the air and cannot be blocked anyway; at 25 the two points a
            // chump saves on the ground are not worth the body.
            val board = listOf(
                Piece("Armored Pegasus", 1),
                Piece("Snapping Drake", 1),
                Piece("Regal Unicorn", 1),
                Piece("Owl Familiar", 1),
                Piece("Starlit Angel", 1),
                Piece("Charging Paladin", 1),
                Piece("Esquire of the King", 2),
                Piece("Esquire of the King", 2, tapped = true),
            )
            val blocks = blocksFor(
                AiProfile.PRODUCTION_CANDIDATE_CHUMPGATE, board,
                attackers = listOf(
                    "Armored Pegasus", "Snapping Drake", "Regal Unicorn",
                    "Owl Familiar", "Starlit Angel", "Charging Paladin",
                ),
                defenderLife = 25, attackerLife = 14,
            )
            withClue("blocks were $blocks") { blocks shouldBe emptyMap() }
        }

        test("game 9 turn 14: a 1/1 does not chump a 2/2 at 12 life") {
            val board = listOf(
                Piece("Grizzly Bears", 1),
                Piece("Hill Giant", 1),
                Piece("Llanowar Elves", 2, tapped = true),
                Piece("Llanowar Elves", 2, tapped = true),
                Piece("Eager Cadet", 2),
            )
            val blocks = blocksFor(
                AiProfile.PRODUCTION_CANDIDATE_CHUMPGATE, board,
                attackers = listOf("Grizzly Bears"), defenderLife = 12, attackerLife = 16,
            )
            withClue("blocks were $blocks") { blocks shouldNotContainKey "Eager Cadet" }
        }

        test("the gate stands aside when the hit leaves us dead to the next attack") {
            // 9 life, a 6/4 swinging with another 6/4 and a 3/3 at home: unblocked we sit at 3
            // facing 15. That is the life the chump is for.
            val board = listOf(
                Piece("Craw Wurm", 1),
                Piece("Craw Wurm", 1),
                Piece("Hill Giant", 1),
                Piece("Eager Cadet", 2),
            )
            for (profile in listOf(AiProfile.PRODUCTION_CHUMPGATE, AiProfile.PRODUCTION_CANDIDATE_CHUMPGATE)) {
                val blocks = blocksFor(profile, board, attackers = listOf("Craw Wurm"), defenderLife = 9)
                withClue("${profile.id}: blocks were $blocks") {
                    blocks shouldBe mapOf("Eager Cadet" to listOf("Craw Wurm"))
                }
            }
        }

        // ── Menace (bug fix, every profile) ─────────────────────────────────────────────────────

        test("game 12 turn 10: the 1/5 blocks a non-menace attacker instead of being stripped off the menace one") {
            val board = listOf(
                Piece("Plundering Pirate", 1),
                Piece("Dinotomaton", 1),
                Piece("Geological Appraiser", 1),
                Piece("Voldaren Bloodcaster", 2),
                Piece("Catapult Fodder", 2),
            )
            for (profile in listOf(AiProfile.PRODUCTION, AiProfile.PRODUCTION_CANDIDATE_EXPIRING)) {
                val blocks = blocksFor(
                    profile, board,
                    attackers = listOf("Plundering Pirate", "Dinotomaton", "Geological Appraiser"),
                    defenderLife = 12,
                )
                withClue("${profile.id}: blocks were $blocks") {
                    blocks shouldContainKey "Catapult Fodder"
                    // A lone blocker on the menace attacker would be an illegal declaration.
                    val onDinotomaton = blocks.filterValues { "Dinotomaton" in it }.keys
                    (onDinotomaton.size != 1) shouldBe true
                }
            }
        }

        // ── Protection (bug fix, every profile) ─────────────────────────────────────────────────

        test("game 24 turn 8: a pro-black attacker does not cost the black creature its free block") {
            val board = listOf(
                Piece("White Knight", 1),
                Piece("Dripping-Tongue Zubera", 1),
                Piece("Hunted Horror", 2),
            )
            for (profile in listOf(AiProfile.PRODUCTION, AiProfile.PRODUCTION_CANDIDATE_EXPIRING)) {
                val blocks = blocksFor(
                    profile, board,
                    attackers = listOf("White Knight", "Dripping-Tongue Zubera"),
                    defenderLife = 20,
                )
                withClue("${profile.id}: blocks were $blocks") {
                    blocks shouldContainKey "Hunted Horror"
                    blocks.getValue("Hunted Horror") shouldContain "Dripping-Tongue Zubera"
                }
            }
        }
    }
}
