package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Terrifying Presence
 * {1}{G}
 * Instant
 * Prevent all combat damage that would be dealt by creatures other than target creature this turn.
 *
 * The target is pinned as the spell resolves, so the shield keeps excluding that creature for the
 * rest of the turn; which other creatures are covered is read when the damage would be dealt.
 */
val TerrifyingPresence = card("Terrifying Presence") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Prevent all combat damage that would be dealt by creatures other than target creature this turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.PreventCombatDamageFrom(GameObjectFilter.Creature.otherThan(creature))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "196"
        artist = "Jaime Jones"
        flavorText = "Elmut had slaughtered vampires, slain werewolves, and destroyed hordes of zombies. Sadly, he never got over his fear of spiders."
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e8d0a22-f31b-45c0-85c7-0101aa63c77b.jpg?1783940661"
        ruling(
            "2012-05-01",
            "Although Terrifying Presence doesn't affect the target creature, if that creature is an illegal target when Terrifying Presence tries to resolve, it won't resolve and none of its effects will happen. No damage will be prevented.",
        )
    }
}
