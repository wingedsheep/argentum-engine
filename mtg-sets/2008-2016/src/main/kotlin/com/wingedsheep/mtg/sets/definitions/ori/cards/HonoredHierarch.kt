package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Honored Hierarch
 * {G}
 * Creature — Human Druid
 * 1/1
 *
 * Renown 1 (When this creature deals combat damage to a player, if it isn't renowned, put a +1/+1 counter on it and it becomes renowned.)
 * As long as this creature is renowned, it has vigilance and "{T}: Add one mana of any color."
 *
 * The mana ability is *granted* while renowned (Fledgling Dragon's conditional self-grant), not a
 * printed ability with an activation restriction — before renown the creature has no such ability.
 */
val HonoredHierarch = card("Honored Hierarch") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Druid"
    oracleText = "Renown 1 (When this creature deals combat damage to a player, if it isn't renowned, put a +1/+1 counter on it and it becomes renowned.)\n" +
        "As long as this creature is renowned, it has vigilance and \"{T}: Add one mana of any color.\""
    power = 1
    toughness = 1

    keywordAbility(KeywordAbility.renown(1))

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.source())
        condition = Conditions.SourceIsRenowned
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                effect = Effects.AddAnyColorMana(1)
                manaAbility = true
            },
            filter = GroupFilter.source(),
        )
        condition = Conditions.SourceIsRenowned
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "182"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/769d0447-2c8f-41ef-a950-3a3022161c7f.jpg?1783938322"
    }
}
