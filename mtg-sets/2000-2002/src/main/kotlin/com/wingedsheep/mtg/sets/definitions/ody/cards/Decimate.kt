package com.wingedsheep.mtg.sets.definitions.ody.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Decimate {2}{R}{G}
 * Sorcery
 *
 * Destroy target artifact, target creature, target enchantment, and target land.
 * (You can't cast this spell unless you have legal choices for all its targets.)
 */
val Decimate = card("Decimate") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Sorcery"
    oracleText = "Destroy target artifact, target creature, target enchantment, and target land. " +
        "(You can't cast this spell unless you have legal choices for all its targets.)"

    spell {
        val artifact = target(TargetFilter.Artifact)
        val creature = target(TargetFilter.Creature)
        val enchantment = target(TargetFilter.Enchantment)
        val land = target(TargetFilter.Land)
        effect = Effects.Destroy(artifact) then
            Effects.Destroy(creature) then
            Effects.Destroy(enchantment) then
            Effects.Destroy(land)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "287"
        artist = "Alex Horley-Orlandelli"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/912c398a-e49a-4399-ac41-7b1d4328a59d.jpg?1783945206"
        flavorText = "\"Anyone can admire creation. Only a barbarian sees the beauty in demolition.\"\n—Kamahl, pit fighter"
    }
}
