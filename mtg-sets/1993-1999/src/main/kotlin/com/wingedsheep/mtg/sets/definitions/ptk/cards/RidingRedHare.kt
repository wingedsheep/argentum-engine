package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Riding Red Hare
 * {2}{W}
 * Sorcery
 * Target creature gets +3/+3 and gains horsemanship until end of turn.
 */
val RidingRedHare = card("Riding Red Hare") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Target creature gets +3/+3 and gains horsemanship until end of turn. (It can't be blocked except by creatures with horsemanship.)"

    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.Composite(
            Effects.ModifyStats(3, 3, t),
            Effects.GrantKeyword(Keyword.HORSEMANSHIP, t)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "18"
        artist = "Qi Baocheng"
        flavorText = "One of the greatest steeds in the empire, Red Hare could travel a thousand *li* a day and climb hills as if running on flat ground."
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0504a24-4b92-471a-8da4-02ec57eb43be.jpg"
    }
}
