package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Assembly-Worker
 * {3}
 * Artifact Creature — Assembly-Worker
 * 2/2
 * {T}: Target Assembly-Worker creature gets +1/+1 until end of turn.
 */
val AssemblyWorker = card("Assembly-Worker") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Assembly-Worker"
    power = 2
    toughness = 2
    oracleText = "{T}: Target Assembly-Worker creature gets +1/+1 until end of turn."

    activatedAbility {
        cost = Costs.Tap
        val t = target(TargetFilter.Creature.withSubtype(Subtype.ASSEMBLY_WORKER))
        effect = Effects.ModifyStats(1, 1, t)
        description = "{T}: Target Assembly-Worker creature gets +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "248"
        artist = "Chippy"
        flavorText = "With their factories long destroyed, some of Mishra's creations still toil in remote areas, " +
            "endlessly performing and reperforming their last orders."
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e086d662-3740-4704-9184-c42c6a16d829.jpg?1783943200"

        ruling(
            "2018-03-16",
            "Assembly-Worker's ability can target any creature with the Assembly-Worker subtype, " +
                "not only creatures named Assembly-Worker."
        )
    }
}
