package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rot Farm Skeleton
 * {2}{B}{G}
 * Creature — Plant Skeleton
 * 4/1
 * This creature can't block.
 * {2}{B}{G}, Mill four cards: Return this card from your graveyard to the battlefield. Activate
 * only as a sorcery.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val RotFarmSkeleton = card("Rot Farm Skeleton") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Plant Skeleton"
    power = 4
    toughness = 1
    oracleText = "This creature can't block.\n" +
        "{2}{B}{G}, Mill four cards: Return this card from your graveyard to the battlefield. Activate only as a sorcery."

    staticAbility {
        ability = CantBlock()
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}{G}"), Costs.Mill(4))
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        effect = Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
        description = "{2}{B}{G}, Mill four cards: Return this card from your graveyard to the battlefield. " +
            "Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "98"
        artist = "Maciej Kuciara"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef5af2dd-75c7-402c-be9a-3d0d4290520c.jpg"
    }
}
