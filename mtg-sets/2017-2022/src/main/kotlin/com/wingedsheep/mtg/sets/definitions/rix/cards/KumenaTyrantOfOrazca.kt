package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kumena, Tyrant of Orazca
 * {1}{G}{U}
 * Legendary Creature — Merfolk Shaman
 * 2/4
 *
 * Tap another untapped Merfolk you control: Kumena can't be blocked this turn.
 * Tap three untapped Merfolk you control: Draw a card.
 * Tap five untapped Merfolk you control: Put a +1/+1 counter on each Merfolk you control.
 *
 * Only the first ability says "another"; Kumena itself can be tapped to pay the second and third.
 * Tapping untapped Merfolk isn't a {T} cost, so summoning-sick Merfolk can pay it.
 */
val KumenaTyrantOfOrazca = card("Kumena, Tyrant of Orazca") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Merfolk Shaman"
    power = 2
    toughness = 4
    oracleText = "Tap another untapped Merfolk you control: Kumena can't be blocked this turn.\n" +
        "Tap three untapped Merfolk you control: Draw a card.\n" +
        "Tap five untapped Merfolk you control: Put a +1/+1 counter on each Merfolk you control."

    activatedAbility {
        cost = Costs.TapAnotherPermanent(GameObjectFilter.Permanent.withSubtype("Merfolk"))
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.TapPermanents(3, GameObjectFilter.Permanent.withSubtype("Merfolk"))
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.TapPermanents(5, GameObjectFilter.Permanent.withSubtype("Merfolk"))
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Permanent.withSubtype("Merfolk").youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "162"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3aef818-c896-46e6-aaff-56aee52a066c.jpg?1783935272"
    }
}
