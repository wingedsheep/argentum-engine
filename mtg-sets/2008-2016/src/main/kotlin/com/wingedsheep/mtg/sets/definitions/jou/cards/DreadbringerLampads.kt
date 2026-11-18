package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dreadbringer Lampads
 * {4}{B}
 * Enchantment Creature — Nymph
 * 4/2
 *
 * Constellation — Whenever this creature or another enchantment you control enters, target
 * creature gains intimidate until end of turn.
 *
 * The Lampads is itself an enchantment, so one watcher over enchantments you control
 * (`Triggers.a`) covers both "this creature" and "another enchantment" — as Eidolon of Blossoms.
 */
val DreadbringerLampads = card("Dreadbringer Lampads") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Nymph"
    power = 4
    toughness = 2
    oracleText = "Constellation — Whenever this creature or another enchantment you control enters, " +
        "target creature gains intimidate until end of turn. (It can't be blocked except by artifact " +
        "creatures and/or creatures that share a color with it.)"

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.INTIMIDATE, creature)
        description = "Constellation — Whenever this creature or another enchantment you control enters, " +
            "target creature gains intimidate until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Willian Murai"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a2b4819-8aeb-49c0-b40a-3ff280604970.jpg?1783939436"
    }
}
