package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Kenrith's Transformation
 * {1}{G}
 * Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, draw a card.
 * Enchanted creature loses all abilities and is a green Elk creature with base power
 * and toughness 3/3. (It loses all other card types and creature types.)
 */
val KenrithsTransformation = card("Kenrith's Transformation") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nWhen this Aura enters, draw a card.\nEnchanted creature loses all abilities and is a green Elk creature with base power and toughness 3/3. (It loses all other card types and creature types.)"

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    // "loses all abilities" — Layer 6
    staticAbility {
        ability = LoseAllAbilities()
    }

    // "is a green Elk creature" — Layer 4 (card types + subtypes) + Layer 5 (color)
    staticAbility {
        ability = TransformPermanent(
            setCardTypes = setOf("CREATURE"),
            setSubtypes = setOf("Elk"),
            setColors = setOf(Color.GREEN)
        )
    }

    // "base power and toughness 3/3" — Layer 7b
    staticAbility {
        ability = SetBasePowerToughnessStatic(3, 3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "164"
        artist = "Kimonas Theodossiou"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6da7cd39-1f8a-4f68-adb7-df2beac02263.jpg?1783932607"
        ruling("2019-10-04", "If the creature Kenrith's Transformation would enchant is an illegal target by the time the Aura spell resolves, the entire spell doesn't resolve. It won't enter the battlefield, so its ability won't trigger.")
        ruling("2019-10-04", "Kenrith's Transformation overwrites all colors and creature types the enchanted creature has. It's just a green Elk. The creature keeps any supertypes (such as legendary) it has, but loses any other card types it has (such as artifact).")
        ruling("2019-10-04", "Kenrith's Transformation may enchant a permanent that is only temporarily a creature, such as Enchanted Carriage. If this happens, the Aura's effect causes the enchanted permanent to remain a green Elk creature even after the temporary effect expires.")
    }
}
