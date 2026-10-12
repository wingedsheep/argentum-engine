package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Penregon Strongbull
 * {2}{R}
 * Creature — Minotaur
 * 2/3
 * {1}, Sacrifice an artifact: This creature gets +1/+1 until end of turn and deals 1 damage to
 * each opponent.
 */
val PenregonStrongbull = card("Penregon Strongbull") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur"
    power = 2
    toughness = 3
    oracleText = "{1}, Sacrifice an artifact: This creature gets +1/+1 until end of turn and deals 1 damage to each opponent."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Sacrifice(GameObjectFilter.Artifact))
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self) then
            Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "{1}, Sacrifice an artifact: This creature gets +1/+1 until end of turn and deals 1 damage to each opponent."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "147"
        artist = "Michael Phillippi"
        flavorText = "His child-lifting act was second only to his cauldron-punting routine."
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c2deb43-7134-4f9c-b02d-78407a8986b1.jpg?1783920064"
    }
}
