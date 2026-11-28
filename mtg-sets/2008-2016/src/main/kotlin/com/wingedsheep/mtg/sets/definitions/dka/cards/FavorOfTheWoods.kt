package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Favor of the Woods
 * {2}{G}
 * Enchantment — Aura
 *
 * Enchant creature
 * Whenever enchanted creature blocks, you gain 3 life.
 *
 * The life goes to the Aura's controller, even if they don't control the enchanted creature.
 */
val FavorOfTheWoods = card("Favor of the Woods") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nWhenever enchanted creature blocks, you gain 3 life."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.blocks()
        effect = Effects.GainLife(3)
        description = "Whenever enchanted creature blocks, you gain 3 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Tomasz Jedruszek"
        flavorText = "Feeling abandoned by Avacyn, some Kessigers turned to the pagan rituals of their ancestors."
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da0e760a-241b-40c5-b201-bfc03effed2e.jpg?1783940810"
        ruling(
            "2011-01-22",
            "The ability will trigger only once per combat, even if the enchanted creature somehow blocks multiple attacking creatures."
        )
    }
}
