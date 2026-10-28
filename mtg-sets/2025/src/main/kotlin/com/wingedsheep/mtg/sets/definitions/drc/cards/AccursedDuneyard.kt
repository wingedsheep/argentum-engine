package com.wingedsheep.mtg.sets.definitions.drc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Accursed Duneyard — Aetherdrift Commander #20 (canonical printing)
 * Land
 *
 * {T}: Add {C}.
 * {2}, {T}: Regenerate target Shade, Skeleton, Specter, Spirit, Vampire, Wraith, or Zombie.
 */
val AccursedDuneyard = card("Accursed Duneyard") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "{T}: Add {C}.\n" +
        "{2}, {T}: Regenerate target Shade, Skeleton, Specter, Spirit, Vampire, Wraith, or Zombie. " +
        "(The next time it would be destroyed this turn, instead tap it, remove it from combat, " +
        "and heal all damage on it.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val undead = target(
            TargetFilter(
                GameObjectFilter.Permanent.withAnySubtype(
                    "Shade", "Skeleton", "Specter", "Spirit", "Vampire", "Wraith", "Zombie"
                )
            )
        )
        effect = Effects.Regenerate(undead)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Maxime Minard"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd9e6ba8-1c5e-4416-8bff-90db3b3b1f41.jpg?1783907748"
    }
}
