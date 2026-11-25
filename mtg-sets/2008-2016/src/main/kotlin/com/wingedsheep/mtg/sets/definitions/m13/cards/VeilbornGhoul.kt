package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Veilborn Ghoul
 * {4}{B}
 * Creature — Zombie
 * 4/1
 * This creature can't block.
 * Whenever a Swamp you control enters, you may return this card from your graveyard to your hand.
 *
 * The recursion trigger functions only from the graveyard (`triggerZone = Zone.GRAVEYARD`), the
 * same shape as Fear of Infinity's Eerie trigger.
 */
val VeilbornGhoul = card("Veilborn Ghoul") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    oracleText = "This creature can't block.\n" +
        "Whenever a Swamp you control enters, you may return this card from your graveyard to your hand."
    power = 4
    toughness = 1

    staticAbility { ability = CantBlock() }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.withSubtype("Swamp").youControl()).enters()
        triggerZone = Zone.GRAVEYARD
        effect = Effects.May(Effects.ReturnToHandFromGraveyard(EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Dan Murayama Scott"
        flavorText = "Liliana's voice carries far, reaching beyond the cold pall of death."
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3f49232-2853-427f-8c20-322e09a3ccde.jpg"
    }
}
