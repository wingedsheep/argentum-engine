package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Urban Utopia
 * {1}{G}
 * Enchantment — Aura
 * Enchant land
 * When this Aura enters, draw a card.
 * Enchanted land has "{T}: Add one mana of any color."
 */
val UrbanUtopia = card("Urban Utopia") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "When this Aura enters, draw a card.\n" +
        "Enchanted land has \"{T}: Add one mana of any color.\""

    auraTarget = TargetObject(filter = TargetFilter.Land)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddAnyColorMana(1),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "146"
        artist = "Jedd Chevrier"
        flavorText = "Ravnica's diversity produces blossoms of opinion in a rich soil of perspectives."
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45d7ab0e-db37-4051-86dd-3ab09c047a2b.jpg?1783934146"
    }
}
