package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Returned Pastcaller — Strixhaven: School of Mages #224 (canonical printing)
 * {3}{R}{R/W}{W} · Creature — Spirit Cleric · 4/2
 *
 * Flying
 * When this creature enters, return target Spirit, instant, or sorcery card from your graveyard
 * to your hand.
 *
 * Pillardrop Rescuer's ETB with the target widened to a union: any card with the Spirit subtype
 * (creature or kindred) or an instant or sorcery card, owned by you, in your graveyard.
 */
val ReturnedPastcaller = card("Returned Pastcaller") {
    manaCost = "{3}{R}{R/W}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Spirit Cleric"
    oracleText =
        "Flying\n" +
        "When this creature enters, return target Spirit, instant, or sorcery card from your graveyard to your hand."
    power = 4
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val returned = target(
            TargetFilter(
                (GameObjectFilter.Any.withSubtype(Subtype.SPIRIT) or GameObjectFilter.InstantOrSorcery).ownedByYou(),
                zone = Zone.GRAVEYARD,
            )
        )
        effect = Effects.ReturnToHand(returned)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "224"
        artist = "Grzegorz Rutkowski"
        flavorText = "Lorehold spirit statues are not forbidden from raising statues of their own."
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7aa0e762-0fec-4f6b-b3d7-ad8295bf557d.jpg?1783927301"
    }
}
