package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Croaking Counterpart — Innistrad: Midnight Hunt #215
 * {1}{G}{U} · Sorcery
 *
 * Create a token that's a copy of target non-Frog creature, except it's a 1/1 green Frog.
 * Flashback {3}{G}{U}
 *
 * "Except it's a 1/1 green Frog" is a set of copy exceptions (CR 707.9b), as on Preston, the
 * Vanisher: base P/T becomes 1/1, green replaces the copied colors, and Frog replaces the copied
 * creature types. Everything else (card types, supertypes, abilities, name) is copied.
 */
val CroakingCounterpart = card("Croaking Counterpart") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Sorcery"
    oracleText = "Create a token that's a copy of target non-Frog creature, except it's a 1/1 green Frog.\n" +
        "Flashback {3}{G}{U} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        val creature = target(TargetFilter.Creature.notSubtype(Subtype.FROG))
        effect = Effects.CreateTokenCopyOfTarget(
            target = creature,
            exceptions = CopyExceptions(
                powerOverride = 1,
                toughnessOverride = 1,
                overrideColors = setOf(Color.GREEN),
                overrideSubtypes = setOf(Subtype.FROG)
            )
        )
    }

    keywordAbility(KeywordAbility.flashback("{3}{G}{U}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "215"
        artist = "Yeong-Hao Han"
        flavorText = "Frog is the sincerest form of flattery."
        imageUri = "https://cards.scryfall.io/normal/front/9/8/9847b22f-6660-4654-91a9-e0adb8606bab.jpg?1783925565"
    }
}
