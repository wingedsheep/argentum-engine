package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Ferocity of the Wilds — Throne of Eldraine #123
 * {2}{R} · Enchantment
 *
 * Attacking non-Human creatures you control get +1/+0 and have trample.
 *
 * Two continuous statics over the same group — attacking non-Human creatures you control — read
 * from projected state, so a creature gains the bonus as it's declared as an attacker and loses it
 * when it leaves combat or becomes a Human.
 */
val FerocityOfTheWilds = card("Ferocity of the Wilds") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Attacking non-Human creatures you control get +1/+0 and have trample."

    val attackingNonHumans = GroupFilter(GameObjectFilter.Creature.notSubtype(Subtype.HUMAN).youControl()).attacking()

    staticAbility {
        ability = ModifyStats(1, 0, attackingNonHumans)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, attackingNonHumans)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Josu Hernaiz"
        flavorText = "Knights who excel in tournaments sometimes underestimate threats beyond the realm."
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f7005fd-5917-4f7d-9a7d-7ccc044d0f87.jpg?1783932625"
    }
}
