package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CrewSaddleContribution
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Roadside Assistance — Aetherdrift #26
 * {2}{W} · Enchantment — Aura
 */
val RoadsideAssistance = card("Roadside Assistance") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature or Vehicle\n" +
        "When this Aura enters, create a 1/1 colorless Pilot creature token with \"This token " +
        "saddles Mounts and crews Vehicles as though its power were 2 greater.\"\n" +
        "Enchanted permanent gets +1/+1 and has lifelink."

    auraTarget = TargetPermanent(filter = TargetFilter(GameObjectFilter.CreatureOrVehicle))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Pilot"),
            imageUri = "https://cards.scryfall.io/normal/front/8/6/8672d795-04f9-4089-9c92-6d6ff628da12.jpg?1783907682",
            staticAbilities = listOf(CrewSaddleContribution(modifier = 2))
        )
        description = "When this Aura enters, create a 1/1 colorless Pilot creature token."
    }

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.LIFELINK)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Artur Nakhodkin"
        flavorText = "No road has to be traveled alone."
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f2a9154-7b43-4b8d-9d81-d11cfda5d597.jpg?1783907914"
        ruling(
            "2025-02-07",
            "Saddling a Mount or crewing a Vehicle doesn’t cause the token’s power to change."
        )
    }
}
