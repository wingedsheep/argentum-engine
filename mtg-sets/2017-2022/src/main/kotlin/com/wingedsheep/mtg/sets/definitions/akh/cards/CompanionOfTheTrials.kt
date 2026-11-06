package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Companion of the Trials
 * {2}{W}
 * Creature — Bird Soldier
 * 2/2
 * Flying
 * {1}{W}: Untap target creature. Activate only if you control a Gideon planeswalker.
 */
val CompanionOfTheTrials = card("Companion of the Trials") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Soldier"
    oracleText = "Flying\n" +
            "{1}{W}: Untap target creature. Activate only if you control a Gideon planeswalker."
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        val creature = target(TargetFilter.Creature)
        effect = Effects.Untap(creature)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Exists(
                    Player.You,
                    Zone.BATTLEFIELD,
                    GameObjectFilter.Planeswalker.withSubtype("Gideon")
                )
            )
        )
        description = "{1}{W}: Untap target creature. Activate only if you control a Gideon planeswalker."
    }

    metadata {
        inBooster = false
        rarity = Rarity.UNCOMMON
        collectorNumber = "271"
        artist = "Aaron Miller"
        flavorText = "\"The fiercest loyalties are earned in battle.\"\n—Gideon Jura"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/637ac220-058f-4212-ba8b-a389bb0528bd.jpg?1783936437"
    }
}
