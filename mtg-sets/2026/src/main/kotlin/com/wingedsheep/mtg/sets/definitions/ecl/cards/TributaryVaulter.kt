package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val TributaryVaulter = card("Tributary Vaulter") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Merfolk Warrior"
    power = 1
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever this creature becomes tapped, another target Merfolk you control gets +2/+0 until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        val merfolk = target(TargetFilter(GameObjectFilter.Permanent.youControl().withSubtype("Merfolk")).other())
        effect = Effects.ModifyStats(2, 0, merfolk)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "40"
        artist = "Tiffany Turrill"
        flavorText = "\"Other species may spread across the land all they like, " +
            "but the riverways and the skies above them will always be ours.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1db2c83-41d1-4cc6-b9a3-fed504b01127.jpg?1767871741"
    }
}
