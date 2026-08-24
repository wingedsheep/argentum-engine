package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Thelonite Druid
 * {2}{G}
 * Creature — Human Cleric Druid
 * 1/1
 * {1}{G}, {T}, Sacrifice a creature: Forests you control become 2/3 creatures until end of turn.
 * They're still lands.
 *
 * Sylvan Awakening's shape, narrowed to Forests: one [BecomeCreatureEffect] per land, adding no
 * creature type and keeping the land type — "they're still lands" is exactly what
 * [BecomeCreatureEffect] does by default. The set is fixed at resolution, so a Forest that arrives
 * later in the turn is unaffected.
 */
val TheloniteDruid = card("Thelonite Druid") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Cleric Druid"
    oracleText = "{1}{G}, {T}, Sacrifice a creature: Forests you control become 2/3 creatures " +
        "until end of turn. They're still lands."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{G}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Creature)
        )
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Land.withSubtype(Subtype.FOREST).youControl()),
            effect = BecomeCreatureEffect(
                target = EffectTarget.Self,
                power = DynamicAmount.Fixed(2),
                toughness = DynamicAmount.Fixed(3),
                duration = Duration.EndOfTurn
            )
        )
        description = "{1}{G}, {T}, Sacrifice a creature: Forests you control become 2/3 creatures until end of turn. They're still lands."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Margaret Organ-Kean"
        flavorText = "\"The magic at the heart of all living things can bear awe-inspiring fruit.\"\n—Kolevi of Havenwood, Elder Druid"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd8772dd-513d-4dd0-a5db-5214dc8da4e0.jpg?1783947884"
    }
}
