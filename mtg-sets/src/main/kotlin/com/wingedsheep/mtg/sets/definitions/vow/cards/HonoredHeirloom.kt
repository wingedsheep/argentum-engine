package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Honored Heirloom
 * {3}
 * Artifact
 * {T}: Add one mana of any color.
 * {2}, {T}: Exile target card from a graveyard.
 */
val HonoredHeirloom = card("Honored Heirloom") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add one mana of any color.\n{2}, {T}: Exile target card from a graveyard."
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val t = target("target", TargetObject(filter = TargetFilter.CardInGraveyard))
        effect = Effects.Move(t, Zone.EXILE)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "257"
        artist = "Leanna Crossan"
        flavorText = "After the Travails, Runechanters set about repairing twisted symbols of Avacyn as the first step in restoring faith in the Church."
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3390e4d-9137-40ff-b998-bdb19c90b7d5.jpg?1782703014"
    }
}
