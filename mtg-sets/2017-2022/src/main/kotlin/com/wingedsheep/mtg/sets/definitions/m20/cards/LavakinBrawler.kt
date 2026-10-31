package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lavakin Brawler (Core Set 2020 #147)
 * {3}{R} · Creature — Elemental Warrior 2/4
 * Whenever this creature attacks, it gets +1/+0 until end of turn for each Elemental you control.
 *
 * "Elemental you control" counts Elemental permanents, Brawler itself included. The count is taken
 * as the trigger resolves and locked in for the turn.
 */
val LavakinBrawler = card("Lavakin Brawler") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental Warrior"
    power = 2
    toughness = 4
    oracleText = "Whenever this creature attacks, it gets +1/+0 until end of turn for each Elemental you control."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ModifyStats(
            power = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Permanent.withSubtype(Subtype.ELEMENTAL)
            ).count(),
            toughness = DynamicAmounts.fixed(0),
            target = EffectTarget.Self
        )
        description = "Whenever this creature attacks, it gets +1/+0 until end of turn for each Elemental you control."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "147"
        artist = "Victor Adame Minguez"
        flavorText = "She swings with the ease of flowing lava and hits with the force of a meteor."
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3eeea9e5-3041-4439-bc52-fd00ec83c7a1.jpg?1783932974"
        ruling(
            "2019-07-12",
            "The size of the bonus is determined as Lavakin Brawler's ability begins to resolve; it won't change later in the turn if the number of Elementals you control changes."
        )
        ruling(
            "2019-07-12",
            "Because Lavakin Brawler is itself an Elemental, its ability will usually give it at least +1/+0."
        )
    }
}
