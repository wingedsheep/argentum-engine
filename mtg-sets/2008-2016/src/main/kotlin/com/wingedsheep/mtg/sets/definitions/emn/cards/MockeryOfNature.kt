package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.emerge
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mockery of Nature
 * {9}
 * Creature — Eldrazi Beast
 * 6/5
 *
 * Emerge {7}{G}
 * When you cast this spell, you may destroy target artifact or enchantment.
 *
 * The destroy is a *cast* trigger: it resolves before the creature itself, and still resolves if
 * the spell is countered.
 */
val MockeryOfNature = card("Mockery of Nature") {
    manaCost = "{9}"
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi Beast"
    power = 6
    toughness = 5
    oracleText = "Emerge {7}{G} (You may cast this spell by sacrificing a creature and paying the " +
        "emerge cost reduced by that creature's mana value.)\n" +
        "When you cast this spell, you may destroy target artifact or enchantment."

    emerge("{7}{G}")

    triggeredAbility {
        trigger = Triggers.self.isCast()
        val artifactOrEnchantment = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.May(Effects.Destroy(artifactOrEnchantment))
        description = "When you cast this spell, you may destroy target artifact or enchantment."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "10"
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/3118737f-2fd9-4fe5-bd0f-43c9ef2166e2.jpg"
    }
}
