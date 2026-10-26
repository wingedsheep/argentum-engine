package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Filtered "whenever a [filter] attacks" triggers functioning from the graveyard (CR 113.6b/m).
 * The graveyard pass must read the attacker filter — relative to the card's owner, who controls
 * its abilities — and fire once per matching attacker, since one declaration with several
 * attackers is several occurrences of the trigger event (CR 603.2c). Before this, it fired once
 * for any declaration at all (Vivi's Persistence returned on a non-commander's attack).
 */
class GraveyardAttackTriggerScenarioTest : ScenarioTestBase() {

    private val goblinA = card("Test Goblin A") {
        manaCost = "{R}"; typeLine = "Creature — Goblin"; power = 1; toughness = 1
    }
    private val goblinB = card("Test Goblin B") {
        manaCost = "{R}"; typeLine = "Creature — Goblin"; power = 1; toughness = 1
    }
    private val watcher = card("Graveyard Goblin Attack Watcher") {
        manaCost = "{1}"; typeLine = "Instant"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.withSubtype("Goblin").youControl()).attacks()
            triggerZone = Zone.GRAVEYARD
            effect = Effects.GainLife(1)
        }
    }
    private val disjunctionWatcher = card("Graveyard Goblin Enters Or Attacks Watcher") {
        manaCost = "{1}"; typeLine = "Instant"
        triggeredAbility {
            trigger = Triggers.or(
                Triggers.a(GameObjectFilter.Creature.withSubtype("Goblin").youControl()).enters(),
                Triggers.a(GameObjectFilter.Creature.withSubtype("Goblin").youControl()).attacks(),
            )
            triggerZone = Zone.GRAVEYARD
            effect = Effects.GainLife(1)
        }
    }

    init {
        cardRegistry.register(goblinA)
        cardRegistry.register(goblinB)
        cardRegistry.register(watcher)
        cardRegistry.register(disjunctionWatcher)

        fun attack(watcherName: String, attackers: List<String>, activePlayer: Int = 1): TestGame {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInGraveyard(1, watcherName)
                .withCardOnBattlefield(activePlayer, "Test Goblin A")
                .withCardOnBattlefield(activePlayer, "Test Goblin B")
                .withCardOnBattlefield(activePlayer, "Grizzly Bears")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(activePlayer)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            val defender = if (activePlayer == 1) 2 else 1
            game.declareAttackers(attackers.associateWith { defender }).error shouldBe null
            return game
        }

        test("fires once per matching attacker, ignoring attackers outside the filter") {
            val game = attack("Graveyard Goblin Attack Watcher", listOf("Test Goblin A", "Test Goblin B", "Grizzly Bears"))
            withClue("two Goblins attacked: two triggers on the stack, none for the Bears") {
                game.state.stack.size shouldBe 2
            }
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 22
        }

        test("doesn't fire when only creatures outside the filter attack") {
            val game = attack("Graveyard Goblin Attack Watcher", listOf("Grizzly Bears"))
            game.state.stack.size shouldBe 0
            game.getLifeTotal(1) shouldBe 20
        }

        test("'you control' reads the card's owner: an opponent's attacking Goblins don't trigger it") {
            val game = attack("Graveyard Goblin Attack Watcher", listOf("Test Goblin A", "Test Goblin B"), activePlayer = 2)
            game.state.stack.size shouldBe 0
            game.getLifeTotal(1) shouldBe 20
        }

        test("an enters-or-attacks disjunction fans out per matching attacker like its bare attacks() atom") {
            val game = attack("Graveyard Goblin Enters Or Attacks Watcher", listOf("Test Goblin A", "Test Goblin B", "Grizzly Bears"))
            game.state.stack.size shouldBe 2
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 22
        }
    }
}
