package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Wolfwillow Haven
 * {1}{G}
 * Enchantment — Aura
 * Enchant land
 * Whenever enchanted land is tapped for mana, its controller adds an additional {G}.
 * {4}{G}, Sacrifice this Aura: Create a 2/2 green Wolf creature token. Activate only during your turn.
 */
val WolfwillowHaven = card("Wolfwillow Haven") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "Whenever enchanted land is tapped for mana, its controller adds an additional {G}.\n" +
        "{4}{G}, Sacrifice this Aura: Create a 2/2 green Wolf creature token. Activate only during your turn."

    auraTarget = TargetObject(filter = TargetFilter.Land)

    staticAbility {
        ability = AdditionalManaOnTap(color = Color.GREEN, amount = DynamicAmounts.fixed(1))
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{G}"), Costs.SacrificeSelf)
        restrictions = listOf(ActivationRestriction.OnlyDuringYourTurn)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf"),
            imageUri = "https://cards.scryfall.io/normal/front/4/1/411f4bf6-7f09-4e24-b483-0068d2f974e5.jpg?1783931451"
        )
        description = "{4}{G}, Sacrifice this Aura: Create a 2/2 green Wolf creature token. Activate only during your turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Jakub Kasper"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72b886c3-234c-49ce-9a11-456c1e8f092f.jpg?1783931526"
    }
}
