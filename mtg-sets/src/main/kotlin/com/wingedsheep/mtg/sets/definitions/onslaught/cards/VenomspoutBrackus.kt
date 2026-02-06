package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetPermanent

/**
 * Venomspout Brackus
 * {6}{G}
 * Creature — Beast
 * 5/5
 * {1}{G}, {T}: Venomspout Brackus deals 5 damage to target attacking or blocking creature with flying.
 * Morph {3}{G}{G}
 */
val VenomspoutBrackus = card("Venomspout Brackus") {
    manaCost = "{6}{G}"
    typeLine = "Creature — Beast"
    power = 5
    toughness = 5

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{G}"),
            AbilityCost.Tap
        )
        target = TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.attackingOrBlocking().withKeyword(Keyword.FLYING))
        )
        effect = DealDamageEffect(5, EffectTarget.ContextTarget(0))
    }

    morph = "{3}{G}{G}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "295"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/0774771c-5373-4636-9174-d06e7d635183.jpg?1562896736"
    }
}
