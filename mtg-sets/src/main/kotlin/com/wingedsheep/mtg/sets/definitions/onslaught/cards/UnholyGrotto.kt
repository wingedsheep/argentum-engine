package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.ZonePlacement
import com.wingedsheep.sdk.targeting.TargetObject

/**
 * Unholy Grotto
 * Land
 * {T}: Add {C}.
 * {B}, {T}: Put target Zombie card from your graveyard on top of your library.
 */
val UnholyGrotto = card("Unholy Grotto") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{B}, {T}: Put target Zombie card from your graveyard on top of your library."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddColorlessManaEffect(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.Tap)
        target = TargetObject(
            filter = TargetFilter(
                GameObjectFilter.Any.withSubtype("Zombie").ownedByYou(),
                zone = Zone.GRAVEYARD
            )
        )
        effect = MoveToZoneEffect(
            target = EffectTarget.ContextTarget(0),
            destination = Zone.LIBRARY,
            placement = ZonePlacement.Top
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "327"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/large/front/c/2/c2f7bcc7-74b5-4ecc-b5c1-3c9e56a2a31b.jpg?1562935857"
    }
}
