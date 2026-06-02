package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.Effects

/**
 * Memorial to Folly
 * Land
 * Memorial to Folly enters the battlefield tapped.
 * {T}: Add {B}.
 * {2}{B}, {T}, Sacrifice Memorial to Folly: Return target creature card from your graveyard to your hand.
 */
val MemorialToFolly = card("Memorial to Folly") {
    typeLine = "Land"
    colorIdentity = "B"
    oracleText = "Memorial to Folly enters the battlefield tapped.\n{T}: Add {B}.\n{2}{B}, {T}, Sacrifice Memorial to Folly: Return target creature card from your graveyard to your hand."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.Tap, Costs.SacrificeSelf)
        val creature = target("target creature card from your graveyard", TargetObject(
            filter = TargetFilter(
                GameObjectFilter.Creature.ownedByYou(),
                zone = Zone.GRAVEYARD
            )
        ))
        effect = Effects.Move(
            target = creature,
            destination = Zone.HAND
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "242"
        artist = "Sung Choi"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebbe56f2-aec5-4e8b-8003-1bf1ca7f5659.jpg?1562744964"
    }
}
