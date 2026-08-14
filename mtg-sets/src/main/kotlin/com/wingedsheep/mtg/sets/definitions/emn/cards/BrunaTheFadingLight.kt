package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Bruna, the Fading Light
 * {5}{W}{W}
 * Legendary Creature — Angel Horror
 * 5/7
 *
 * When you cast this spell, you may return target Angel or Human creature card from your
 * graveyard to the battlefield.
 * Flying, vigilance
 * (Melds with Gisela, the Broken Blade.)
 */
val BrunaTheFadingLight = card("Bruna, the Fading Light") {
    manaCost = "{5}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Angel Horror"
    power = 5
    toughness = 7
    oracleText = "When you cast this spell, you may return target Angel or Human creature card " +
        "from your graveyard to the battlefield.\nFlying, vigilance\n" +
        "(Melds with Gisela, the Broken Blade.)"

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.WhenYouCastThisSpell()
        optional = true
        val creature = target(
            "target Angel or Human creature card in your graveyard",
            TargetObject(
                filter = TargetFilter(
                    baseFilter = GameObjectFilter.Creature
                        .withAnySubtype("Angel", "Human")
                        .ownedByYou(),
                    zone = Zone.GRAVEYARD,
                ),
            ),
        )
        effect = Effects.PutOntoBattlefield(creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Clint Cearley"
        flavorText = "She now sees only Emrakul's visions."
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27907985-b5f6-4098-ab43-15a0c2bf94d5.jpg?1783937523"
    }
}
