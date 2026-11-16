package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sheltered Aerie
 * {2}{G}
 * Enchantment — Aura
 * Enchant land
 * Enchanted land has "{T}: Add two mana of any one color."
 *
 * The granted ability is a separate mana ability on the land (as Weirding Wood), not an
 * addition to the land's own tap-for-mana, so it is a [GrantActivatedAbility].
 */
val ShelteredAerie = card("Sheltered Aerie") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "Enchanted land has \"{T}: Add two mana of any one color.\""

    auraTarget = TargetObject(filter = TargetFilter.Land)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddAnyColorMana(2),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "206"
        artist = "Raoul Vitale"
        flavorText = "Dromoka's scalelords patrol the skies over Arashin, offering her people safety from the harsh world."
        imageUri = "https://cards.scryfall.io/normal/front/a/f/afecf5a8-3c9e-48e0-8818-e2e3183e958c.jpg?1783938576"
    }
}
