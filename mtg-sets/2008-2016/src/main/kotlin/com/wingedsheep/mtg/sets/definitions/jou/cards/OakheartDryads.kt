package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Oakheart Dryads
 * {2}{G}
 * Enchantment Creature — Nymph Dryad
 * 2/3
 *
 * Constellation — Whenever this creature or another enchantment you control enters, target
 * creature gets +1/+1 until end of turn.
 *
 * The Dryads are themselves an enchantment, so "this creature or another enchantment you control"
 * is one watcher over enchantments you control (`Triggers.a`), firing for their own entry too —
 * the shape Eidolon of Blossoms uses.
 */
val OakheartDryads = card("Oakheart Dryads") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Nymph Dryad"
    power = 2
    toughness = 3
    oracleText = "Constellation — Whenever this creature or another enchantment you control enters, " +
        "target creature gets +1/+1 until end of turn."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 1, creature)
        description = "Constellation — Whenever this creature or another enchantment you control enters, " +
            "target creature gets +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "133"
        artist = "Johann Bodin"
        flavorText = "\"I do not know if the forest was different after they passed. But I know I was.\"\n" +
            "—Ianthe, Setessan hunter"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8ef63b9-c021-40fe-910e-93c06d660c20.jpg?1783939411"
    }
}
