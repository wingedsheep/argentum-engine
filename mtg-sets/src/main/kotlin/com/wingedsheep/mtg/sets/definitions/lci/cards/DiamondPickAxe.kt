package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility

/**
 * Diamond Pick-Axe (LCI #143) — {R} Artifact — Equipment
 *
 * Indestructible (Effects that say "destroy" don't destroy this Equipment.)
 * Equipped creature gets +1/+1 and has "Whenever this creature attacks, create a Treasure token."
 * Equip {2}
 *
 * Implementation notes:
 * - Indestructible is a printed keyword on the Equipment itself, not on the equipped creature —
 *   wired via [keywords(Keyword.INDESTRUCTIBLE)].
 * - The +1/+1 pump is a [ModifyStats] static ability scoped to [Filters.EquippedCreature].
 * - The attack-triggered Treasure creation is granted to the equipped creature via
 *   [GrantTriggeredAbility] with [Triggers.attacks()] (SELF binding) so the ability lives on the
 *   creature and fires when that creature attacks — matching the oracle "Whenever this creature
 *   attacks, create a Treasure token."
 */
val DiamondPickAxe = card("Diamond Pick-Axe") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "Indestructible (Effects that say \"destroy\" don't destroy this Equipment.)\n" +
        "Equipped creature gets +1/+1 and has \"Whenever this creature attacks, create a Treasure token.\"\n" +
        "Equip {2}"

    // The Equipment itself is indestructible.
    keywords(Keyword.INDESTRUCTIBLE)

    // Equipped creature gets +1/+1.
    staticAbility {
        ability = ModifyStats(+1, +1, Filters.EquippedCreature)
    }

    // Equipped creature has "Whenever this creature attacks, create a Treasure token."
    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.attacks().event,
                binding = Triggers.attacks().binding,
                effect = Effects.CreateTreasure()
            ),
            filter = Filters.EquippedCreature
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Dibujante Nocturno"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4ae30fa7-3d1d-417f-80d7-a668236cb2c1.jpg?1782694493"
    }
}
