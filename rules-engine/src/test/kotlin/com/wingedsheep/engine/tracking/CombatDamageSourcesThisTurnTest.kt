package com.wingedsheep.engine.tracking

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.battlefield.DamageSourceLki
import com.wingedsheep.engine.state.components.player.CombatDamageSourcesThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * `DynamicAmount.CombatDamageSourcesThisTurn(player, sourceFilter)` and the per-player
 * `CombatDamageSourcesThisTurnComponent` record behind it — "a player who was dealt combat damage by
 * three or more Pirates this turn" (Admiral Beckett Brass), "an opponent was dealt combat damage by a
 * legendary creature this turn" (Blitzball). Pins the card's rulings: sources count as they were when
 * they dealt the damage (one that has since left still counts), a double striker counts once, only
 * combat damage to that player counts, and the record clears at end of turn.
 */
class CombatDamageSourcesThisTurnTest : ScenarioTestBase() {

    private fun pirate(name: String, vararg keywords: Keyword) = card(name) {
        manaCost = "{1}"
        typeLine = "Creature — Human Pirate"
        power = 1
        toughness = 1
        if (keywords.isNotEmpty()) this.keywords(*keywords)
    }

    private val pirates = GameObjectFilter.Creature.withSubtype(Subtype.PIRATE)

    private fun ScenarioTestBase.TestGame.count(
        player: EntityId,
        filter: GameObjectFilter = pirates,
    ): Int = services.dynamicAmountEvaluator.evaluate(
        state,
        DynamicAmount.CombatDamageSourcesThisTurn(Player.You, filter),
        EffectContext(sourceId = null, controllerId = player),
    )

    init {
        cardRegistry.register(pirate("Test Pirate A"))
        cardRegistry.register(pirate("Test Pirate B"))
        cardRegistry.register(pirate("Test Pirate C"))
        cardRegistry.register(pirate("Test Double-Strike Pirate", Keyword.DOUBLE_STRIKE))

        test("a source recorded with two different snapshots is still one source") {
            // First-strike damage as a Pirate, regular damage after it stopped being one.
            val you = EntityId.generate()
            val source = EntityId.generate()
            val asPirate = DamageSourceLki(you, source, sourceSubtypes = setOf(Subtype.PIRATE), amount = 1)
            val asOther = DamageSourceLki(you, source, sourceSubtypes = emptySet(), amount = 1)
            val record = CombatDamageSourcesThisTurnComponent().adding(asPirate).adding(asOther).adding(asPirate)

            record.sources.size shouldBe 2
            record.countMatching(pirates, you) shouldBe 1
            record.countMatching(GameObjectFilter.Creature, you) shouldBe 1
        }

        test("each distinct Pirate that connects counts; a non-Pirate doesn't") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Test Pirate A", summoningSickness = false)
                .withCardOnBattlefield(1, "Test Pirate B", summoningSickness = false)
                .withCardOnBattlefield(1, "Test Pirate C", summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(
                mapOf("Test Pirate A" to 2, "Test Pirate B" to 2, "Test Pirate C" to 2, "Grizzly Bears" to 2)
            ).error shouldBe null
            game.declareNoBlockers()
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.count(game.player2Id) shouldBe 3
            withClue("any creature: the Bears count too") {
                game.count(game.player2Id, GameObjectFilter.Creature) shouldBe 4
            }
            withClue("the attacking player was dealt no combat damage") {
                game.count(game.player1Id) shouldBe 0
            }
        }

        test("a double striker deals combat damage twice but counts as one source") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Test Double-Strike Pirate", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Test Double-Strike Pirate" to 2)).error shouldBe null
            game.declareNoBlockers()
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("two hits landed") { game.getLifeTotal(2) shouldBe 18 }
            game.count(game.player2Id) shouldBe 1
        }

        test("a blocked Pirate dealt no combat damage to the player and doesn't count") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Test Pirate A", summoningSickness = false)
                .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Test Pirate A" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Hill Giant" to listOf("Test Pirate A"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.count(game.player2Id) shouldBe 0
        }

        test("a Pirate that has since left the battlefield still counts") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Test Pirate A", summoningSickness = false)
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Test Pirate A" to 2)).error shouldBe null
            game.declareNoBlockers()
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            val pirate = game.findPermanent("Test Pirate A")!!
            game.castSpell(1, "Shock", pirate).error shouldBe null
            game.resolveStack()

            withClue("the Pirate died") { game.findPermanent("Test Pirate A") shouldBe null }
            game.count(game.player2Id) shouldBe 1
        }

        test("noncombat damage from a Pirate isn't recorded, and the record clears at end of turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Test Pirate A", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Test Pirate A" to 2)).error shouldBe null
            game.declareNoBlockers()
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.count(game.player2Id) shouldBe 1

            game.passUntilPhase(Phase.ENDING, Step.END)
            withClue("still recorded in the end step, where Beckett's trigger reads it") {
                game.count(game.player2Id) shouldBe 1
            }
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.getEntity(game.player2Id)?.get<CombatDamageSourcesThisTurnComponent>() shouldBe null
            game.count(game.player2Id) shouldBe 0
        }
    }
}
