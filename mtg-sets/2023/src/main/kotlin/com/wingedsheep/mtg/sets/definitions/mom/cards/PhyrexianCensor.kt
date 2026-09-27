package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PermanentsEnterTapped
import com.wingedsheep.sdk.scripting.RestrictSpellsCastPerTurn

/**
 * Phyrexian Censor
 * {2}{W}
 * Creature — Phyrexian Wizard
 * 3/3
 *
 * Each player can't cast more than one non-Phyrexian spell each turn.
 * Non-Phyrexian creatures enter tapped.
 *
 * The first ability is the global, filtered form of [RestrictSpellsCastPerTurn]: only
 * non-Phyrexian spells count toward the cap and only they are blocked by it, so Phyrexian spells
 * stay castable. The second is a global [PermanentsEnterTapped] over every non-Phyrexian creature.
 */
val PhyrexianCensor = card("Phyrexian Censor") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Wizard"
    power = 3
    toughness = 3
    oracleText = "Each player can't cast more than one non-Phyrexian spell each turn.\n" +
        "Non-Phyrexian creatures enter tapped."

    staticAbility {
        ability = RestrictSpellsCastPerTurn(
            maxPerTurn = 1,
            eachPlayer = true,
            spellFilter = GameObjectFilter.Any.notSubtype(Subtype.PHYREXIAN),
        )
    }

    replacementEffect(
        PermanentsEnterTapped(
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.notSubtype(Subtype.PHYREXIAN),
                to = Zone.BATTLEFIELD,
            )
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "31"
        artist = "Alexey Kruglov"
        flavorText = "Quintorius muffled a sob as he watched the thing that was once Professor Pitnik " +
            "\"confiscate\" yet another priceless historical tome."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/150e17b1-b9fd-4ec4-b305-19596fed14d1.jpg?1783917052"
        ruling(
            "2023-04-14",
            "A \"non-Phyrexian spell\" is any spell that doesn't have the Phyrexian creature type, even " +
                "if \"Phyrexian\" appears in its name. Artifact spells, battle spells, and Goblin creature " +
                "spells are examples of non-Phyrexian spells."
        )
        ruling(
            "2023-04-14",
            "Phyrexian Censor looks at the entire turn to see if a player has cast a non-Phyrexian spell, " +
                "even if Phyrexian Censor wasn't on the battlefield when that spell was cast. Phyrexian " +
                "Censor is a Phyrexian spell, so you could cast it and then a non-Phyrexian spell in the same turn."
        )
        ruling(
            "2023-04-14",
            "If a player casts a non-Phyrexian spell that was countered, they can't cast another " +
                "non-Phyrexian spell during the same turn."
        )
    }
}
