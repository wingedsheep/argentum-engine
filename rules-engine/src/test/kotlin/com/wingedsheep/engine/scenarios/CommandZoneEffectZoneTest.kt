package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * The command zone as an ordinary effect source and destination — "put a commander you own from
 * the command zone onto the battlefield" / "return it to the command zone" (Hellkite Courser).
 * Nothing here is new vocabulary: `CardSource.FromZone(Zone.COMMAND, …)` gathers from the owner's
 * command zone, `StatePredicate.IsCommander` (`.commander()`) keeps emblems, Vanguard avatars and
 * other non-commander objects out, and `Zone.COMMAND` is a plain `MoveToZone` / `MoveCollection`
 * destination. This pins those pieces at the engine level:
 *  - the gather sees only commanders the caster owns, never an opponent's or a non-commander card;
 *  - leaving the command zone for the battlefield is a real zone change (event emitted) and is not a
 *    cast, so commander tax doesn't grow;
 *  - a move *to* the command zone is a real zone change, lands in the **owner's** command zone even
 *    when another player resolves it, and isn't treated as a same-zone no-op;
 *  - with nothing to gather the effect does nothing.
 */
class CommandZoneEffectZoneTest : ScenarioTestBase() {

    private val commanderBear = card("Test Commander Bear") {
        manaCost = "{1}{G}"
        typeLine = "Legendary Creature — Bear"
        power = 2
        toughness = 2
    }

    private val bystander = card("Test Command Bystander") {
        manaCost = "{1}"
        typeLine = "Legendary Artifact"
    }

    private val summons = card("Test Command Summons") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        oracleText = "Put each commander you own from the command zone onto the battlefield."
        spell {
            effect = Effects.Pipeline {
                val commanders = gather(
                    CardSource.FromZone(Zone.COMMAND, Player.You, GameObjectFilter.Any.commander().ownedByYou()),
                )
                move(commanders, CardDestination.ToZone(Zone.BATTLEFIELD))
            }
        }
    }

    private val recall = card("Test Command Recall") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Put target creature into the command zone."
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Move(t, Zone.COMMAND)
        }
    }

    init {
        cardRegistry.register(commanderBear)
        cardRegistry.register(bystander)
        cardRegistry.register(summons)
        cardRegistry.register(recall)
    }

    private fun TestGame.commandZone(player: Int): List<EntityId> =
        state.getZone(ZoneKey(if (player == 1) player1Id else player2Id, Zone.COMMAND))

    private fun TestGame.makeCommander(player: Int, name: String): EntityId {
        val playerId = if (player == 1) player1Id else player2Id
        val id = commandZone(player).single { state.getEntity(it)?.get<CardComponent>()?.name == name }
        state = state.updateEntity(id) { it.with(CommanderComponent(ownerId = playerId)) }
        return id
    }

    private fun List<com.wingedsheep.engine.core.ExecutionResult>.zoneChanges(id: EntityId) =
        flatMap { it.events }.filterIsInstance<ZoneChangeEvent>().filter { it.entityId == id }

    init {
        test("gathers only commanders the caster owns, and moves them out as a zone change, not a cast") {
            val game = scenario()
                .withPlayers()
                .withCardInCommandZone(1, "Test Commander Bear")
                .withCardInCommandZone(1, "Test Command Bystander")
                .withCardInCommandZone(2, "Test Commander Bear")
                .withCardInHand(1, "Test Command Summons")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val mine = game.makeCommander(1, "Test Commander Bear")
            val theirs = game.makeCommander(2, "Test Commander Bear")

            game.castSpell(1, "Test Command Summons").error shouldBe null
            val results = game.resolveStack()

            game.state.getBattlefield().filter {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Test Commander Bear"
            } shouldContainExactly listOf(mine)
            withClue("the non-commander and the opponent's commander stay in their command zones") {
                game.commandZone(1).map { game.state.getEntity(it)?.get<CardComponent>()?.name } shouldBe
                    listOf("Test Command Bystander")
                game.commandZone(2) shouldContainExactly listOf(theirs)
            }
            results.zoneChanges(mine).map { it.fromZone to it.toZone } shouldBe listOf(Zone.COMMAND to Zone.BATTLEFIELD)
            game.state.getEntity(mine)?.get<CommanderComponent>()?.castsFromCommandZone shouldBe 0
        }

        test("a move to the command zone is a real zone change into the owner's command zone") {
            val game = scenario()
                .withPlayers()
                .withCardInCommandZone(1, "Test Commander Bear")
                .withCardInHand(1, "Test Command Summons")
                .withCardInHand(2, "Test Command Recall")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(2, "Island", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bear = game.makeCommander(1, "Test Commander Bear")
            game.castSpell(1, "Test Command Summons").error shouldBe null
            game.resolveStack()
            game.findPermanent("Test Commander Bear") shouldBe bear

            // The opponent resolves the move: the destination is still the owner's command zone.
            game.passPriority()
            game.castSpell(2, "Test Command Recall", bear).error shouldBe null
            val results = game.resolveStack()

            game.findPermanent("Test Commander Bear") shouldBe null
            game.commandZone(1) shouldContainExactly listOf(bear)
            game.commandZone(2) shouldBe emptyList()
            results.zoneChanges(bear).map { it.fromZone to it.toZone } shouldBe listOf(Zone.BATTLEFIELD to Zone.COMMAND)
        }

        test("nothing to gather: the effect does nothing") {
            val game = scenario()
                .withPlayers()
                .withCardInCommandZone(1, "Test Command Bystander")
                .withCardInHand(1, "Test Command Summons")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Test Command Summons").error shouldBe null
            game.resolveStack()

            game.commandZone(1).size shouldBe 1
            game.state.getBattlefield().size shouldBe 1
        }
    }
}
