package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReplaceLandManaColor
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Deep Water
 * {U}{U}
 * Enchantment
 * {U}: Until end of turn, if you tap a land you control for mana, it produces {U} instead of any
 * other type.
 *
 * The rule is Pulse of Llanowar's [ReplaceLandManaColor] with two differences, and both of them
 * are the interesting part of this card:
 *
 *  - The colour is **fixed** ({U}) rather than a free choice, so the static carries a `color` and
 *    the mana ability's produced mana is rewritten directly instead of being routed through the
 *    any-colour choice machinery. For the mana solver that also means a matched land is *not*
 *    suddenly a five-colour source — it is a blue source.
 *  - It is **durational**, granted by an activated ability rather than printed. A grant lives in
 *    `GameState.grantedStaticAbilities`, which the layer projector does not carry, so the mana
 *    path had to learn the same point-of-use read the combat checks already do. Granting it to
 *    Deep Water itself (`EffectTarget.Self`) keeps the static where its filter's "you" means the
 *    enchantment's controller, which is exactly the printed "a land you control".
 */
val DeepWater = card("Deep Water") {
    manaCost = "{U}{U}"
    typeLine = "Enchantment"
    oracleText = "{U}: Until end of turn, if you tap a land you control for mana, it produces " +
        "{U} instead of any other type."

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Effects.GrantStaticAbility(
            ability = ReplaceLandManaColor(
                filter = GameObjectFilter.Land.youControl(),
                color = Color.BLUE,
            ),
            target = EffectTarget.Self,
        )
        description = "{U}: Until end of turn, if you tap a land you control for mana, it " +
            "produces {U} instead of any other type."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "23"
        artist = "Jeff A. Menges"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9dd6a230-6bc0-499c-b7fd-4aaa2569f98f.jpg?1783947945"
    }
}
