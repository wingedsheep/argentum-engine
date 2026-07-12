package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Reclusive Taxidermist
 * {1}{G}
 * Creature — Human Druid
 * 1/2
 *
 * This creature gets +3/+2 as long as there are four or more creature cards in your graveyard.
 * {T}: Add one mana of any color.
 *
 * The buff is a [ConditionalStaticAbility] over [ModifyStats] on the source, gated by
 * [Conditions.CreatureCardsInGraveyardAtLeast] (Basking Capybara idiom). The mana ability is a
 * {T}: any-color mana ability.
 */
val ReclusiveTaxidermist = card("Reclusive Taxidermist") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Druid"
    power = 1
    toughness = 2
    oracleText = "This creature gets +3/+2 as long as there are four or more creature cards in " +
        "your graveyard.\n" +
        "{T}: Add one mana of any color."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 3, toughnessBonus = 2, filter = GroupFilter.source()),
            condition = Conditions.CreatureCardsInGraveyardAtLeast(4)
        )
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add one mana of any color."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "214"
        artist = "Wisnu Tan"
        flavorText = "All druids seek to preserve nature, but some go about it in rather unusual ways."
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10edf37a-ee35-491d-b83a-39035f7df65a.jpg?1782703043"
    }
}
