package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SacrificeAtEndOfCombatEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mardu Blazebringer
 * {2}{R}
 * Creature — Ogre Warrior
 * 4/4
 * When Mardu Blazebringer attacks or blocks, sacrifice it at end of combat.
 */
val MarduBlazebringer = card("Mardu Blazebringer") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Ogre Warrior"
    power = 4
    toughness = 4
    oracleText = "When Mardu Blazebringer attacks or blocks, sacrifice it at end of combat."

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = SacrificeAtEndOfCombatEffect(EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.Blocks
        effect = SacrificeAtEndOfCombatEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "115"
        artist = "Peter Mohrbacher"
        flavorText = "\"Make sure he's pointed in the right direction before you light him. And don't let the goblins anywhere near the torch.\" —Kerai Suddenblade, Mardu hordechief"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbf7a797-f32a-4ed2-b835-a356120f5817.jpg?1562793597"
    }
}
