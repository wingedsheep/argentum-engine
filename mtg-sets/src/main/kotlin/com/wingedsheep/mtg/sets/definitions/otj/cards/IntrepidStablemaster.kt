package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Intrepid Stablemaster
 * {1}{G}
 * Creature — Human Scout
 * 2/2
 * Reach
 * {T}: Add {G}.
 * {T}: Add two mana of any one color. Spend this mana only to cast Mount or Vehicle spells.
 */
val IntrepidStablemaster = card("Intrepid Stablemaster") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Scout"
    power = 2
    toughness = 2
    oracleText = "Reach\n{T}: Add {G}.\n{T}: Add two mana of any one color. Spend this mana only to cast Mount or Vehicle spells."

    keywords(Keyword.REACH)

    // {T}: Add {G}.
    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    // {T}: Add two mana of any one color. Spend this mana only to cast Mount or Vehicle spells.
    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddAnyColorMana(
            amount = 2,
            restriction = ManaRestriction.SubtypeSpellsOnly(setOf("Mount", "Vehicle"))
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "169"
        artist = "Svetlin Velinov"
        flavorText = "\"Never get too cocky. Remember you stay upright only by your mount's good graces.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d6cdf2a-026a-41ba-87d9-8fcd8a67f06e.jpg?1712355945"
    }
}
