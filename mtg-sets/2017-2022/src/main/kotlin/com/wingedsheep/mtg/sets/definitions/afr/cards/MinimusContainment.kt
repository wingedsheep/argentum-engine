package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Minimus Containment
 * {2}{W}
 * Enchantment — Aura
 *
 * Enchant nonland permanent
 * Enchanted permanent is a Treasure artifact with "{T}, Sacrifice this artifact: Add one mana of
 * any color," and it loses all other abilities. (If it was a creature, it's no longer a creature.)
 *
 * The Sugar Coat / Imprisoned in the Moon shape — a stack of statics on the enchanted permanent:
 *  - [TransformPermanent] (Layer 4): "is a Treasure artifact" sets the card type to ARTIFACT and
 *    the subtypes to Treasure, replacing the old ones (a creature stops being a creature, an
 *    Equipment stops being an Equipment). Supertypes are untouched (CR 205.4b), and colors are
 *    left alone (`setColors = null`) — per the ruling, it keeps its name, colors and mana cost.
 *  - [LoseAllAbilities] (Layer 6) strips every other ability.
 *  - [GrantActivatedAbility] (Layer 6) grants the Treasure mana ability.
 */
val MinimusContainment = card("Minimus Containment") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant nonland permanent\nEnchanted permanent is a Treasure artifact with " +
        "\"{T}, Sacrifice this artifact: Add one mana of any color,\" and it loses all other " +
        "abilities. (If it was a creature, it's no longer a creature.)"

    // Enchant nonland permanent
    auraTarget = TargetObject(filter = TargetFilter.NonlandPermanent)

    // "is a Treasure artifact" — Layer 4; colors unchanged.
    staticAbility {
        ability = TransformPermanent(
            setCardTypes = setOf("ARTIFACT"),
            setSubtypes = setOf("Treasure")
        )
    }

    // "it loses all other abilities" — Layer 6
    staticAbility {
        ability = LoseAllAbilities()
    }

    // "with '{T}, Sacrifice this artifact: Add one mana of any color'" — Layer 6
    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf),
                effect = Effects.AddAnyColorMana(1),
                isManaAbility = true,
                descriptionOverride = "{T}, Sacrifice this artifact: Add one mana of any color."
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/0556f0d9-50d2-4c67-8522-de366d96500a.jpg?1783926530"

        ruling(
            "2021-07-23",
            "A creature enchanted with Minimus Containment stops being a creature. Equipment " +
                "attached to that creature become unattached. Auras attached to that creature " +
                "will be put into their owner's graveyard (unless they could also enchant a " +
                "Treasure artifact)."
        )
        ruling(
            "2021-07-23",
            "The enchanted permanent still retains its name, colors, mana cost, and mana value. " +
                "It isn't a token unless it already was one."
        )
        ruling(
            "2021-07-23",
            "If an effect refers to a Treasure, it means any Treasure artifact, not just a " +
                "Treasure artifact token."
        )
    }
}
