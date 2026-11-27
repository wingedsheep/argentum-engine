package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Wildwood Geist — Avacyn Restored #204 (canonical printing)
 * {4}{G} · Creature — Spirit · 3/3
 *
 * During your turn, this creature gets +2/+2.
 *
 * [HardyVeteran] at +2/+2: [ModifyStats] on self gated by `Conditions.IsYourTurn`.
 */
val WildwoodGeist = card("Wildwood Geist") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spirit"
    oracleText = "During your turn, this creature gets +2/+2."
    power = 3
    toughness = 3

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(2, 2, Filters.Self),
            condition = Conditions.IsYourTurn,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "204"
        artist = "Lars Grant-West"
        flavorText = "The geists that dwell in the deep forests of Kessig are as untamable as the woods themselves."
        imageUri = "https://cards.scryfall.io/normal/front/4/6/4658b4b2-7043-4ca2-96fd-4f663c20c80f.jpg?1783940657"
    }
}
