package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.webSlinging
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Silk, Web Weaver — Marvel's Spider-Man #145
 * {2}{G}{W} · Legendary Creature — Spider Human Hero · 3/5
 *
 * Web-slinging {1}{G}{W}
 * Whenever you cast a creature spell, create a 1/1 green and white Human Citizen creature token.
 * {3}{G}{W}: Creatures you control get +2/+2 and gain vigilance until end of turn.
 */
val SilkWebWeaver = card("Silk, Web Weaver") {
    manaCost = "{2}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Spider Human Hero"
    power = 3
    toughness = 5
    oracleText = "Web-slinging {1}{G}{W} (You may cast this spell for {1}{G}{W} if you also return " +
        "a tapped creature you control to its owner's hand.)\n" +
        "Whenever you cast a creature spell, create a 1/1 green and white Human Citizen creature token.\n" +
        "{3}{G}{W}: Creatures you control get +2/+2 and gain vigilance until end of turn."

    webSlinging("{1}{G}{W}")

    triggeredAbility {
        trigger = Triggers.YouCastCreature
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN, Color.WHITE),
            creatureTypes = setOf("Human", "Citizen")
        )
        description = "Whenever you cast a creature spell, create a 1/1 green and white Human " +
            "Citizen creature token."
    }

    activatedAbility {
        cost = Costs.Mana("{3}{G}{W}")
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.Composite(
                Effects.ModifyStats(2, 2, EffectTarget.Self),
                Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.Self)
            )
        )
        description = "Creatures you control get +2/+2 and gain vigilance until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "145"
        artist = "Carissa Susilo"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/588dc8d9-6ce0-4bd7-afbd-84bb251fdcb1.jpg?1783905311"
    }
}
