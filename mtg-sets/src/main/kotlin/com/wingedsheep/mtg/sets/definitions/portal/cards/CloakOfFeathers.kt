package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Cloak of Feathers
 * {U}
 * Sorcery
 * Target creature gains flying until end of turn. Draw a card.
 */
val CloakOfFeathers = card("Cloak of Feathers") {
    manaCost = "{U}"
    typeLine = "Sorcery"

    spell {
        target = TargetCreature()
        effect = CompositeEffect(
            listOf(
                GrantKeywordUntilEndOfTurnEffect(Keyword.FLYING, EffectTarget.ContextTarget(0)),
                DrawCardsEffect(1, EffectTarget.Controller)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "44"
        artist = "Rebecca Guay"
        flavorText = "\"A thousand feathers from a thousand birds, sewn with magic and song.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/7/9746790c-a426-4135-8c9d-afb82a0c26b8.jpg"
    }
}
