package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Automated Warfare System — Star Trek #93
 * {2}{B} · Artifact Creature — Construct · 2/2
 *
 * Flying
 * Whenever this creature attacks, you may sacrifice another creature or artifact. If you do,
 * draw a card.
 *
 * Kill-Zone Acrobat's attack trigger: an [Effects.MayPay] whose optional cost sacrifices another
 * creature or artifact (`excludeSource`), so the card is drawn only if you do.
 */
val AutomatedWarfareSystem = card("Automated Warfare System") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Construct"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Whenever this creature attacks, you may sacrifice another creature or artifact. If you do, draw a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            Effects.SacrificeOwn(GameObjectFilter.CreatureOrArtifact, excludeSource = true),
            then = Effects.DrawCards(1)
        )
        description = "Whenever this creature attacks, you may sacrifice another creature or artifact. " +
            "If you do, draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Pierre Raveneau"
        flavorText = "Peace through superior firepower.\n—Minosian motto, pre-extinction"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3deae89b-b207-487b-9e82-1fa7fcd7854b.jpg?1785981442"
    }
}
