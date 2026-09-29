package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * All Will Be One (ONE #118) — {3}{R}{R} Enchantment.
 *
 *   Whenever you put one or more counters on a permanent or player, this enchantment deals that
 *   much damage to target opponent, creature an opponent controls, or planeswalker an opponent
 *   controls.
 *
 * Rulings pinned here: a permanent entering or a spell resolving is "you put"; combat damage from a
 * toxic source is too (CR 702.164c: its controller gives the poison counters), and several toxic
 * creatures hitting one player at once are one event — one trigger (CR 603.2c). Proliferate is one
 * placement per recipient, so a permanent given two kinds triggers once for "that much" = 2.
 *
 * Every board gives the opponent a creature so each trigger has to *ask* for its target, which is
 * how the tests count triggers rather than just totalling damage.
 */
class AllWillBeOneScenarioTest : ScenarioTestBase() {

    private val growth = card("Test Double Growth") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Put two +1/+1 counters on target creature."
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, t)
        }
    }

    private val spreading = card("Test Spreading") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Proliferate."
        spell { effect = Effects.Proliferate() }
    }

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun life(game: TestGame, playerId: EntityId): Int =
        game.state.getEntity(playerId)!!.get<LifeTotalComponent>()!!.life

    private fun poison(game: TestGame, playerId: EntityId): Int =
        game.state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    /**
     * Drain the stack; every trigger target prompt is aimed at player 2's face. Proliferate's own
     * choice ([proliferateChoice]) is answered with the given recipients. Returns how many trigger
     * target prompts there were.
     */
    private fun drain(game: TestGame, proliferateChoice: List<EntityId>? = null): Int {
        var prompts = 0
        var guard = 0
        var proliferated = false
        while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 40) {
            if (game.hasPendingDecision()) {
                if (proliferateChoice != null && !proliferated) {
                    game.selectCards(proliferateChoice)
                    proliferated = true
                } else {
                    game.selectTargets(listOf(game.player2Id)).error shouldBe null
                    prompts++
                }
            } else {
                game.resolveStack()
            }
        }
        return prompts
    }

    init {
        cardRegistry.register(growth)
        cardRegistry.register(spreading)

        test("putting two +1/+1 counters on your creature deals 2 damage") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "All Will Be One")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInHand(1, "Test Double Growth")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Test Double Growth", bears).error shouldBe null
            drain(game) shouldBe 1

            life(game, game.player2Id) shouldBe 18
        }

        test("proliferate triggers once per recipient, including a player, with that recipient's count") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "All Will Be One")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInHand(1, "Test Spreading")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)
            seed(game, bears, CounterType.OIL, 1)
            seed(game, game.player2Id, CounterType.POISON, 1)

            game.castSpell(1, "Test Spreading").error shouldBe null
            val prompts = drain(game, proliferateChoice = listOf(bears, game.player2Id))

            withClue("one trigger for the Bears (two kinds, one event) and one for the poisoned player") {
                prompts shouldBe 2
            }
            // 2 (Bears got a +1/+1 and an oil counter) + 1 (the player got a poison counter).
            life(game, game.player2Id) shouldBe 17
        }

        test("two toxic creatures connecting at once are one event and one trigger") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "All Will Be One")
                .withCardOnBattlefield(1, "Phyrexian Mite", isToken = true)
                .withCardOnBattlefield(1, "Phyrexian Mite", isToken = true)
                .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val mites = game.findAllPermanents("Phyrexian Mite")
            mites.size shouldBe 2

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.execute(DeclareAttackers(game.player1Id, mites.associateWith { game.player2Id })).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null

            var prompts = 0
            var guard = 0
            while (game.state.step != Step.POSTCOMBAT_MAIN && guard++ < 40) {
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(game.player2Id)).error shouldBe null
                    prompts++
                } else {
                    game.execute(PassPriority(game.state.priorityPlayerId!!))
                }
            }

            poison(game, game.player2Id) shouldBe 2
            withClue("one trigger for the single poison placement") { prompts shouldBe 1 }
            // 2 combat damage + 2 from the trigger.
            life(game, game.player2Id) shouldBe 16
        }

        test("counters an opponent puts don't trigger it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "All Will Be One")
                .withCardOnBattlefield(2, "Phyrexian Mite", isToken = true)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Phyrexian Mite" to 1)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            poison(game, game.player1Id) shouldBe 1
            game.state.stack.isEmpty() shouldBe true
            life(game, game.player2Id) shouldBe 20
        }
    }
}
