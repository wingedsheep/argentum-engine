package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Slaughter Singer
 * {G}{W}
 * Creature — Phyrexian Cleric
 * 2/2
 * Toxic 2
 * Whenever another creature you control with toxic attacks, it gets +1/+1 until end of turn.
 *
 * "it" is the attacking creature, bound as [EffectTarget.TriggeringEntity].
 */
val SlaughterSinger = card("Slaughter Singer") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Phyrexian Cleric"
    power = 2
    toughness = 2
    oracleText = "Toxic 2 (Players dealt combat damage by this creature also get two poison counters.)\n" +
        "Whenever another creature you control with toxic attacks, it gets +1/+1 until end of turn."

    keywordAbility(KeywordAbility.toxic(2))

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl().withKeyword(Keyword.TOXIC)).attacks()
        effect = Effects.ModifyStats(1, 1, EffectTarget.TriggeringEntity)
        description = "Whenever another creature you control with toxic attacks, it gets +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "216"
        artist = "Adam Burn"
        flavorText = "After giving in to the thrill of the hunt, he could never again return to the quiet of the Basilica."
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a37aa46-bcf3-48a5-9f74-05e4878ad96f.jpg?1783917996"
    }
}
