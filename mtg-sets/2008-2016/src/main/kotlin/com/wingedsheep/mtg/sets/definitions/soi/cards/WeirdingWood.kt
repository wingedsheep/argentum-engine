package com.wingedsheep.mtg.sets.definitions.soi.cards

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
 * Weirding Wood
 * {2}{G}
 * Enchantment — Aura
 * Enchant land
 * When this Aura enters, investigate.
 * Enchanted land has "{T}: Add two mana of any one color."
 *
 * The granted ability is a separate mana ability on the land, not an addition to its own
 * tap-for-mana, so it is a [GrantActivatedAbility] (as New Horizons) rather than the
 * `AdditionalManaOnTap` (Fertile Ground) shape.
 */
val WeirdingWood = card("Weirding Wood") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "When this Aura enters, investigate. (Create a Clue token. It's an artifact with " +
        "\"{2}, Sacrifice this token: Draw a card.\")\n" +
        "Enchanted land has \"{T}: Add two mana of any one color.\""

    auraTarget = TargetObject(filter = TargetFilter.Land)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Investigate()
    }

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
        rarity = Rarity.UNCOMMON
        collectorNumber = "240"
        artist = "Jung Park"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8033b16-7a48-4455-9b1b-f62ceb7b9b61.jpg?1783937715"
    }
}
