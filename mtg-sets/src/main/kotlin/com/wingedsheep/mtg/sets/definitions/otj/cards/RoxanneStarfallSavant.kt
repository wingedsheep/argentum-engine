package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Roxanne, Starfall Savant
 * {3}{R}{G}
 * Legendary Creature — Cat Druid
 * 4/3
 *
 * Whenever Roxanne enters or attacks, create a tapped colorless artifact token named Meteorite
 * with "When this token enters, it deals 2 damage to any target" and "{T}: Add one mana of any color."
 * Whenever you tap an artifact token for mana, add one mana of any type that artifact token produced.
 *
 * "Enters or attacks" is two triggered abilities sharing one effect (CR 603.2 — the two events
 * trigger independently), each creating a tapped [Meteorite] predefined token (its ETB-deal-2 and
 * mana ability live on the token definition). The second ability is the
 * [AdditionalManaOnSourceTap] static with `color = null` (mirror the produced type) over
 * artifact tokens you control — the same shape as Lavaleaper's "add one mana of any type that land
 * produced", here scoped to artifact tokens.
 */
val RoxanneStarfallSavant = card("Roxanne, Starfall Savant") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Cat Druid"
    power = 4
    toughness = 3
    oracleText = "Whenever Roxanne enters or attacks, create a tapped colorless artifact token " +
        "named Meteorite with \"When this token enters, it deals 2 damage to any target\" and " +
        "\"{T}: Add one mana of any color.\"\n" +
        "Whenever you tap an artifact token for mana, add one mana of any type that artifact token produced."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateMeteorite(tapped = true)
        description = "Whenever Roxanne enters, create a tapped Meteorite token."
    }
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.CreateMeteorite(tapped = true)
        description = "Whenever Roxanne attacks, create a tapped Meteorite token."
    }

    staticAbility {
        ability = AdditionalManaOnSourceTap(
            sourceFilter = GameObjectFilter.Artifact.token().youControl(),
            color = null,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "228"
        artist = "Ina Wong"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11fbe52f-febd-49fc-8391-28d3efe9c3eb.jpg?1712356193"

        ruling("2024-04-12", "You're tapping an artifact token for mana only if you're activating a mana ability of that token that includes the tap symbol ({T}) in its cost.")
        ruling("2024-04-12", "The additional mana is produced by Roxanne, not by the artifact token that you tapped for mana.")
    }
}
