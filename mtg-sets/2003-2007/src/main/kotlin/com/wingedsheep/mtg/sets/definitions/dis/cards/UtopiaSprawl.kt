package com.wingedsheep.mtg.sets.definitions.dis.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Utopia Sprawl — Dissension #99
 * {G} · Enchantment — Aura
 *
 * Enchant Forest
 * As this Aura enters, choose a color.
 * Whenever enchanted Forest is tapped for mana, its controller adds an additional one mana of the
 * chosen color.
 *
 * The Shimmerwilds Growth shape without the recolour: the Forest keeps making its own {G}, and
 * `AdditionalManaOnTap(color = null)` reads the color stored on the Aura by [EntersWithChoice].
 */
val UtopiaSprawl = card("Utopia Sprawl") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant Forest\nAs this Aura enters, choose a color.\nWhenever enchanted Forest is tapped for mana, its controller adds an additional one mana of the chosen color."

    auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.Land.withSubtype(Subtype.FOREST)))

    replacementEffect(EntersWithChoice(ChoiceType.COLOR))

    staticAbility {
        ability = AdditionalManaOnTap(
            color = null,
            amount = DynamicAmounts.fixed(1),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "99"
        artist = "Ron Spears"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/5047e271-fbf1-402c-9eb9-0806e5988f76.jpg?1792506540"
    }
}
