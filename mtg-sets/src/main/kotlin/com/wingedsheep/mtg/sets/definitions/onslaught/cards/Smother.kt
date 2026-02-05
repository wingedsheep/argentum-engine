package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeRegeneratedEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Smother
 * {1}{B}
 * Instant
 * Destroy target creature with mana value 3 or less. It can't be regenerated.
 */
val Smother = card("Smother") {
    manaCost = "{1}{B}"
    typeLine = "Instant"

    spell {
        target = TargetCreature(filter = TargetFilter.Creature.manaValueAtMost(3))
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.GRAVEYARD, byDestruction = true) then
                CantBeRegeneratedEffect(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "170"
        artist = "Carl Critchlow"
        flavorText = "\"I can't hear them scream, but at least I don't have to listen to them beg.\" —Phage the Untouchable"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a8321af-d667-44e7-8c03-3957286604b9.jpg?1562931422"
    }
}
