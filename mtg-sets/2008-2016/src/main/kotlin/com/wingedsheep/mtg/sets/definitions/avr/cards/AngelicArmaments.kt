package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantColor
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Angelic Armaments
 * {3}
 * Artifact — Equipment
 * Equipped creature gets +2/+2, has flying, and is a white Angel in addition to its other colors and types.
 * Equip {4}
 */
val AngelicArmaments = card("Angelic Armaments") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+2, has flying, and is a white Angel in addition to its other colors and types.\n" +
        "Equip {4} ({4}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility { ability = ModifyStats(2, 2, Filters.EquippedCreature) }
    staticAbility { ability = GrantKeyword(Keyword.FLYING, Filters.EquippedCreature) }
    staticAbility { ability = GrantColor(Color.WHITE, Filters.EquippedCreature) }
    staticAbility { ability = GrantSubtype(Subtype.ANGEL.value, Filters.EquippedCreature) }

    equipAbility("{4}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "212"
        artist = "Daniel Ljunggren"
        flavorText = "Forged in dark hours from the flame that would not die."
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3fa99b48-469d-4112-bdfd-2391fa439514.jpg?1783940654"
    }
}
