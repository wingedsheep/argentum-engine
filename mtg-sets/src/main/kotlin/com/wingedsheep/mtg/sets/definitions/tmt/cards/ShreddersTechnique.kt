package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.sneak
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shredder's Technique
 * {2}{B}
 * Sorcery
 *
 * Sneak {B} (You may cast this spell for {B} if you also return an unblocked
 * attacker you control to hand during the declare blockers step.)
 * Destroy target creature or enchantment. If an enchantment was destroyed this
 * way, you lose 2 life.
 *
 * The enchantment-ness is read off the chosen target before it is destroyed.
 * (An indestructible enchantment that survives would still trigger the life
 * loss — a negligible edge given no TMT enchantment is indestructible.)
 */
val ShreddersTechnique = card("Shredder's Technique") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Sneak {B} (You may cast this spell for {B} if you also return an unblocked attacker you control to hand during the declare blockers step.)\nDestroy target creature or enchantment. If an enchantment was destroyed this way, you lose 2 life."

    sneak("{B}")

    spell {
        val t = target("target creature or enchantment", Targets.CreatureOrEnchantment)
        effect = ConditionalEffect(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Enchantment),
            effect = Effects.Destroy(t).then(Effects.LoseLife(2, EffectTarget.Controller)),
            elseEffect = Effects.Destroy(t)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "77"
        artist = "Dominik Mayer"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99a24349-7d11-421b-a161-c1edbb8f53b1.jpg?1771342367"
    }
}
