package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Nebelgast Intruder (Innistrad: Midnight Hunt #64)
 * {2}{U}
 * Creature — Spirit
 * 2/1
 * Flash
 * Flying
 * When this creature enters, up to one target creature an opponent controls gets -2/-0 until end of turn.
 */
val NebelgastIntruder = card("Nebelgast Intruder") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    oracleText = "Flash\n" +
        "Flying\n" +
        "When this creature enters, up to one target creature an opponent controls gets -2/-0 until end of turn."
    power = 2
    toughness = 1

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter(GameObjectFilter.Creature.opponentControls()), optional = true)
        effect = Effects.ModifyStats(-2, 0, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c801346-0052-4f4b-a497-1567a34679fb.jpg"
    }
}
