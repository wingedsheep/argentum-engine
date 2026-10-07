package com.wingedsheep.mtg.sets.definitions.csp.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val PhyrexianIronfoot = card("Phyrexian Ironfoot") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Snow Artifact Creature — Phyrexian Construct"
    oracleText = "This creature doesn't untap during your untap step.\n{1}{S}: Untap this creature. ({S} can be paid with one mana from a snow source.)"
    power = 3
    toughness = 4

    flags(AbilityFlag.DOESNT_UNTAP)

    activatedAbility {
        cost = Costs.Mana("{1}{S}")
        effect = Effects.Untap(EffectTarget.Self)
        description = "{1}{S}: Untap this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "139"
        artist = "Stephan Martiniere"
        flavorText = "It took the Rimewind cultists days to realize they had successfully activated the creature—it just wasn't interested in moving."
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49ff9859-92a4-4732-94bf-6c02b0a57338.jpg?1783943320"
    }
}
