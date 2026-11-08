package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Slimy Piper
 * {1}{G}
 * Creature — Fungus Bard
 * 2/1
 *
 * Whenever this creature attacks, it gets +1/+1 until end of turn. If you control four or more
 * creatures, it gets +2/+2 and gains indestructible until end of turn instead.
 *
 * The creature count is checked as the trigger resolves (not an intervening "if"), and includes
 * Slimy Piper itself.
 */
val SlimyPiper = card("Slimy Piper") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Fungus Bard"
    power = 2
    toughness = 1
    oracleText = "Whenever this creature attacks, it gets +1/+1 until end of turn. If you control four or more creatures, it gets +2/+2 and gains indestructible until end of turn instead. (Damage and effects that say \"destroy\" don't destroy it.)"

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.If(
            condition = Conditions.ControlCreaturesAtLeast(4),
            then = Effects.ModifyStats(2, 2, EffectTarget.Self) then
                Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self),
            otherwise = Effects.ModifyStats(1, 1, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Borja Pindado"
        flavorText = "All throughout the glade, sprouts squelched happily along to the tune."
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e89dd05a-e21e-4252-ba18-9f3329c6d4dd.jpg?1783908863"
    }
}
