package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Contested Cliffs
 * Land
 * {T}: Add {C}.
 * {R}{G}, {T}: Target Beast creature you control fights target creature an opponent controls.
 */
val ContestedCliffs = card("Contested Cliffs") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{R}{G}, {T}: Target Beast creature you control fights target creature an opponent controls."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddColorlessManaEffect(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{R}{G}"),
            Costs.Tap
        )
        val beast = target("Beast creature you control", TargetCreature(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Beast").youControl())
        ))
        val opponentCreature = target("creature an opponent controls", TargetCreature(
            filter = TargetFilter(GameObjectFilter.Creature.opponentControls())
        ))
        effect = Effects.Fight(beast, opponentCreature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "314"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d6363ea-3814-4014-ad9e-1066c72d907c.jpg?1562928304"
    }
}
