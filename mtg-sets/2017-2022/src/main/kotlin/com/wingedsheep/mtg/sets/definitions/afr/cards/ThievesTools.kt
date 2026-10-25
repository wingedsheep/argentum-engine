package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedWhilePropertyAtMost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Thieves' Tools
 * {1}{B}
 * Artifact — Equipment
 * When this Equipment enters, create a Treasure token.
 * Equipped creature can't be blocked as long as its power is 3 or less.
 * Equip {2}
 *
 * The evasion is gated on the equipped creature's *current* power, which is settled in Layer 7,
 * so it is [CantBeBlockedWhilePropertyAtMost] scoped to the attached creature (resolved in the
 * projector's post-layer pass) rather than a conditional `CantBeBlocked` grant, which would read
 * printed power and never switch off.
 */
val ThievesTools = card("Thieves' Tools") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")\n" +
        "Equipped creature can't be blocked as long as its power is 3 or less.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateTreasure(1)
    }

    staticAbility {
        ability = CantBeBlockedWhilePropertyAtMost(
            maxValue = 3,
            properties = setOf(EntityNumericProperty.Power),
            filter = GroupFilter.attachedCreature()
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "122"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f50b922e-32dd-4645-b6f4-c07d1a0c70fc.jpg?1783926487"
    }
}
