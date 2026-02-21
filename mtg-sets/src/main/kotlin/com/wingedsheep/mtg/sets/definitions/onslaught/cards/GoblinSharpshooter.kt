package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Goblin Sharpshooter
 * {2}{R}
 * Creature — Goblin
 * 1/1
 * Goblin Sharpshooter doesn't untap during your untap step.
 * Whenever a creature dies, untap Goblin Sharpshooter.
 * {T}: Goblin Sharpshooter deals 1 damage to any target.
 */
val GoblinSharpshooter = card("Goblin Sharpshooter") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Goblin"
    power = 1
    toughness = 1
    oracleText = "Goblin Sharpshooter doesn't untap during your untap step.\nWhenever a creature dies, untap Goblin Sharpshooter.\n{T}: Goblin Sharpshooter deals 1 damage to any target."

    keywords(Keyword.DOESNT_UNTAP)

    triggeredAbility {
        trigger = Triggers.AnyCreatureDies
        effect = Effects.Untap(EffectTarget.Self)
    }

    activatedAbility {
        cost = AbilityCost.Tap
        val t = target("target", AnyTarget())
        effect = DealDamageEffect(1, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "207"
        artist = "Greg Staples"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/large/front/7/e/7e689df7-b85d-4346-bee8-5e978b5cbbbc.jpg?1562924782"
    }
}
