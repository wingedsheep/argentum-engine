package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * [com.wingedsheep.ai.engine.AiProfile.refuseUnspendableGrants], measured on the policy rather than
 * through an evaluator: each position is a misplay from the 2026-10-09 log review (or its
 * control), and the claim is the verdict [HoldPolicy] hands the Strategist for it.
 *
 * Three policies are compared, matching the three profiles the flag is measured on:
 * [expiring] is `production-candidate-expiring` as it shipped (the deferral floor, nothing else),
 * [noop] is `production-noop` (the new floors alone) and [both] is `production-candidate-noop`.
 * `PuzzleSuiteTest` measures what the AI then does with the verdict.
 */
class UnspendableGrantTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private val expiring by lazy { HoldPolicy(intents, holdExpiringGrantsForCombat = true) }
    private val noop by lazy { HoldPolicy(intents, refuseUnspendableGrants = true) }
    private val both by lazy {
        HoldPolicy(intents, holdExpiringGrantsForCombat = true, refuseUnspendableGrants = true)
    }

    /** Seat 1 activating the first non-mana ability of its [cardName], aimed at [targets]. */
    private fun TestGame.activation(cardName: String, vararg targets: String): ActivateAbility {
        val ability = cardRegistry.getCard(cardName)!!.script.activatedAbilities
            .first { !it.isManaAbility }
        return ActivateAbility(
            player1Id,
            findPermanent(cardName)!!,
            ability.id,
            targets = targets.map { ChosenTarget.Permanent(findPermanent(it)!!) },
        )
    }

    private fun TestGame.verdict(policy: HoldPolicy, cardName: String, vararg targets: String) =
        policy.verdictFor(state, player1Id, cardName, activation = activation(cardName, *targets))

    /**
     * Pass priority until the stack is empty again — resolving whatever the test put there, with
     * neither seat responding.
     */
    private fun TestGame.resolveStack() {
        while (state.stack.isNotEmpty()) {
            execute(PassPriority(state.priorityPlayerId!!))
        }
    }

    init {

        // ── Poison Dart Frog: `{2}: gains deathtouch until end of turn` ──

        test("turn 24, our precombat main: the deferral holds the deathtouch for combat") {
            // g11 turn 24, four activations in one main phase on a turn the Frogs never attacked.
            // `Patience`'s long-game release had switched the deferral off from turn 14 on.
            val game = scenario().withPlayers()
                .withTurnNumber(24)
                .withCardOnBattlefield(1, "Poison Dart Frog")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardOnBattlefield(2, "Hill Giant")
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.verdict(expiring, "Poison Dart Frog") shouldBe TimingVerdict.Neutral
            game.verdict(both, "Poison Dart Frog") shouldBe TimingVerdict.NoWindow
            // The Frog can still attack, so on its own the new floor leaves it to the leaf.
            game.verdict(noop, "Poison Dart Frog") shouldBe TimingVerdict.Neutral
        }

        test("their attack is in and the Frog can block: the deathtouch is worth buying") {
            val game = scenario().withPlayers()
                .withActivePlayer(2)
                .withCardOnBattlefield(1, "Poison Dart Frog")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
                .advanceToDeclaration(2, Step.DECLARE_ATTACKERS)
                .also { it.declareAttackers(mapOf("Hill Giant" to 1)) }
                .advanceToPriority(1, Step.DECLARE_ATTACKERS)

            game.verdict(both, "Poison Dart Frog") shouldBe TimingVerdict.Neutral
            game.verdict(noop, "Poison Dart Frog") shouldBe TimingVerdict.Neutral
        }

        test("a second deathtouch grant buys nothing — pending on the stack, or already in force") {
            val game = scenario().withPlayers()
                .withActivePlayer(2)
                .withCardOnBattlefield(1, "Poison Dart Frog")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
                .advanceToDeclaration(2, Step.DECLARE_ATTACKERS)
                .also { it.declareAttackers(mapOf("Hill Giant" to 1)) }
                .advanceToPriority(1, Step.DECLARE_ATTACKERS)

            game.execute(game.activation("Poison Dart Frog")).error shouldBe null
            game.state.stack.size shouldBe 1
            game.verdict(noop, "Poison Dart Frog") shouldBe TimingVerdict.NoWindow

            game.resolveStack()
            game.verdict(noop, "Poison Dart Frog") shouldBe TimingVerdict.NoWindow
        }

        // ── Esquire of the King: `{4}{W}, {T}: Creatures you control get +1/+1 until end of turn` ──

        test("our precombat main: the team pump is held for combat, whatever the turn") {
            // g21 turns 13–23: activated in the precombat main every time, then no attack declared.
            val game = scenario().withPlayers()
                .withTurnNumber(13)
                .withCardOnBattlefield(1, "Esquire of the King")
                .withCardOnBattlefield(1, "Mushroom Watchdogs")
                .withLandsOnBattlefield(1, "Plains", 6)
                .withCardOnBattlefield(2, "Regal Unicorn")
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            // The shipped deferral cannot read a group pump at all.
            game.verdict(expiring, "Esquire of the King") shouldBe TimingVerdict.Neutral
            game.verdict(both, "Esquire of the King") shouldBe TimingVerdict.NoWindow
        }

        test("our postcombat main: no fight is left to spend the pump in") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Esquire of the King")
                .withCardOnBattlefield(1, "Mushroom Watchdogs")
                .withLandsOnBattlefield(1, "Plains", 6)
                .build().advanceToPriority(1, Step.POSTCOMBAT_MAIN)

            game.verdict(noop, "Esquire of the King") shouldBe TimingVerdict.NoWindow
        }

        test("our declare-blockers step: the pump lands on a creature that is attacking") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Esquire of the King")
                .withCardOnBattlefield(1, "Mushroom Watchdogs")
                .withLandsOnBattlefield(1, "Plains", 6)
                .withCardOnBattlefield(2, "Regal Unicorn")
                .build()
                .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                .also { it.declareAttackers(mapOf("Mushroom Watchdogs" to 2)) }
                .advanceToPriority(1, Step.DECLARE_BLOCKERS)

            game.verdict(noop, "Esquire of the King") shouldBe TimingVerdict.Neutral
            game.verdict(both, "Esquire of the King") shouldBe TimingVerdict.Neutral
        }

        // ── Dream Thrush: `{T}: Target land becomes the basic land type of your choice until EOT` ──

        test("our own turn, an opponent's land: nothing walks through it and no mana is denied") {
            // g03 turns 13, 21 and 23. Turn 21 left the Thrush tapped through their attack at 4
            // life, and it fell to 1.
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Dream Thrush")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(2, "Dismal Backwater", 1)
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.verdict(noop, "Dream Thrush", "Dismal Backwater") shouldBe TimingVerdict.NoWindow
        }

        test("a swampwalker of ours can attack through it: the land type is the attack") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Dream Thrush")
                .withCardOnBattlefield(1, "Plague Beetle")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(2, "Dismal Backwater", 1)
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.verdict(noop, "Dream Thrush", "Dismal Backwater") shouldBe TimingVerdict.Neutral
        }

        test("their main phase, their untapped land: denying the mana is a real play") {
            val game = scenario().withPlayers()
                .withActivePlayer(2)
                .withCardOnBattlefield(1, "Dream Thrush")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(2, "Dismal Backwater", 1)
                .withCardInHand(2, "Hill Giant")
                .build()
            game.state = game.state.copy(priorityPlayerId = game.player1Id)

            game.verdict(noop, "Dream Thrush", "Dismal Backwater") shouldBe TimingVerdict.Neutral
        }

        // ── Vanguard of the Rose: `{1}, Sacrifice another creature or artifact: indestructible
        //    until end of turn. Tap it.` ──

        test("our precombat main: it taps itself, so nothing is left to fight with") {
            // g11 turns 28 and 32: a creature sacrificed for indestructible with nothing
            // threatening, on a turn the Vanguard then could not attack.
            val game = scenario().withPlayers()
                .withTurnNumber(32)
                .withCardOnBattlefield(1, "Vanguard of the Rose")
                .withCardOnBattlefield(1, "Poison Dart Frog")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardOnBattlefield(2, "Hill Giant")
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.verdict(expiring, "Vanguard of the Rose") shouldBe TimingVerdict.Neutral
            game.verdict(noop, "Vanguard of the Rose") shouldBe TimingVerdict.NoWindow
        }

        test("blocking: indestructible keeps the blocker alive, and tapping it does not unblock it") {
            val game = scenario().withPlayers()
                .withActivePlayer(2)
                .withCardOnBattlefield(1, "Vanguard of the Rose")
                .withCardOnBattlefield(1, "Poison Dart Frog")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
                .advanceToDeclaration(2, Step.DECLARE_ATTACKERS)
                .also { it.declareAttackers(mapOf("Hill Giant" to 1)) }
                .advanceToDeclaration(1, Step.DECLARE_BLOCKERS)
                .also { it.declareBlockers(mapOf("Vanguard of the Rose" to listOf("Hill Giant"))) }
                .advanceToPriority(1, Step.DECLARE_BLOCKERS)

            game.verdict(noop, "Vanguard of the Rose") shouldBe TimingVerdict.Neutral
        }
    }
}
