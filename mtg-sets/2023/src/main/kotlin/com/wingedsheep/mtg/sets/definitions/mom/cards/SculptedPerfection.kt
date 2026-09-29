package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Sculpted Perfection — March of the Machine #253
 * {2}{W}{B} · Enchantment
 *
 * When this enchantment enters, incubate 2.
 * Phyrexians you control get +1/+1.
 */
val SculptedPerfection = card("Sculpted Perfection") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, incubate 2. (Create an Incubator token with two +1/+1 " +
        "counters on it and \"{2}: Transform this token.\" It transforms into a 0/0 Phyrexian artifact creature.)\n" +
        "Phyrexians you control get +1/+1."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Incubate(2)
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Permanent.youControl().withSubtype("Phyrexian"))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "253"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/594aefa2-7a5b-4ec1-841b-a6ef4f5aa2b1.jpg?1783916937"
    }
}
