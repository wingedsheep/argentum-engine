package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Slayer's Plate
 * {3}
 * Artifact — Equipment
 * Equipped creature gets +4/+2.
 * Whenever equipped creature dies, if it was a Human, create a 1/1 white Spirit creature token
 * with flying.
 * Equip {3}
 *
 * "If it was a Human" is an intervening-if (CR 603.4) read off the dead creature's last-known
 * type line carried on the dies event (`TriggeringEntityWas`), so a Human token — gone by
 * resolution — and a creature that was a Human only through a continuous effect both count.
 */
val SlayersPlate = card("Slayer's Plate") {
    manaCost = "{3}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +4/+2.\n" +
        "Whenever equipped creature dies, if it was a Human, create a 1/1 white Spirit creature " +
        "token with flying.\n" +
        "Equip {3}"

    staticAbility {
        ability = ModifyStats(4, 2)
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

    equipAbility("{3}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "264"
        artist = "Ben Maier"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0810651-0301-46d4-b9d7-c2283ef4031a.jpg?1783937705"
    }
}
