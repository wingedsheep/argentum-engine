package com.wingedsheep.ai.puzzles.categories

import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.sdk.core.Step

/**
 * Instant timing: hold it until it does something, then actually use it.
 *
 * Half of these are negative controls (don't fire the trick in your own main phase) and half are
 * positive (fire it in the window where it wins the fight). A category made only of "don't cast"
 * puzzles would score 100% for an AI that never casts anything, which measures nothing.
 */
object HoldingInstantsPuzzles {

    fun all(): List<AiPuzzle> = listOf(

        AiPuzzle(
            id = "instants-01",
            category = PuzzleCategory.HOLDING_INSTANTS,
            expectation = "Hold Giant Growth in our own main phase — +3/+3 still loses to a 6/4 blocker",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Giant Growth")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .build()
            },
            check = { shouldNotCast("Giant Growth") },
        ),

        AiPuzzle(
            id = "instants-02",
            category = PuzzleCategory.HOLDING_INSTANTS,
            expectation = "Giant Growth on the blocked attacker: the 2/2 becomes a 5/5 and eats the 3/3",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Giant Growth")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                    .also { it.declareAttackers(mapOf("Grizzly Bears" to 2)) }
                    .advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
                    .also { it.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))) }
                    .advanceToPriority(1, Step.DECLARE_BLOCKERS)
            },
            check = {
                shouldCast("Giant Growth")
                shouldTarget("Grizzly Bears")
            },
        ),

        AiPuzzle(
            id = "instants-03",
            category = PuzzleCategory.HOLDING_INSTANTS,
            expectation = "Titanic Growth on our blocker: the 3/3 becomes a 7/7 and kills the 6/4",
            aiSeat = 2,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withCardInHand(2, "Titanic Growth")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                    .also { it.declareAttackers(mapOf("Craw Wurm" to 2)) }
                    .advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
                    .also { it.declareBlockers(mapOf("Hill Giant" to listOf("Craw Wurm"))) }
                    .advanceToPriority(2, Step.DECLARE_BLOCKERS)
            },
            check = {
                shouldCast("Titanic Growth")
                shouldTarget("Hill Giant")
            },
        ),

        AiPuzzle(
            id = "instants-04",
            category = PuzzleCategory.HOLDING_INSTANTS,
            expectation = "Hold Fog in our own main phase — there is no combat damage to prevent",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Fog")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .build()
            },
            check = { shouldNotCast("Fog") },
        ),

        AiPuzzle(
            id = "instants-05",
            category = PuzzleCategory.HOLDING_INSTANTS,
            expectation = "Fog the lethal alpha strike at 2 life",
            aiSeat = 2,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withCardInHand(2, "Fog")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLifeTotal(2, 2)
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                    .also { it.declareAttackers(mapOf("Craw Wurm" to 2, "Hill Giant" to 2)) }
                    .advanceToPriority(2, Step.DECLARE_BLOCKERS)
            },
            check = { shouldCast("Fog") },
        ),

        AiPuzzle(
            id = "instants-06",
            category = PuzzleCategory.HOLDING_INSTANTS,
            expectation = "Do not dump Giant Growth on the opponent's end step just because mana would be wasted",
            aiSeat = 2,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withCardInHand(2, "Giant Growth")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .build()
                    .advanceToPriority(2, Step.END)
            },
            // Probes `Strategist`'s hard-coded `passScore - 1.5` end-step discount directly: the
            // pump wears off in cleanup, so spending it here throws the card away for nothing.
            check = { shouldNotCast("Giant Growth") },
        ),

        AiPuzzle(
            id = "instants-07",
            category = PuzzleCategory.HOLDING_INSTANTS,
            expectation = "They took the 2/2 unblocked at 5 life — Giant Growth is exactly lethal",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLifeTotal(2, 5)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Giant Growth")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    // The blocker they declined to use. It is also a second legal target for the
                    // trick, which is the whole difficulty: with only one creature on the board the
                    // AI finds this line on every profile ever measured.
                    .withCardOnBattlefield(2, "Llanowar Elves")
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                    .also { it.declareAttackers(mapOf("Grizzly Bears" to 2)) }
                    .advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
                    .also { it.declareBlockers(emptyMap()) }
                    .advanceToPriority(1, Step.DECLARE_BLOCKERS)
            },
            check = {
                shouldCast("Giant Growth")
                shouldTarget("Grizzly Bears")
            },
        ),

        AiPuzzle(
            id = "instants-08",
            category = PuzzleCategory.HOLDING_INSTANTS,
            expectation = "Do not pump before blockers — a visible 5/5 gets chump-blocked by the 1/1",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLifeTotal(2, 5)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Giant Growth")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Llanowar Elves")
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                    .also { it.declareAttackers(mapOf("Grizzly Bears" to 2)) }
                    .advanceToPriority(1, Step.DECLARE_ATTACKERS)
            },
            // The negative control for 07: the same board, the same lethal trick, one priority
            // window earlier. Casting now is not merely premature, it *loses* the kill — blocking
            // a 2/2 with a 1/1 is a bad trade the defender declines, and chump-blocking a 5/5 to
            // live is one they take. No board evaluation can see that, because the information the
            // cast leaks is not on the board.
            check = { shouldNotCast("Giant Growth") },
        ),

        AiPuzzle(
            id = "instants-09",
            category = PuzzleCategory.HOLDING_INSTANTS,
            expectation = "Hold Restoration Angel in our own main phase — flash is worth nothing spent here",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInHand(1, "Restoration Angel")
                    // Something to ambush. Without a creature across the table the hold is still
                    // right, but for a weaker reason, and a puzzle should probe the strong one.
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
            },
            // Taken from a real game (turn 7, four Plains, precombat main). The leaf sees a 3/4
            // flier on the battlefield and scores it the same wherever in the turn it landed, so
            // casting beat passing by +4.06 and the AI jammed it. It cannot attack this turn
            // either way; what casting now spends is the whole reason flash is printed.
            check = { shouldNotCast("Restoration Angel") },
        ),

        AiPuzzle(
            id = "instants-10",
            category = PuzzleCategory.HOLDING_INSTANTS,
            expectation = "They attacked with the 3/3 — flash the Angel in and ambush it",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withActivePlayer(2)
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInHand(1, "Restoration Angel")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(2, "Mountain", 4)
                    .build()
                    .advanceToDeclaration(2, Step.DECLARE_ATTACKERS)
                    .also { it.declareAttackers(mapOf("Hill Giant" to 1)) }
                    .advanceToPriority(1, Step.DECLARE_ATTACKERS)
            },
            // The positive half, and the reason `instants-09` is not just a licence to never cast:
            // a category made only of "don't cast" positions scores 100% for an AI that never plays
            // a spell. This is the window the hold was *for* — attackers are committed, and a 3/4
            // deployed now blocks in the next step (CR 509.1).
            check = { shouldCast("Restoration Angel") },
        ),

        AiPuzzle(
            id = "instants-11",
            category = PuzzleCategory.HOLDING_INSTANTS,
            expectation = "Raging Kavu has haste — deploy it in our main phase and attack",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Raging Kavu")
                    .build()
            },
            // The haste guard. Flash and haste are printed together precisely so the body can be
            // deployed and used at once, so the domination argument `instants-09` rests on does not
            // hold: there *is* a payoff before the ambush window.
            check = { shouldCast("Raging Kavu") },
        ),

        AiPuzzle(
            id = "instants-12",
            category = PuzzleCategory.HOLDING_INSTANTS,
            expectation = "Nebelgast Herald's ETB taps a blocker — that is worth having before we attack",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInHand(1, "Nebelgast Herald")
                    // Our attacker, and the blocker the ETB would tap out of its way.
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
            },
            // The ETB guard. A flash creature whose trigger changes this turn's combat has a real
            // payoff before the ambush window, and the policy has no way to rank the two — so it
            // declines rather than guessing, and the ordinary board evaluation decides.
            check = { shouldCast("Nebelgast Herald") },
        ),

        AiPuzzle(
            id = "instants-13",
            category = PuzzleCategory.HOLDING_INSTANTS,
            expectation = "Past the patience horizon the Angel is just a body — play it",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withTurnNumber(16)
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInHand(1, "Restoration Angel")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
            },
            // The release guard, and the reason a floor this hard is not a brick: `AmbushWindow`
            // inherits `Patience`'s three exits whole, and this is the one a long game reaches.
            // Same position as `instants-09`, nine turns later.
            check = { shouldCast("Restoration Angel") },
        ),

        AiPuzzle(
            id = "instants-14",
            category = PuzzleCategory.HOLDING_INSTANTS,
            expectation = "Don't pitch a card for flying on their main phase — it expires before any combat",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withActivePlayer(2)
                    .withCardOnBattlefield(1, "Olivia's Dragoon")
                    // The card the ability's cost would eat. No lands, so it is uncastable and the
                    // activation is the only thing on offer besides passing.
                    .withCardInHand(1, "Hill Giant")
                    // A ground board across the table, so flying reads as real evasion to the
                    // evaluator — `ThreatAssessment.evasivePower` pays for it against a defender
                    // with no flier or reach. That is what makes this the strong version of the
                    // position: the keyword is genuinely good, and still cannot be spent here.
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(2, "Mountain", 4)
                    .build()
                    .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            },
            // Taken from a real game (turn 4, their precombat main, a Battleground Geist discarded
            // to give a summoning-sick 2/2 flying). `BoardPresence` prices flying at 1.5 + power ×
            // 0.3 with no reading of whether it is evasive *now*, so the leaf scored the activation
            // +2.35 over passing. The identical text on an instant is `instants-06`, which the AI
            // already gets right — the ability path had no window verdict at all.
            check = { shouldNotActivate("Olivia's Dragoon") },
        ),

        AiPuzzle(
            id = "instants-15",
            category = PuzzleCategory.HOLDING_INSTANTS,
            expectation = "They attacked with a flier — now the flying is worth a card, so buy it",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withActivePlayer(2)
                    .withCardOnBattlefield(1, "Olivia's Dragoon")
                    .withCardInHand(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Wind Drake")
                    .withLandsOnBattlefield(2, "Island", 4)
                    .build()
                    .advanceToDeclaration(2, Step.DECLARE_ATTACKERS)
                    .also { it.declareAttackers(mapOf("Wind Drake" to 1)) }
                    .advanceToPriority(1, Step.DECLARE_ATTACKERS)
            },
            // The positive half, and the same argument `instants-10` makes for the ambush: a
            // category of "don't activate" positions scores 100% for an agent that never activates
            // anything. This is the window the floor releases at, and the last one that can still
            // change a block (CR 509.1a) — the 2/2 blocks the Drake only if it has flying now.
            check = { shouldActivate("Olivia's Dragoon") },
        ),

        AiPuzzle(
            id = "instants-16",
            category = PuzzleCategory.HOLDING_INSTANTS,
            expectation = "At a full hand the discard is free — the cleanup step was taking that card anyway",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withActivePlayer(2)
                    .withCardOnBattlefield(1, "Olivia's Dragoon")
                    .withCardInHand(1, "Hill Giant")
                    .withCardInHand(1, "Craw Wurm")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Llanowar Elves")
                    .withCardInHand(1, "Trained Armodon")
                    .withCardInHand(1, "Wind Drake")
                    .withCardInHand(1, "Giant Spider")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(2, "Mountain", 4)
                    .build()
                    .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            },
            // `instants-14`'s board with a hand at `MaximumHandSize.DEFAULT`, which is the release
            // that matters most for this shape: the commonest cost on an expiring grant is a
            // discard, and at seven cards the card being pitched is the one cleanup takes. Held
            // apart from the other two `Patience` exits because those are shared with every policy
            // and this is the one that turns *this* ability's cost to zero.
            check = { shouldActivate("Olivia's Dragoon") },
        ),

        AiPuzzle(
            id = "instants-17",
            category = PuzzleCategory.HOLDING_INSTANTS,
            expectation = "Our own begin-combat: buy the flying now, or the attack is declared without it",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Olivia's Dragoon")
                    .withCardInHand(1, "Hill Giant")
                    // The blocker the flying is for: a 3/3 eats the 2/2 on the ground and cannot
                    // touch it in the air.
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
                    .advanceToPriority(1, Step.BEGIN_COMBAT)
            },
            // The asymmetry guard, and the reason the deadline is a step earlier on our own turn:
            // we declare attackers *in* `DECLARE_ATTACKERS` and only get priority there afterwards,
            // and `CombatAdvisor` reads the board as it stands. A floor that held to the same step
            // it holds to on their turn would keep the 2/2 home and then have nothing to spend the
            // grant on — the line thrown away by protecting it.
            check = { shouldActivate("Olivia's Dragoon") },
        ),
    )
}
