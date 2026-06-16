package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Sandstorm Verge
 * Land — Desert
 *
 * {T}: Add {C}.
 * {3}, {T}: Target creature can't block this turn. Activate only as a sorcery.
 */
val SandstormVerge = card("Sandstorm Verge") {
    typeLine = "Land — Desert"
    colorIdentity = ""
    oracleText = "{T}: Add {C}.\n" +
        "{3}, {T}: Target creature can't block this turn. Activate only as a sorcery."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddColorlessManaEffect(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        val creature = target("creature", TargetCreature())
        effect = Effects.CantBlock(creature)
        description = "{3}, {T}: Target creature can't block this turn. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "263"
        artist = "Jorge Jacinto"
        flavorText = "\"Ever been caught out in a storm like that? Think of it like drowning, " +
            "but all the water is knives.\"\n—Annie Flash"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3ab4e0a4-2faf-456b-99e3-ee06c008538c.jpg?1712356353"
    }
}
