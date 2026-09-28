package com.wingedsheep.mtg.sets.definitions.rav.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sins of the Past — Ravnica: City of Guilds #106
 * {4}{B}{B} · Sorcery
 *
 * Until end of turn, you may cast target instant or sorcery card from your graveyard without
 * paying its mana cost. If that spell would be put into your graveyard, exile it instead. Exile
 * Sins of the Past.
 *
 * The card stays in the graveyard: [Effects.GrantFreeCastTargetFromExile] writes a
 * `MayPlayPermission` plus a free-cast stamp on it wherever it is, and the cast-from-zone path
 * honours those in the graveyard too (Malcolm, Alluring Scoundrel). The permission is not
 * `permanent`, so end-of-turn cleanup removes it — "until end of turn". `insteadOfGraveyard = EXILE`
 * stamps `AfterResolveDestinationComponent`, which the stack resolver honours on resolution *and*
 * on counter/fizzle — the first ruling ("never gets put back into your graveyard, even if it's
 * countered"). `selfExile()` is "Exile Sins of the Past".
 */
val SinsOfThePast = card("Sins of the Past") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Until end of turn, you may cast target instant or sorcery card from your graveyard " +
        "without paying its mana cost. If that spell would be put into your graveyard, exile it " +
        "instead. Exile Sins of the Past."

    spell {
        val target = target(TargetFilter.InstantOrSorceryInGraveyard.ownedByYou())
        effect = Effects.GrantFreeCastTargetFromExile(
            target = target,
            insteadOfGraveyard = AfterResolveDestination.EXILE
        )
        selfExile()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "106"
        artist = "Jeremy Jarvis"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c518792-a50f-40b1-b6fb-674432093b6a.jpg?1783943661"
    }
}
