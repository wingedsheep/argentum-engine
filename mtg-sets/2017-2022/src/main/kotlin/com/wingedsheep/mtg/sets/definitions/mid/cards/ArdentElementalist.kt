package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ardent Elementalist (Innistrad: Midnight Hunt #128)
 * {3}{R}
 * Creature — Human Shaman
 * 2/1
 * When this creature enters, return target instant or sorcery card from your graveyard to your hand.
 */
val ArdentElementalist = card("Ardent Elementalist") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    oracleText = "When this creature enters, return target instant or sorcery card from your graveyard to your hand."
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.InstantOrSorceryInYourGraveyard)
        effect = Effects.Move(t, Zone.HAND)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "128"
        artist = "Miguel Mercado"
        flavorText = "While the Dawnhart witches focus their magic on nature and community, other covens consort with devils and twist the elements to their will."
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f58592f7-1df5-428d-9dde-e6acd9a5d1d5.jpg"
    }
}
