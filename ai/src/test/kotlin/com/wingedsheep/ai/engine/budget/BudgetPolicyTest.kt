package com.wingedsheep.ai.engine.budget

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Which window gets which tier.
 *
 * The tier assignment is where a budget can be *wrong* rather than merely small: a lethal window
 * mis-tiered as ROUTINE drops the simulation-refined target pick at the exact moment it matters.
 */
class BudgetPolicyTest : FunSpec({

    val bear = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2, toughness = 2,
    )

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(listOf(bear))
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
    }

    fun castAction(playerId: EntityId) = LegalAction(
        action = PassPriority(playerId),
        actionType = "CastSpell",
        description = "Cast something",
    )

    val policy = TieredBudgetPolicy()

    /**
     * One Bear attacking a player on 5, blocks declined — `instants-07`'s board. Two power against
     * five life, so the attack is *not* lethal as it stands; a trick is what would make it so.
     */
    fun attackingIntoLethal(): GameTestDriver = driver().apply {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        val attacker = putCreatureOnBattlefield(player1, "Grizzly Bears")
            .also { removeSummoningSickness(it) }
        replaceState(state.withLifeTotal(player2, 5))
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, listOf(attacker), player2)
        passPriorityUntil(Step.DECLARE_BLOCKERS)
    }

    test("nothing meaningful to choose between is TRIVIAL") {
        val d = driver()
        policy.tierFor(d.state, d.player1, emptyList()) shouldBe BudgetTier.TRIVIAL
    }

    test("a combat declaration is always CRITICAL") {
        val d = driver()
        val declare = LegalAction(
            action = DeclareAttackers(d.player1, emptyMap()),
            actionType = "DeclareAttackers",
            description = "Declare attackers",
        )
        policy.tierFor(d.state, d.player1, listOf(declare)) shouldBe BudgetTier.CRITICAL
    }

    test("our own main phase is NORMAL while nobody is threatening lethal") {
        val d = driver()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        policy.tierFor(d.state, d.player1, listOf(castAction(d.player1))) shouldBe BudgetTier.NORMAL
    }

    test("an opponent's quiet upkeep is ROUTINE") {
        val d = driver()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        // player2 is not the active player, and their window is not a main phase.
        policy.tierFor(d.state, d.player2, listOf(castAction(d.player2))) shouldBe BudgetTier.ROUTINE
    }

    test("a board that can kill somebody this turn promotes even a quiet window to CRITICAL") {
        val d = driver()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        repeat(4) {
            val attacker = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
            d.removeSummoningSickness(attacker)
        }
        d.replaceState(d.state.withLifeTotal(d.player2, 8)) // 4 x 2 power >= 8 life
        policy.tierFor(d.state, d.player2, listOf(castAction(d.player2))) shouldBe BudgetTier.CRITICAL
    }

    test("blocks in and damage next is ROUTINE by default") {
        // Declarations are spent, and `someoneIsInLethalRange` cannot fire: attacking taps the
        // creature (CR 508.1f), so the attacking side reads as zero power — and a trick's whole
        // job is to make lethal an attack that is not lethal yet. So the window where an instant
        // converts an attack falls through to the same tier as a quiet upkeep.
        val d = attackingIntoLethal()
        policy.tierFor(d.state, d.player1, listOf(castAction(d.player1))) shouldBe BudgetTier.ROUTINE
    }

    test("grading the pre-damage window NORMAL is what restores the refined target pick") {
        val d = attackingIntoLethal()
        val graded = TieredBudgetPolicy(preDamageCombatIsNormal = true)
        graded.tierFor(d.state, d.player1, listOf(castAction(d.player1))) shouldBe BudgetTier.NORMAL
        // The threshold that actually matters — below NORMAL_MILLIS a spell's target is whatever
        // the heuristic names first, which is the mechanism `instants-07` fails on.
        graded.budgetFor(d.state, d.player1, listOf(castAction(d.player1)))
            .allowances.refineTargetsBySimulation shouldBe true
    }

    test("the legacy policy never tiers at all") {
        val d = driver()
        LegacyBudgetPolicy.budgetFor(d.state, d.player1, emptyList()).allowances shouldBe
            SearchAllowances.LEGACY
        LegacyBudgetPolicy.budgetForDecision(d.state, d.player1).allowances shouldBe
            SearchAllowances.LEGACY
    }
})
