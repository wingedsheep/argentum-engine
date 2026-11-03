package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lore, Positronic Provocateur
 * {1}{U/B}{U/B}
 * Legendary Artifact Creature — Android Rogue
 * 2/3
 * When Lore enters, choose one —
 * • Target creature gets +1/+0 until end of turn and can't be blocked this turn.
 * • Surveil 2.
 */
val LorePositronicProvocateur = card("Lore, Positronic Provocateur") {
    manaCost = "{1}{U/B}{U/B}"
    colorIdentity = "UB"
    typeLine = "Legendary Artifact Creature — Android Rogue"
    power = 2
    toughness = 3
    oracleText = "When Lore enters, choose one —\n" +
        "• Target creature gets +1/+0 until end of turn and can't be blocked this turn.\n" +
        "• Surveil 2. (Look at the top two cards of your library, then put any number of them into " +
        "your graveyard and the rest on top of your library in any order.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Target creature gets +1/+0 until end of turn and can't be blocked this turn.") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(1, 0, creature) then
                    Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature)
            },
            mode("Surveil 2.") {
                effect = Effects.Surveil(2)
            },
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "246"
        artist = "Wei Guan"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62afa55d-d4ba-48d5-9991-0f8408f9c92c.jpg?1792508378"
    }
}
