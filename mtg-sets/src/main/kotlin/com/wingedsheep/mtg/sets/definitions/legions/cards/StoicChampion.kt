package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stoic Champion
 * {W}{W}
 * Creature — Human Soldier
 * 2/2
 * Whenever a player cycles a card, Stoic Champion gets +2/+2 until end of turn.
 */
val StoicChampion = card("Stoic Champion") {
    manaCost = "{W}{W}"
    typeLine = "Creature — Human Soldier"
    oracleText = "Whenever a player cycles a card, this creature gets +2/+2 until end of turn."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.AnyPlayerCycles
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Greg Hildebrandt"
        flavorText = "His outer calm belies his inner fury."
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b69d619-c31b-472b-9ae8-d4503704680d.jpg?1562916585"
    }
}
