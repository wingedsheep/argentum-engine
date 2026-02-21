package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Wirewood Lodge
 * Land
 * {T}: Add {C}.
 * {G}, {T}: Untap target Elf.
 */
val WirewoodLodge = card("Wirewood Lodge") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{G}, {T}: Untap target Elf."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddColorlessManaEffect(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Tap)
        val t = target("target", TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Elf"))
        ))
        effect = TapUntapEffect(
            target = t,
            tap = false
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "329"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/large/front/3/d/3d251490-41bb-4ad3-bfd0-a5e66ee42598.jpg?1562909365"
    }
}
