package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Grazing Gladehart
 * {2}{G}
 * Creature — Antelope
 * 2/2
 * Landfall — Whenever a land you control enters, you may gain 2 life.
 *
 * Landfall is [Triggers.LandYouControlEnters] — the `ZoneChangeEvent` over
 * `GameObjectFilter.Land.youControl()` with [com.wingedsheep.sdk.scripting.TriggerBinding.ANY].
 * The printed "you may" is the builder's `optional = true`, which lowers to a
 * [com.wingedsheep.sdk.scripting.effects.Gate.MayDecide] gate around [Effects.GainLife], so the
 * consent lives on the gate rather than on a flag beside it.
 */
val GrazingGladehart = card("Grazing Gladehart") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Antelope"
    power = 2
    toughness = 2
    oracleText = "Landfall — Whenever a land you control enters, you may gain 2 life."

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        optional = true
        effect = Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "163"
        artist = "Ryan Pancoast"
        flavorText = "\"Don't be fooled. If it were as docile as it looks, it would've died off long ago.\"\n—Yon Basrel, Oran-Rief survivalist"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/078b5290-a613-496f-bd23-8fd109549f31.jpg"
        ruling("2024-11-08", "A landfall ability triggers whenever a land you control enters for any reason. It triggers whenever you play a land, as well as whenever a spell or ability puts a land onto the battlefield under your control.")
        ruling("2024-11-08", "A landfall ability doesn't trigger if a permanent already on the battlefield becomes a land.")
        ruling("2024-11-08", "Whenever a land you control enters, each landfall ability of the permanents you control will trigger. You can put them   on the stack in any order. The last ability you put on the stack will be the first one to resolve (As a result, you can have those abilities resolve in the order of your choosing.).")
    }
}
