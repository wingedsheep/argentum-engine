package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Fang Guardian — Aetherdrift #162
 * {3}{G} · Creature — Ape Druid · 4/2
 *
 * Flash
 * When this creature enters, another target creature or Vehicle you control gets +2/+2 until end of turn.
 *
 * "Another target creature or Vehicle you control" is a [GameObjectFilter.CreatureOrVehicle]
 * permanent restricted to your control with `excludeSelf` (a Vehicle matched by subtype even while
 * not a creature). The buff is a trigger-granted [Effects.ModifyStats], which defaults to
 * end-of-turn duration.
 */
val FangGuardian = card("Fang Guardian") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Ape Druid"
    power = 4
    toughness = 2
    oracleText = "Flash\n" +
        "When this creature enters, another target creature or Vehicle you control gets +2/+2 until end of turn."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target(
            "another target creature or Vehicle you control",
            TargetPermanent(
                filter = TargetFilter(
                    GameObjectFilter.CreatureOrVehicle.youControl(),
                    excludeSelf = true
                )
            )
        )
        effect = Effects.ModifyStats(2, 2, t)
        description = "When this creature enters, another target creature or Vehicle you control gets " +
            "+2/+2 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "162"
        artist = "Jason A. Engle"
        flavorText = "Though the Fang Druids desired peace, the Grand Prix's interference threatened to " +
            "awaken old dangers on Muraganda."
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6db483a7-c9f0-449d-be97-77dbd6d3a27a.jpg?1782687833"
    }
}
