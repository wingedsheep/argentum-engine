package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones

/**
 * Khan, Vengeful Heart — Star Trek #119
 * {2}{B}{B} · Legendary Creature — Human Warrior · 5/3
 *
 * When Khan enters, you may sacrifice another creature or artifact. If you do, draw two cards.
 * You may cast this card from exile.
 *
 * The ETB is Automated Warfare System's optional sacrifice cost ([Effects.MayPay] with
 * `excludeSource`); the cast permission is Squee, the Immortal's [MayCastSelfFromZones].
 */
val KhanVengefulHeart = card("Khan, Vengeful Heart") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Warrior"
    power = 5
    toughness = 3
    oracleText = "When Khan enters, you may sacrifice another creature or artifact. If you do, draw two cards.\n" +
        "You may cast this card from exile."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.MayPay(
            Effects.SacrificeOwn(GameObjectFilter.CreatureOrArtifact, excludeSource = true),
            then = Effects.DrawCards(2)
        )
        description = "When Khan enters, you may sacrifice another creature or artifact. If you do, draw two cards."
    }

    staticAbility {
        ability = MayCastSelfFromZones(zones = listOf(Zone.EXILE))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "119"
        artist = "Sam Kennedy"
        flavorText = "\"Surely, I have made my meaning plain. I mean to avenge myself upon you, Admiral.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6f47b34-02fb-41d2-978a-a6d0a47ee8ba.jpg?1792551016"
    }
}
