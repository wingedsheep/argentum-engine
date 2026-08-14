package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Slith Predator — Mirrodin #129
 * {G}{G} · Creature — Slith · 1/1
 *
 * Trample
 * Whenever this creature deals combat damage to a player, put a +1/+1 counter on it.
 *
 * The green member of the Slith cycle. Same growth trigger as [SlithFirewalker]; trample is what
 * keeps it growing once the board fills up, since a chump blocker no longer stops the damage from
 * reaching the player and so no longer stops the counter.
 */
val SlithPredator = card("Slith Predator") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Slith"
    power = 1
    toughness = 1
    oracleText = "Trample\n" +
        "Whenever this creature deals combat damage to a player, put a +1/+1 counter on it."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever this creature deals combat damage to a player, put a +1/+1 counter on it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "129"
        artist = "Justin Sweet"
        flavorText = "Born amid the molten metal of the Great Furnace, the slith have more than " +
            "adapted to the perils of a metal world."
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14bae45c-eaa8-4c6d-8042-df5cf26e294f.jpg?1783944532"
    }
}
