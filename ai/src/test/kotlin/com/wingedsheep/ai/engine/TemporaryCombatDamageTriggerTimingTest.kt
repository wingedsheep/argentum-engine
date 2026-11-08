package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.effects.CreateGlobalTriggeredAbilityEffect
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * An until-end-of-turn global trigger paid off only by creatures of ours dealing combat damage to
 * a player — Aphelia, Viper Whisperer's "{4}{B}: Until end of turn, whenever one or more Gorgons
 * and/or Snakes you control deal combat damage to a player, that player loses half their life,
 * rounded up" ([ExpiringGrantWindow]'s `CONNECT` payoff).
 *
 * The window is our own combat, once blocks show a matching attacker connects. Before blocks the
 * activation waits; their turn, our second main, or an attacker that was blocked has no window, and
 * the mana is wasted.
 */
class TemporaryCombatDamageTriggerTimingTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    /** The grant floors [AiProfile.LIVE] ships. */
    private val livePolicy by lazy {
        HoldPolicy(intents, holdExpiringGrantsForCombat = true, refuseUnspendableGrants = true, lifelinkWaitsForBlocks = true)
    }

    /** The "nothing can spend it" floor alone, without the deferral to a later window. */
    private val floorOnlyPolicy by lazy { HoldPolicy(intents, refuseUnspendableGrants = true) }

    /** A trampling Snake, so a blocked attacker can still connect. */
    private val tramplingSnake = card("Trampling Test Snake") {
        manaCost = "{3}{G}"
        typeLine = "Creature — Snake"
        power = 4
        toughness = 4
        keywords(Keyword.TRAMPLE)
    }

    private fun TestGame.apheliaActivation(): ActivateAbility {
        val ability = cardRegistry.getCard(APHELIA)!!.script.activatedAbilities.single()
        return ActivateAbility(player1Id, findPermanent(APHELIA)!!, ability.id)
    }

    private fun TestGame.verdict(policy: HoldPolicy = livePolicy): TimingVerdict =
        policy.verdictFor(state, player1Id, APHELIA, activation = apheliaActivation())

    private fun TestGame.jacePlusOne(): ActivateAbility {
        // The +1: the loyalty ability whose effect is the temporary combat-damage trigger.
        val ability = cardRegistry.getCard(JACE)!!.script.activatedAbilities.first {
            it.isPlaneswalkerAbility && it.effect is CreateGlobalTriggeredAbilityEffect
        }
        return ActivateAbility(player1Id, findPermanent(JACE)!!, ability.id)
    }

    private fun TestGame.choice(): GameAction =
        AIPlayer.create(cardRegistry, player1Id, AiProfile.LIVE).chooseAction(state)

    private fun GameAction.isAphelia(game: TestGame): Boolean =
        this is ActivateAbility && sourceId == game.findPermanent(APHELIA)

    /** Aphelia at home, a Python (a vanilla 3/2 Snake) and five Swamps of ours. */
    private fun board(
        active: Int = 1,
        vararg theirCreatures: String,
        snake: String = "Python",
        sick: Boolean = false,
    ) = scenario().withPlayers()
        .withActivePlayer(active)
        .withTurnNumber(3)
        .withCardOnBattlefield(1, APHELIA, summoningSickness = sick)
        .withCardOnBattlefield(1, snake, summoningSickness = sick)
        .withLandsOnBattlefield(1, "Swamp", 5)
        .apply { theirCreatures.forEach { withCardOnBattlefield(2, it) } }
        .build()

    /** Our turn, [snake] attacking player two. */
    private fun pythonAttacks(vararg theirCreatures: String, snake: String = "Python") =
        board(1, *theirCreatures, snake = snake)
            .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
            .also { it.declareAttackers(mapOf(snake to 2)).error shouldBe null }

    init {
        cardRegistry.register(listOf(tramplingSnake))

        test("the Python is unblocked: the AI activates before damage, and the opponent loses half") {
            val game = pythonAttacks().advanceToPriority(1, Step.DECLARE_BLOCKERS)

            game.verdict() shouldNotBe TimingVerdict.NoWindow
            val action = game.choice().shouldBeInstanceOf<ActivateAbility>()
            action.isAphelia(game) shouldBe true

            game.execute(action).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            // 20 - 3 combat damage = 17, then half of 17 rounded up = 9.
            game.getLifeTotal(2) shouldBe 8
        }

        test("attackers declared, blocks not yet known: the trigger waits for blocks") {
            val game = pythonAttacks().advanceToPriority(1, Step.DECLARE_ATTACKERS)

            game.verdict() shouldBe TimingVerdict.NoWindow
            game.choice().isAphelia(game) shouldBe false
        }

        test("the Python is blocked: nothing of ours can connect") {
            val game = pythonAttacks("Grizzly Bears")
                .advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
                .also { it.declareBlockers(mapOf("Grizzly Bears" to listOf("Python"))).error shouldBe null }
                .advanceToPriority(1, Step.DECLARE_BLOCKERS)

            game.verdict() shouldBe TimingVerdict.NoWindow
            game.choice().isAphelia(game) shouldBe false
        }

        test("a blocked trampler still connects: the window stays open") {
            val snake = tramplingSnake.name
            val game = pythonAttacks("Grizzly Bears", snake = snake)
                .advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
                .also { it.declareBlockers(mapOf("Grizzly Bears" to listOf(snake))).error shouldBe null }
                .advanceToPriority(1, Step.DECLARE_BLOCKERS)

            game.verdict() shouldNotBe TimingVerdict.NoWindow
        }

        test("precombat main: Aphelia (a Gorgon) and the Snake both summoning-sick, nothing can connect") {
            val ready = board().advanceToPriority(1, Step.PRECOMBAT_MAIN)
            ready.verdict(floorOnlyPolicy) shouldBe TimingVerdict.Neutral

            val sick = board(sick = true).advanceToPriority(1, Step.PRECOMBAT_MAIN)
            sick.verdict(floorOnlyPolicy) shouldBe TimingVerdict.NoWindow
            sick.choice().isAphelia(sick) shouldBe false
        }

        test("Jace, Cunning Castaway's +1 is still taken with no attacker, precombat and postcombat: the loyalty is permanent") {
            for (step in listOf(Step.PRECOMBAT_MAIN, Step.POSTCOMBAT_MAIN)) {
                val game = scenario().withPlayers()
                    .withActivePlayer(1)
                    .withTurnNumber(3)
                    .withCardOnBattlefield(1, JACE)
                    .build()
                    .advanceToPriority(1, step)
                withClue(step) {
                    livePolicy.verdictFor(game.state, game.player1Id, JACE, activation = game.jacePlusOne()) shouldNotBe
                        TimingVerdict.NoWindow
                    val action = game.choice().shouldBeInstanceOf<ActivateAbility>()
                    action.sourceId shouldBe game.findPermanent(JACE)
                    action.abilityId shouldBe game.jacePlusOne().abilityId
                }
            }
        }

        test("our second main, after the Python connected: combat is over") {
            val game = pythonAttacks().advanceToPriority(1, Step.POSTCOMBAT_MAIN)

            game.verdict() shouldBe TimingVerdict.NoWindow
            game.choice().isAphelia(game) shouldBe false
        }

        test("the opponent's turn: our creatures deal no combat damage to a player") {
            for (step in listOf(Step.PRECOMBAT_MAIN, Step.DECLARE_ATTACKERS, Step.END)) {
                val game = board(active = 2).advanceToPriority(1, step)
                withClue(step) {
                    game.verdict() shouldBe TimingVerdict.NoWindow
                    game.choice().isAphelia(game) shouldBe false
                }
            }
        }

        test("Aphelia reads fully") {
            val reading = AiReadingAudit.read(cardRegistry.requireCard(APHELIA))
            withClue(reading.toString()) { reading.readFully shouldBe true }
        }
    }

    private companion object {
        const val APHELIA = "Aphelia, Viper Whisperer"
        const val JACE = "Jace, Cunning Castaway"
    }
}
