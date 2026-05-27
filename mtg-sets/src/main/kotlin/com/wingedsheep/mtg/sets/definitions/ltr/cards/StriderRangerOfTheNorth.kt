package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Strider, Ranger of the North
 * {2}{R}{G}
 * Legendary Creature — Human Ranger
 * 4/4
 *
 * Landfall — Whenever a land you control enters, target creature gets +1/+1 until end of turn.
 * Then if that creature has power 4 or greater, it gains first strike until end of turn.
 */
val StriderRangerOfTheNorth = card("Strider, Ranger of the North") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "GR"
    typeLine = "Legendary Creature — Human Ranger"
    power = 4
    toughness = 4
    oracleText = "Landfall — Whenever a land you control enters, target creature gets +1/+1 until end of turn. " +
        "Then if that creature has power 4 or greater, it gains first strike until end of turn."

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        val creature = target("creature", Targets.Creature)
        effect = Effects.ModifyStats(1, 1, creature)
            .then(
                ConditionalEffect(
                    condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature.powerAtLeast(4)),
                    effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature)
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "232"
        artist = "Jarel Threat"
        flavorText = "The greatest traveler and huntsman of this age of the world."
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54461d61-a745-467f-9fbe-b5e7a8edbdbf.jpg?1686970082"
    }
}
