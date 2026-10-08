package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Skystrike Officer
 * {2}{U}
 * Creature — Human Soldier
 * 2/3
 * Flying
 * Whenever this creature attacks, create a 1/1 colorless Soldier artifact creature token.
 * Tap three untapped Soldiers you control: Draw a card.
 *
 * The tap cost's filter is the bare tribal noun "Soldiers" — a permanent filter with the subtype;
 * `CostAtom.TapPermanents` already restricts to untapped permanents you control. The officer can
 * tap itself to help pay it.
 */
val SkystrikeOfficer = card("Skystrike Officer") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever this creature attacks, create a 1/1 colorless Soldier artifact creature token.\n" +
        "Tap three untapped Soldiers you control: Draw a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Soldier"),
            artifactToken = true
        )
    }

    activatedAbility {
        cost = Costs.TapPermanents(3, GameObjectFilter.Permanent.withSubtype("Soldier"))
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "62"
        artist = "Diego Gisbert"
        flavorText = "\"Landing zone is clear. Activate and deploy!\""
        imageUri = "https://cards.scryfall.io/normal/front/9/2/925fa343-d9c3-4ed9-bd1f-aaa31c0badd7.jpg"
    }
}
