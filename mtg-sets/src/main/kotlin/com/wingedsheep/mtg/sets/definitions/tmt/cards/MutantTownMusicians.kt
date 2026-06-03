package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mutant Town Musicians
 * {2}{R}
 * Creature — Mutant Bard Performer
 * 2/4
 *
 * Trample
 * Alliance — Whenever another creature you control enters, this
 * creature gets +1/+0 until end of turn.
 */
val MutantTownMusicians = card("Mutant Town Musicians") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Mutant Bard Performer"
    oracleText = "Trample\nAlliance — Whenever another creature you control enters, this creature gets +1/+0 until end of turn."
    power = 2
    toughness = 4

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.OtherCreatureEnters
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
        description = "Alliance — Whenever another creature you control enters, this creature gets +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "97"
        artist = "Leonardo Vincent (Levinky)"
        flavorText = "After the Bomb's take on thrash metal isn't for everyone . . . and Sheena wouldn't have it any other way."
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2dbe9207-06e8-4837-8227-0f960490771b.jpg?1771766034"
    }
}
