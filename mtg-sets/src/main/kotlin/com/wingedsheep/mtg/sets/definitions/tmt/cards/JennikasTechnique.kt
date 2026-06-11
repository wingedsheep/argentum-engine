package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jennika's Technique
 * {2}{R}
 * Instant
 *
 * Sneak {R} (You may cast this spell for {R} if you also return an unblocked
 * attacker you control to hand during the declare blockers step.)
 * Jennika's Technique deals 2 damage to each creature.
 */
val JennikasTechnique = card("Jennika's Technique") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Sneak {R} (You may cast this spell for {R} if you also return an unblocked attacker you control to hand during the declare blockers step.)\nJennika's Technique deals 2 damage to each creature."

    sneak("{R}")

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature),
            DealDamageEffect(2, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "93"
        artist = "Andreas Zafiratos"
        flavorText = "\"Being a mutant isn't easy. Lucky for me, I've been preparing for this my entire life!\""
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df559199-c8b9-455b-aa07-0a042348de96.jpg?1771502629"
    }
}
