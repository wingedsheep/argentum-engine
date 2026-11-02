package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hellkite Whelp
 * {4}{R}
 * Creature — Dragon
 * 3/3
 * Flying
 * Whenever this creature attacks, it deals 1 damage to target creature defending player controls.
 */
val HellkiteWhelp = card("Hellkite Whelp") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    oracleText = "Flying\n" +
        "Whenever this creature attacks, it deals 1 damage to target creature defending player controls."
    power = 3
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.Creature.defendingPlayerControls())
        effect = Effects.DealDamage(1, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Johann Bodin"
        flavorText = "\"They play by spitting fire at each other. Don't be offended if one gives you a love-scorch.\"\n" +
            "—Esfir, Rakdos dragon wrangler"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65e902e8-bbd5-40fe-a655-45d3d3bc9fde.jpg?1783934162"
    }
}
