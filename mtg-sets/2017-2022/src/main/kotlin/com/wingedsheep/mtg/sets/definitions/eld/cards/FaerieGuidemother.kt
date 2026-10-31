package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Faerie Guidemother // Gift of the Fae
 * {W}
 * Creature — Faerie
 * 1/1
 *
 * Flying
 *
 * Adventure: Gift of the Fae — {1}{W}, Sorcery — Adventure
 * Target creature gets +2/+1 and gains flying until end of turn.
 */
val FaerieGuidemother = card("Faerie Guidemother") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Faerie"
    power = 1
    toughness = 1
    oracleText = "Flying"

    keywords(Keyword.FLYING)

    adventure("Gift of the Fae") {
        manaCost = "{1}{W}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Target creature gets +2/+1 and gains flying until end of turn. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(2, 1, t) then
                Effects.GrantKeyword(Keyword.FLYING, t)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "Mila Pesic"
        flavorText = "No one is so lost that a faerie can't find them."
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8bbece8-9620-44d9-b991-350fe952538a.jpg?1783932674"
    }
}
