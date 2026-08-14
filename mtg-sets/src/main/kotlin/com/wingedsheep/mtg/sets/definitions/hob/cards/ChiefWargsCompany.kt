package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Chief Warg's Company — The Hobbit #151
 * {1}{B}{G} · Creature — Wolf · Rare
 * 5/3
 *
 * Trample
 * This creature can't attack unless you control two or more other Wolves.
 * At the beginning of your upkeep, create a 2/2 green Wolf creature token.
 *
 * The attack restriction counts *other* Wolves, so it is `AggregateBattlefield(excludeSelf = true)`
 * rather than a plain "three or more Wolves" tally: `CantAttackUnlessDefenderRule` evaluates the
 * condition with `sourceId` set to the attacker, which is what `excludeSelf` keys off. Counting three
 * Wolves total would silently diverge the moment this creature stops being a Wolf (a type-changing
 * effect), or when it isn't itself the one attacking.
 *
 * The tokens it makes are Wolves too, so the restriction unlocks itself after two upkeeps.
 */
val ChiefWargsCompany = card("Chief Warg's Company") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Wolf"
    power = 5
    toughness = 3
    oracleText = "Trample\n" +
        "This creature can't attack unless you control two or more other Wolves.\n" +
        "At the beginning of your upkeep, create a 2/2 green Wolf creature token."

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = CantAttackUnless(
            Compare(
                DynamicAmount.AggregateBattlefield(
                    player = Player.You,
                    filter = GameObjectFilter.Creature.withSubtype(Subtype.WOLF),
                    excludeSelf = true
                ),
                ComparisonOperator.GTE,
                DynamicAmount.Fixed(2)
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf"),
            controller = EffectTarget.Controller,
            imageUri = "https://cards.scryfall.io/normal/front/d/5/d5f1e139-3054-4273-8a4d-faaaa9c383a8.jpg?1783924694",
        )
        description = "At the beginning of your upkeep, create a 2/2 green Wolf creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "151"
        artist = "Jason Kang"
        flavorText = "Seldom did Wargs venture near the dwellings of Humans, preferring instead to " +
            "hunt and den over the Edge of the Wild."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbc634af-63d2-444a-8123-85f16fe3e364.jpg?1784733927"
    }
}
