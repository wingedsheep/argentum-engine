package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Project Deathlok Soldier
 * {B}
 * Artifact Creature — Zombie Soldier
 * 1/2
 * {2}{B}: Return this card from your graveyard to your hand.
 *
 * A graveyard-activated ability (`activateFromZone = Zone.GRAVEYARD`) — no activation timing
 * restriction is printed, so it can be activated any time its controller has priority.
 */
val ProjectDeathlokSoldier = card("Project Deathlok Soldier") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Zombie Soldier"
    oracleText = "{2}{B}: Return this card from your graveyard to your hand."
    power = 1
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{2}{B}")
        effect = Effects.Move(EffectTarget.Self, Zone.HAND)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "109"
        artist = "InHyuk Lee"
        flavorText = "\"ENERGY OUTPUT AT 97.003%. WAKE UP, DEATHLOK UNIT. REPEAT: WAKE UP.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c21a120-4ec7-46ec-974a-2204edb92abb.jpg?1783902940"
    }
}
