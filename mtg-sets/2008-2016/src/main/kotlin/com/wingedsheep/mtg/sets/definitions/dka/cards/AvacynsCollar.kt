package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Avacyn's Collar
 * {1}
 * Artifact — Equipment
 * Equipped creature gets +1/+0 and has vigilance.
 * Whenever equipped creature dies, if it was a Human, create a 1/1 white Spirit creature token
 * with flying.
 * Equip {2}
 *
 * "If it was a Human" reads the dead creature's last-known type line off the dies event
 * (`TriggeringEntityWas`), the same shape as Slayer's Plate.
 */
val AvacynsCollar = card("Avacyn's Collar") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+0 and has vigilance.\n" +
        "Whenever equipped creature dies, if it was a Human, create a 1/1 white Spirit creature " +
        "token with flying.\n" +
        "Equip {2}"

    staticAbility {
        ability = ModifyStats(1, 0, Filters.EquippedCreature)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        interveningIf = Conditions.TriggeringEntityWas(GameObjectFilter.Any.withSubtype("Human"))
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING)
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "145"
        artist = "James Paick"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/972e9a78-204b-4012-b394-b40fd0edac4c.jpg?1783940792"
    }
}
