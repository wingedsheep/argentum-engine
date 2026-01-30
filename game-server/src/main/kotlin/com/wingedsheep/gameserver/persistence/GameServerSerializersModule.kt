package com.wingedsheep.gameserver.persistence

import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.gameserver.dto.ClientEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Serializers module for game server persistence types.
 * Includes the engine serializers module plus game-server specific types.
 */
val gameServerSerializersModule = SerializersModule {
    include(engineSerializersModule)

    // ClientEvent hierarchy for game log persistence
    polymorphic(ClientEvent::class) {
        subclass(ClientEvent.LifeChanged::class)
        subclass(ClientEvent.DamageDealt::class)
        subclass(ClientEvent.StatsModified::class)
        subclass(ClientEvent.CardDrawn::class)
        subclass(ClientEvent.CardDiscarded::class)
        subclass(ClientEvent.PermanentEntered::class)
        subclass(ClientEvent.PermanentLeft::class)
        subclass(ClientEvent.CreatureAttacked::class)
        subclass(ClientEvent.CreatureBlocked::class)
        subclass(ClientEvent.CreatureDied::class)
        subclass(ClientEvent.SpellCast::class)
        subclass(ClientEvent.SpellResolved::class)
        subclass(ClientEvent.SpellCountered::class)
        subclass(ClientEvent.AbilityTriggered::class)
        subclass(ClientEvent.AbilityActivated::class)
        subclass(ClientEvent.PermanentTapped::class)
        subclass(ClientEvent.PermanentUntapped::class)
        subclass(ClientEvent.CounterAdded::class)
        subclass(ClientEvent.CounterRemoved::class)
        subclass(ClientEvent.ManaAdded::class)
        subclass(ClientEvent.PlayerLost::class)
        subclass(ClientEvent.GameEnded::class)
        subclass(ClientEvent.HandLookedAt::class)
        subclass(ClientEvent.HandRevealed::class)
        subclass(ClientEvent.CardsRevealed::class)
        subclass(ClientEvent.ScryCompleted::class)
        subclass(ClientEvent.PermanentsSacrificed::class)
        subclass(ClientEvent.LibraryReordered::class)
    }
}

/**
 * JSON instance configured for persistence serialization.
 */
val persistenceJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    classDiscriminator = "type"
    allowStructuredMapKeys = true
    serializersModule = gameServerSerializersModule
}
