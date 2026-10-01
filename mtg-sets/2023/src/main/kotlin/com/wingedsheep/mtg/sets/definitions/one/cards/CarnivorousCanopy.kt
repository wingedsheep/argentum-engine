package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Carnivorous Canopy — Phyrexia: All Will Be One #162
 * {2}{G}
 * Sorcery
 * Destroy target artifact, enchantment, or creature with flying. If that permanent's mana value
 * was 3 or less, proliferate.
 *
 * The mana-value check reads the target card after the destroy (same shape as Seedship Impact /
 * Fading Hope), so it still applies whether the permanent died or survived.
 */
val CarnivorousCanopy = card("Carnivorous Canopy") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Destroy target artifact, enchantment, or creature with flying. If that permanent's mana value was 3 or less, proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    spell {
        val permanent = target(
            TargetFilter(
                GameObjectFilter.Artifact or GameObjectFilter.Enchantment or
                    GameObjectFilter.Creature.withKeyword(Keyword.FLYING)
            )
        )
        effect = Effects.Destroy(permanent) then
            Effects.If(
                condition = Conditions.TargetSpellManaValueAtMost(DynamicAmounts.fixed(3), permanent),
                then = Effects.Proliferate()
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "162"
        artist = "John Di Giovanni"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b43ba43-f7ae-4e00-b797-2360c3ccd839.jpg?1783918018"
    }
}
