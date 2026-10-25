package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Compelled Duel
 * {1}{G}
 * Sorcery
 * Target creature gets +3/+3 until end of turn and must be blocked this turn if able.
 */
val CompelledDuel = card("Compelled Duel") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target creature gets +3/+3 until end of turn and must be blocked this turn if able."
    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(3, 3, t) then
            Effects.MustBeBlocked(t, allCreatures = false)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "178"
        artist = "Zezhou Chen"
        flavorText = "\"You might be four times my size, but I will not be ignored!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a7e501d-943e-4e6d-9d8e-6bedc32a0dd8.jpg?1783926465"
        ruling("2021-07-23", "Only one creature is required to block the affected creature. Other creatures may also block it and are free to block other creatures or not block at all.")
        ruling("2021-07-23", "If each creature the defending player controls can't block for any reason (such as being tapped), then the affected creature isn't blocked. If there's a cost associated with blocking the affected creature, the defending player isn't forced to pay that cost, so it doesn't have to be blocked in that case either.")
    }
}
