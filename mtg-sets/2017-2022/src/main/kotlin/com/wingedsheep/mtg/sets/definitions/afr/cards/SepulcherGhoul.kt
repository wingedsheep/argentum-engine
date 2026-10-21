package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sepulcher Ghoul
 * {1}{B}
 * Creature — Zombie
 * 2/1
 * Sacrifice another creature: This creature gets +2/+2 until end of turn. Activate only once each turn.
 */
val SepulcherGhoul = card("Sepulcher Ghoul") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    oracleText = "Sacrifice another creature: This creature gets +2/+2 until end of turn. Activate only once each turn."
    power = 2
    toughness = 1
    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.Creature)
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.OncePerTurn)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "118"
        artist = "Jason A. Engle"
        flavorText = "Ghouls devour the dead and any living creatures unwary enough to fall into their grasp."
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b302bc8-683f-4a26-ab86-87eaa19dce4d.jpg?1783926490"
    }
}
