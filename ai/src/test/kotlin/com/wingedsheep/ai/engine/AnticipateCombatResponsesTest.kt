package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.seatId
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * The opponent uses what its board can do in a fight ([AiProfile.anticipateCombatResponses]):
 * every simulated combat gives it the activated abilities its permanents can afford, and it uses
 * the ones that win the fight for it.
 *
 * Built from real cards with no card-specific AI code anywhere: Leonin Sun Standard ("{1}{W}:
 * Creatures you control get +1/+1 until end of turn") for the team pump, Prodigal Sorcerer
 * ("{T}: This creature deals 1 damage to any target") for the pinger, Skilled Battlecarver
 * ("During your turn, this creature has first strike. {1}{R}: This creature gets +1/+0 until end of
 * turn") for a self-pump on an attacker that's worth no less than the blocker in front of it.
 */
class AnticipateCombatResponsesTest : ScenarioTestBase() {

    private companion object {
        const val SCENARIO_SEED = 20261018L
        val WITHOUT = AiProfile.LIVE.copy(id = "live-without-responses", anticipateCombatResponses = false)
    }

    private data class Permanent(val name: String, val tapped: Boolean = false, val token: Boolean = false)

    private fun TestGame.names(ids: Collection<EntityId>): List<String> =
        ids.map { state.getEntity(it)?.get<CardComponent>()?.name ?: it.value }

    // ── Attacking: seat 1 is the AI, declaring attackers into seat 2's board ──

    private fun attackPosition(ours: List<Permanent>, theirs: List<Permanent>): TestGame {
        var builder = scenario().withRngSeed(SCENARIO_SEED).withPlayers()
        ours.forEach { builder = builder.withCardOnBattlefield(1, it.name, tapped = it.tapped) }
        theirs.forEach { builder = builder.withCardOnBattlefield(2, it.name, tapped = it.tapped, isToken = it.token) }
        return builder.build().advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
    }

    private fun TestGame.attackersChosenBy(profile: AiProfile): Set<EntityId> =
        (AIPlayer.create(cardRegistry, seatId(1), profile).chooseAction(state) as? DeclareAttackers)
            ?.attackers?.keys.orEmpty()

    private fun pumpBoard(plainsTapped: Boolean, plains: Int = 4) = listOf(
        Permanent("Grizzly Bears"),
        Permanent("Leonin Sun Standard"),
    ) + List(plains) { Permanent("Plains", tapped = plainsTapped) }

    /**
     * The defender's side of a logged game: Tenured Tethermage ("Tap two untapped artifacts you
     * control: Put two +1/+1 counters on this creature") beside [untappedArtifacts] untapped
     * artifacts — Heartwood tokens and a Medic's Kitesail — and two Heartwoods that entered tapped.
     */
    private fun tethermageBoard(untappedArtifacts: Boolean) = listOf(
        Permanent("Tenured Tethermage"),
        Permanent("Medic's Kitesail", tapped = !untappedArtifacts),
    ) + List(3) { Permanent("Heartwood", tapped = !untappedArtifacts, token = true) } +
        List(2) { Permanent("Heartwood", tapped = true, token = true) } +
        listOf(Permanent("Forest"), Permanent("Mountain", tapped = true), Permanent("Plains", tapped = true), Permanent("Forest", tapped = true))

    // ── Blocking: seat 1 attacks with what the test declares, seat 2 is the AI ──

    private fun blocksChosenBy(
        profile: AiProfile,
        attackers: List<String>,
        theirs: List<Permanent>,
        ours: List<Permanent>,
    ): Pair<TestGame, Map<EntityId, List<EntityId>>> {
        var builder = scenario().withRngSeed(SCENARIO_SEED).withPlayers()
        (attackers.map { Permanent(it) } + theirs).forEach { builder = builder.withCardOnBattlefield(1, it.name, tapped = it.tapped) }
        ours.forEach { builder = builder.withCardOnBattlefield(2, it.name, tapped = it.tapped) }
        val game = builder.build().advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
        val attackerIds = attackers.map { name -> game.findPermanents(name).single() }
        game.execute(DeclareAttackers(game.seatId(1), attackerIds.associateWith { game.seatId(2) }))
        game.advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
        val action = AIPlayer.create(cardRegistry, game.seatId(2), profile).chooseAction(game.state)
        return game to (action as? DeclareBlockers)?.blockers.orEmpty().filterValues { it.isNotEmpty() }
    }

    init {
        // A 3/3 into a 2/2 reads as a free three damage — the Bears can't block it profitably —
        // until the Standard and four open Plains make the Bears a 4/4 before blocks.
        test("doesn't attack into a team pump the defender can afford") {
            val game = attackPosition(
                ours = listOf(Permanent("Hill Giant")),
                theirs = pumpBoard(plainsTapped = false),
            )
            val giant = game.findPermanent("Hill Giant")!!

            val before = game.attackersChosenBy(WITHOUT)
            withClue("reproduction: attacked with ${game.names(before)}") { before shouldContain giant }

            val after = game.attackersChosenBy(AiProfile.LIVE)
            withClue("attacked with ${game.names(after)}") { after shouldNotContain giant }
        }

        test("attacks into the same pump when the defender is tapped out") {
            val game = attackPosition(
                ours = listOf(Permanent("Hill Giant")),
                theirs = pumpBoard(plainsTapped = true),
            )
            val after = game.attackersChosenBy(AiProfile.LIVE)
            withClue("attacked with ${game.names(after)}") { after shouldContain game.findPermanent("Hill Giant")!! }
        }

        // Not paralysed by a trick that can't win: two open Plains buy one pump, and a 3/3 Bears
        // still dies to a 6/4 without killing it.
        test("still attacks when the affordable pump can't win the fight") {
            val game = attackPosition(
                ours = listOf(Permanent("Craw Wurm")),
                theirs = pumpBoard(plainsTapped = false, plains = 2),
            )
            val after = game.attackersChosenBy(AiProfile.LIVE)
            withClue("attacked with ${game.names(after)}") { after shouldContain game.findPermanent("Craw Wurm")!! }
        }

        // The mirror: a 3/3 blocking a 2/2 is a free kill, unless the attacker's controller pumps
        // the team twice after blocks.
        test("doesn't block into a team pump the attacker can afford") {
            val (game, before) = blocksChosenBy(
                WITHOUT, attackers = listOf("Grizzly Bears"), theirs = pumpBoard(plainsTapped = false).drop(1),
                ours = listOf(Permanent("Hill Giant")),
            )
            withClue("reproduction: blocked with ${game.names(before.keys)}") {
                game.names(before.keys) shouldContain "Hill Giant"
            }

            val (_, after) = blocksChosenBy(
                AiProfile.LIVE, attackers = listOf("Grizzly Bears"), theirs = pumpBoard(plainsTapped = false).drop(1),
                ours = listOf(Permanent("Hill Giant")),
            )
            withClue("blocked with ${game.names(after.keys)}") { after.keys.shouldBeEmpty() }
        }

        // A 3/2 into a 1/1 reads as a free three damage — until the defender taps its artifacts
        // twice before blocks: two +1/+1 counters each time make the Tethermage a 5/5 that eats
        // the attacker and keeps the counters.
        test("doesn't attack into a +1/+1-counter pump the defender can afford") {
            val game = attackPosition(
                ours = listOf(Permanent("Divining Duelist"), Permanent("Living Library")),
                theirs = tethermageBoard(untappedArtifacts = true),
            )
            val duelist = game.findPermanent("Divining Duelist")!!

            val before = game.attackersChosenBy(WITHOUT)
            withClue("reproduction: attacked with ${game.names(before)}") { before shouldContain duelist }

            val after = game.attackersChosenBy(AiProfile.LIVE)
            withClue("attacked with ${game.names(after)}") { after shouldNotContain duelist }
        }

        test("attacks past the same Tethermage when its artifacts are tapped") {
            val game = attackPosition(
                ours = listOf(Permanent("Divining Duelist"), Permanent("Living Library")),
                theirs = tethermageBoard(untappedArtifacts = false),
            )
            val after = game.attackersChosenBy(AiProfile.LIVE)
            withClue("attacked with ${game.names(after)}") { after shouldContain game.findPermanent("Divining Duelist")!! }
        }

        // 2026-10-31 replay 5d3038e5: a 2/3 in front of a first-striking 2/1 reads as a free kill
        // — it survives two damage and strikes back — and is worth no more than the Battlecarver,
        // so local search never tried taking the block back. One {1}{R} pump after blocks makes
        // the Battlecarver a 3/1 that kills the 2/3 first.
        test("doesn't block a first striker that can pump itself past the blocker") {
            val (game, after) = blocksChosenBy(
                AiProfile.LIVE, attackers = listOf("Skilled Battlecarver"),
                theirs = List(2) { Permanent("Mountain") },
                ours = listOf(Permanent("Blind Phantasm")),
            )
            withClue("blocked with ${game.names(after.keys)}") { after.keys.shouldBeEmpty() }
        }

        test("blocks the same first striker when its controller is tapped out") {
            val (game, after) = blocksChosenBy(
                AiProfile.LIVE, attackers = listOf("Skilled Battlecarver"),
                theirs = List(2) { Permanent("Mountain", tapped = true) },
                ours = listOf(Permanent("Blind Phantasm")),
            )
            withClue("blocked with ${game.names(after.keys)}") { game.names(after.keys) shouldContain "Blind Phantasm" }
        }

        test("blocks the same attacker when its controller is tapped out") {
            val (game, after) = blocksChosenBy(
                AiProfile.LIVE, attackers = listOf("Grizzly Bears"), theirs = pumpBoard(plainsTapped = true).drop(1),
                ours = listOf(Permanent("Hill Giant")),
            )
            withClue("blocked with ${game.names(after.keys)}") { game.names(after.keys) shouldContain "Hill Giant" }
        }

        // Not a pump: a 1/3 in front of a 2/2 survives — unless the untapped Prodigal Sorcerer
        // pings it after blocks for the third point of damage.
        test("doesn't put a blocker where the attacker's pinger finishes it") {
            val (game, before) = blocksChosenBy(
                WITHOUT, attackers = listOf("Grizzly Bears"), theirs = listOf(Permanent("Prodigal Sorcerer")),
                ours = listOf(Permanent("Maritime Guard")),
            )
            withClue("reproduction: blocked with ${game.names(before.keys)}") {
                game.names(before.keys) shouldContain "Maritime Guard"
            }

            val (_, after) = blocksChosenBy(
                AiProfile.LIVE, attackers = listOf("Grizzly Bears"), theirs = listOf(Permanent("Prodigal Sorcerer")),
                ours = listOf(Permanent("Maritime Guard")),
            )
            withClue("blocked with ${game.names(after.keys)}") { after.keys.shouldBeEmpty() }
        }

        test("blocks in front of the same attacker when the pinger is tapped") {
            val (game, after) = blocksChosenBy(
                AiProfile.LIVE, attackers = listOf("Grizzly Bears"), theirs = listOf(Permanent("Prodigal Sorcerer", tapped = true)),
                ours = listOf(Permanent("Maritime Guard")),
            )
            withClue("blocked with ${game.names(after.keys)}") { game.names(after.keys) shouldContain "Maritime Guard" }
        }
    }
}
