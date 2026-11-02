package com.wingedsheep.engine.replacement

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.PermanentsEnterTapped
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The "instead it enters tapped and attacking" axis of the global [PermanentsEnterTapped]
 * replacement (`attackDefender = CONTROLLER_CHOOSES`), through Kira Nerys, Firebrand: "If a creature
 * you control would enter while you control an attacking creature, instead it enters tapped and
 * attacking."
 *
 * Rules pinned: CR 508.4 (the entering creature's controller chooses which defending player,
 * planeswalker or battle it attacks — asked only when there is a choice; such a creature is attacking
 * but never "attacked"), CR 506.3a (a noncreature permanent never attacks), CR 506.3b (a creature
 * entering under a non-attacking player's control never attacks), and that every entry seam —
 * a resolving permanent spell, created tokens, and a card an effect puts onto the battlefield —
 * consults the same replacement.
 */
class PermanentsEnterTappedAndAttackingTest : ScenarioTestBase() {

    private fun TestGame.isTapped(id: EntityId) = state.getEntity(id)?.has<TappedComponent>() == true
    private fun TestGame.attacking(id: EntityId) = state.getEntity(id)?.get<AttackingComponent>()

    /** Kira and a Grizzly Bears on player 1's side, declared attacking player 2. */
    private fun attackingBoard(extra: ScenarioBuilder.() -> ScenarioBuilder = { this }): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Kira Nerys, Firebrand")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Forest", 2)
            .withLandsOnBattlefield(1, "Plains", 4)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .extra()
            .build()
        game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        context("PermanentsEnterTapped with attackDefender") {

            test("rejects defender rules a replacement can't follow") {
                shouldThrow<IllegalArgumentException> {
                    PermanentsEnterTapped(attackDefender = EnteringAttackDefender.JOIN_SOURCE)
                }
                shouldThrow<IllegalArgumentException> {
                    PermanentsEnterTapped(attackDefender = EnteringAttackDefender.RETURNED_ATTACKER)
                }
            }

            test("a creature spell resolving while you control an attacker enters tapped and attacking") {
                val game = attackingBoard { withCardInHand(1, "Ambush Viper") }
                game.castSpell(1, "Ambush Viper").error shouldBe null
                game.resolveStack()

                val viper = game.findPermanent("Ambush Viper")!!
                game.isTapped(viper) shouldBe true
                withClue("only one legal defender — no question, it attacks player 2") {
                    game.hasPendingDecision() shouldBe false
                    game.attacking(viper)?.defenderId shouldBe game.player2Id
                }
            }

            test("created tokens enter tapped and attacking") {
                val game = attackingBoard { withCardInHand(1, "Raise the Alarm") }
                game.castSpell(1, "Raise the Alarm").error shouldBe null
                game.resolveStack()

                val soldiers = game.findPermanents("Soldier Token")
                soldiers.size shouldBe 2
                soldiers.forEach {
                    game.isTapped(it) shouldBe true
                    game.attacking(it)?.defenderId shouldBe game.player2Id
                }
            }

            test("a card an effect returns to the battlefield enters tapped and attacking") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kira Nerys, Firebrand")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInHand(1, "Cloudshift")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.resolveStack()

                // Blink the untapped, non-attacking Elves: the returning card is a new object
                // entering under Kira's replacement.
                game.castSpell(1, "Cloudshift", game.findPermanent("Llanowar Elves")!!).error shouldBe null
                game.resolveStack()

                val elves = game.findPermanent("Llanowar Elves")!!
                game.isTapped(elves) shouldBe true
                game.attacking(elves)?.defenderId shouldBe game.player2Id
            }

            test("with no attacking creature the replacement doesn't apply") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kira Nerys, Firebrand")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInHand(1, "Ambush Viper")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpell(1, "Ambush Viper").error shouldBe null
                game.resolveStack()

                val viper = game.findPermanent("Ambush Viper")!!
                game.isTapped(viper) shouldBe false
                game.attacking(viper) shouldBe null
            }

            test("an \"enters attacking\" trigger sees the creature attacking, even after a defender choice") {
                val game = attackingBoard {
                    withCardOnBattlefield(1, "Arni Metalbrow")
                        .withCardInHand(1, "Ambush Viper")
                        .withCardOnBattlefield(2, "Jace Beleren")
                }
                // Declaring the Bears triggered Arni ("attacks"); decline its payment.
                while (game.hasPendingDecision() || game.state.stack.isNotEmpty()) {
                    if (game.getPendingDecision() != null) game.answerYesNo(false) else game.resolveStack()
                }
                game.castSpell(1, "Ambush Viper").error shouldBe null
                game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(game.player2Id)).error shouldBe null

                val triggered = game.state.stack.mapNotNull {
                    game.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()?.sourceName
                } + listOfNotNull(game.getPendingDecision()?.let { "pending:${it::class.simpleName}" })
                withClue("Arni's \"enters attacking\" half triggers for the Viper: $triggered") {
                    triggered.contains("Arni Metalbrow") shouldBe true
                }
                game.attacking(game.findPermanent("Ambush Viper")!!)?.defenderId shouldBe game.player2Id
            }

            test("an opponent's creature entering isn't affected") {
                val game = attackingBoard { withCardInHand(2, "Ambush Viper").withLandsOnBattlefield(2, "Forest", 2) }
                game.passPriority() // player 1 passes; player 2 casts at instant speed
                game.castSpell(2, "Ambush Viper").error shouldBe null
                game.resolveStack()

                val viper = game.findPermanent("Ambush Viper")!!
                game.isTapped(viper) shouldBe false
                game.attacking(viper) shouldBe null
            }

            test("with a planeswalker to attack, the controller chooses what the creature attacks") {
                val game = attackingBoard {
                    withCardInHand(1, "Ambush Viper").withCardOnBattlefield(2, "Jace Beleren")
                }
                val jace = game.findPermanent("Jace Beleren")!!
                game.castSpell(1, "Ambush Viper").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                decision.playerId shouldBe game.player1Id
                decision.legalTargets[0]!!.toSet() shouldBe setOf(game.player2Id, jace)

                game.selectTargets(listOf(jace)).error shouldBe null
                val viper = game.findPermanent("Ambush Viper")!!
                game.isTapped(viper) shouldBe true
                game.attacking(viper)?.defenderId shouldBe jace
            }
        }
    }
}
