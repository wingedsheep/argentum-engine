package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Stalactite Dagger
 * {2}
 * Artifact — Equipment
 *
 * When this Equipment enters, create a 1/1 colorless Shapeshifter creature token with changeling.
 * Equipped creature gets +1/+1 and is all creature types.
 * Equip {2}
 */
val StalactiteDagger = card("Stalactite Dagger") {
    manaCost = "{2}"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, create a 1/1 colorless Shapeshifter creature token with changeling. (It's every creature type.)\n" +
        "Equipped creature gets +1/+1 and is all creature types.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Shapeshifter"),
            keywords = setOf(Keyword.CHANGELING),
            imageUri = "https://cards.scryfall.io/normal/front/c/2/c2963ce1-f9d8-437a-9489-e0913a8b8d26.jpg?1767660071"
        )
    }

    staticAbility {
        effect = Effects.ModifyStats(+1, +1)
        filter = Filters.EquippedCreature
    }

    staticAbility {
        effect = Effects.GrantKeyword(Keyword.CHANGELING)
        filter = Filters.EquippedCreature
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "261"
        artist = "Drew Tucker"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/6954df09-95f3-46cf-9ba8-2a1aea653d8f.jpg?1767957330"
    }
}
