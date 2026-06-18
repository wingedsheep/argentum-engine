package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Balemurk Leech
 * {1}{B}
 * Creature — Leech
 * 2/2
 *
 * Eerie — Whenever an enchantment you control enters and whenever you fully unlock a
 * Room, each opponent loses 1 life.
 */
val BalemurkLeech = card("Balemurk Leech") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Leech"
    power = 2
    toughness = 2
    oracleText = "Eerie — Whenever an enchantment you control enters and whenever you fully unlock a Room, each opponent loses 1 life."

    keywords(Keyword.EERIE)

    // Eerie trigger — part 1: whenever an enchantment you control enters
    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Enchantment.youControl(),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Eerie — Whenever an enchantment you control enters, each opponent loses 1 life."
    }

    // Eerie trigger — part 2: whenever you fully unlock a Room
    triggeredAbility {
        trigger = Triggers.RoomFullyUnlocked
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Eerie — Whenever you fully unlock a Room, each opponent loses 1 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "John Tedrick"
        flavorText = "Adapted to feed on dead flesh, it's not averse to making its own when no corpses are available."
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0621b32-95c5-4f70-96cf-d46d20efc85d.jpg?1726286164"
    }
}
