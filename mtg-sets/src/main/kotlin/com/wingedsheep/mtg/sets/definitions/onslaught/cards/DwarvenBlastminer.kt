package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardPredicate
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetPermanent

/**
 * Dwarven Blastminer
 * {1}{R}
 * Creature — Dwarf
 * 1/1
 * {2}{R}, {T}: Destroy target nonbasic land.
 * Morph {R}
 */
val DwarvenBlastminer = card("Dwarven Blastminer") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Dwarf"
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.Tap)
        target = TargetPermanent(
            filter = TargetFilter(
                GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.IsLand,
                        CardPredicate.Not(CardPredicate.IsBasicLand)
                    )
                )
            )
        )
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.GRAVEYARD, byDestruction = true)
    }

    morph = "{R}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "199"
        artist = "Gary Ruddell"
        flavorText = "\"Life is too short for something like a hunk of rock to get in my way.\""
        imageUri = "https://cards.scryfall.io/large/front/2/9/2970831a-738b-476f-9d46-39f10a1f91e7.jpg?1562904808"
    }
}
