package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Yellowjacket, Heartless Marauder
 * {1}{B}
 * Legendary Creature — Human Rogue Villain
 * 1/2
 * Flying
 * Whenever another Villain you control enters, Yellowjacket gets +1/+0 and gains lifelink until
 * end of turn.
 *
 * The trigger watches any Villain *permanent* (Villain shows up on artifact creatures such as the
 * Doombot token too), with [TriggerBinding.OTHER] so Yellowjacket's own arrival doesn't fire it.
 */
val YellowjacketHeartlessMarauder = card("Yellowjacket, Heartless Marauder") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Rogue Villain"
    oracleText = "Flying\nWhenever another Villain you control enters, Yellowjacket gets +1/+0 and gains lifelink until end of turn."
    power = 1
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Permanent.withSubtype(Subtype.VILLAIN).youControl(),
            binding = TriggerBinding.OTHER
        )
        effect = Effects.Composite(
            Effects.ModifyStats(1, 0, EffectTarget.Self),
            Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Alexander Skripnikov"
        flavorText = "\"They won't see me, but they'll certainly feel my sting.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8f91212-2ff5-4024-8f14-86bb5c4a9754.jpg?1783902936"
    }
}
