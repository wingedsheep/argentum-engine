package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sam's Desperate Rescue
 * {B}
 * Sorcery
 *
 * Return target creature card from your graveyard to your hand. The Ring tempts you.
 */
val SamsDesperateRescue = card("Sam's Desperate Rescue") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return target creature card from your graveyard to your hand. The Ring tempts you."

    spell {
        val t = target("target creature card in your graveyard", Targets.CreatureCardInYourGraveyard)
        effect = Effects.ReturnToHand(t).then(Effects.TheRingTemptsYou())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Lixin Yin"
        flavorText = "\"By all the signs, there's a large warrior loose, with an Elf-sword, and an axe as well maybe.\" Gorbag spat. Sam smiled grimly at this description of himself."
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bdd4d735-8cda-47c1-865b-48b51ac8f666.jpg?1686968689"
    }
}
