package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Darkthicket Wolf
 * {1}{G}
 * Creature — Wolf
 * 2/2
 * {2}{G}: This creature gets +2/+2 until end of turn. Activate only once each turn.
 */
val DarkthicketWolf = card("Darkthicket Wolf") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf"
    oracleText = "{2}{G}: This creature gets +2/+2 until end of turn. Activate only once each turn."
    power = 2
    toughness = 2
    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.OncePerTurn)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "175"
        artist = "Wayne England"
        flavorText = "Werewolf slayers don't know for certain how werewolves are created. So they kill all the wolves, just to be safe."
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fec37c5a-8223-441c-a8a6-8da1a2dfc3fb.jpg"
    }
}
