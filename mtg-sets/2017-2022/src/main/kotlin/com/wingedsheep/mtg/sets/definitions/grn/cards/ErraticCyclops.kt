package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Erratic Cyclops
 * {3}{R}
 * Creature — Cyclops Shaman
 * 0/8
 *
 * Trample
 * Whenever you cast an instant or sorcery spell, this creature gets +X/+0 until end of turn, where X
 * is that spell's mana value.
 *
 * "That spell's mana value" reads [DynamicAmounts.triggeringSpellManaValue], captured on the cast
 * event, so the bonus still applies if the spell is countered before the trigger resolves (ruling).
 */
val ErraticCyclops = card("Erratic Cyclops") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Cyclops Shaman"
    power = 0
    toughness = 8
    oracleText = "Trample\n" +
        "Whenever you cast an instant or sorcery spell, this creature gets +X/+0 until end of turn, " +
        "where X is that spell's mana value."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.ModifyStats(
            DynamicAmounts.triggeringSpellManaValue(),
            DynamicAmounts.fixed(0),
            EffectTarget.Self
        )
        description = "Whenever you cast an instant or sorcery spell, Erratic Cyclops gets +X/+0 until end " +
            "of turn, where X is that spell's mana value."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Milivoj Ćeran"
        flavorText = "\"The League accepts no responsibility for former members and the damage they may " +
            "cause.\"\n—Maree, Izzet chamberlain"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c310df89-d894-40ab-ae34-7d0bfd19a1af.jpg?1783934165"
        ruling("2018-10-05", "If a spell has {X} in its mana cost, include the value chosen for that X when determining the mana value of that spell.")
        ruling("2018-10-05", "Erratic Cyclops's triggered ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
    }
}
