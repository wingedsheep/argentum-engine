package com.wingedsheep.mtg.sets.definitions.arb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Behemoth Sledge
 * {1}{G}{W}
 * Artifact — Equipment
 * Equipped creature gets +2/+2 and has trample and lifelink.
 * Equip {3}
 *
 * The standard Equipment shape: a [ModifyStats] static for the +2/+2 and one [GrantKeyword] static
 * per granted keyword — the two keywords stay separate abilities rather than one composite, which is
 * how the corpus (Loxodon Warhammer) and Argentum Assay both read the printed "has trample and
 * lifelink". `equipAbility("{3}")` lowers the printed Equip line into the sorcery-speed activated
 * ability targeting a creature you control.
 */
val BehemothSledge = card("Behemoth Sledge") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "WG"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+2 and has trample and lifelink.\n" +
        "Equip {3}"

    staticAbility {
        ability = ModifyStats(2, 2)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.LIFELINK)
    }
    equipAbility("{3}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Steve Prescott"
        flavorText = "Those who worship the great gargantuans could hardly be expected to fight with a subtle weapon."
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84e4a224-2db7-47d0-ba8b-98aeb50ad5d3.jpg"
        ruling("2009-10-01", "Multiple instances of lifelink on the same creature are redundant. If a creature with multiple instances of lifelink deals damage, its controller still only gains life equal to the damage dealt.")
    }
}
