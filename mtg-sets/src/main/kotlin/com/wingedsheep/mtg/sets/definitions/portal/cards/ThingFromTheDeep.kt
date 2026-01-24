package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SacrificeUnlessSacrificePermanentEffect
import com.wingedsheep.sdk.trigger.OnAttack

/**
 * Thing from the Deep
 * {6}{U}{U}{U}
 * Creature — Leviathan
 * 9/9
 * Whenever Thing from the Deep attacks, sacrifice it unless you sacrifice an Island.
 */
val ThingFromTheDeep = card("Thing from the Deep") {
    manaCost = "{6}{U}{U}{U}"
    typeLine = "Creature — Leviathan"
    subtypes = listOf(Subtype.LEVIATHAN)
    power = 9
    toughness = 9

    triggeredAbility {
        trigger = OnAttack()
        effect = SacrificeUnlessSacrificePermanentEffect("Island")
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "73"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0a5a7a6-0e7e-4a3f-8b26-dbf9862f0fce.jpg"
    }
}
