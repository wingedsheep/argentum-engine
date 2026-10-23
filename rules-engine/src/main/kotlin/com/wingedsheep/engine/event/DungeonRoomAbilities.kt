package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.VenturedIntoDungeonEvent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dungeons.Dungeons
import com.wingedsheep.sdk.model.DungeonDefinition
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Dungeon room abilities (CR 309.4c): every room has the triggered ability "When you move your venture
 * marker into this room, [effect]", its source is the dungeon card, and the dungeon's owner controls
 * it. A dungeon card's abilities trigger from the command zone (CR 309.4c), which is the only zone a
 * dungeon is ever in.
 *
 * Detected from [VenturedIntoDungeonEvent] the same way Saga chapters are detected from lore counters:
 * the room's [com.wingedsheep.sdk.model.DungeonRoom] is turned into a [TriggeredAbility] carrying its
 * effect and targets, and queued with the dungeon card as source. The pending trigger keeps the
 * dungeon from completing until the ability has left the stack (CR 309.6, 704.5t).
 */
object DungeonRoomAbilities {

    private const val ID_PREFIX = "dungeon_room:"

    /**
     * Whether [trigger] is a room ability. Read off the ability id, not the dungeon card entity: a
     * later venture in the same batch can complete the dungeon and remove its card before the
     * room ability is placed.
     */
    fun isRoomAbility(trigger: PendingTrigger): Boolean = trigger.ability.id.value.startsWith(ID_PREFIX)

    /** Queue the room ability for every move into a room in [events]. */
    fun detect(events: List<GameEvent>, triggers: MutableList<PendingTrigger>) {
        for (event in events) {
            if (event !is VenturedIntoDungeonEvent) continue
            val dungeon = Dungeons.byName(event.dungeonName) ?: continue
            triggers += PendingTrigger(
                ability = ability(dungeon, event.roomIndex),
                sourceId = event.dungeonEntityId,
                sourceName = event.dungeonName,
                controllerId = event.playerId,
                triggerContext = TriggerContext(triggeringPlayerId = event.playerId)
            )
        }
    }

    /**
     * Room [roomIndex] of [dungeon] as a triggered ability. The trigger pattern is never matched — the
     * ability is created directly by [detect] — so it only has to be a well-formed placeholder. The id
     * is stable per room, so every copy compares equal.
     */
    fun ability(dungeon: DungeonDefinition, roomIndex: Int): TriggeredAbility {
        val room = dungeon.rooms[roomIndex]
        return TriggeredAbility.create(
            id = AbilityId("$ID_PREFIX${dungeon.name}:$roomIndex"),
            trigger = EventPattern.StepEvent(Step.UPKEEP, Player.You),
            binding = TriggerBinding.SELF,
            effect = room.effect,
            targetRequirement = room.targetRequirements.firstOrNull(),
            additionalTargetRequirements = room.targetRequirements.drop(1),
            descriptionOverride = "${room.name} — ${room.text}"
        )
    }
}
