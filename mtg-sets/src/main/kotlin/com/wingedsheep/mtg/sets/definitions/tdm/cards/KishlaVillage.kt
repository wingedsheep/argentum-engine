package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Kishla Village — Tarkir: Dragonstorm #259
 * Land
 *
 * This land enters tapped unless you control an Island or a Swamp.
 * {T}: Add {G}.
 * {3}{G}, {T}: Surveil 2.
 *
 * The conditional enters-tapped is the check-land replacement [EntersTapped] gated on
 * controlling an Island or a Swamp (same shape as Isolated Chapel). {T}: Add {G} is a mana
 * ability; the surveil ability is a non-mana activated ability ({3}{G} + tap) composed from
 * the atomic [LibraryPatterns.surveil] pipeline. The surveil ability has no timing restriction,
 * so it can be activated at instant speed.
 */
val KishlaVillage = card("Kishla Village") {
    typeLine = "Land"
    colorIdentity = "G"
    oracleText = "This land enters tapped unless you control an Island or a Swamp.\n" +
        "{T}: Add {G}.\n" +
        "{3}{G}, {T}: Surveil 2. (Look at the top two cards of your library, then put any number " +
        "of them into your graveyard and the rest on top of your library in any order.)"

    replacementEffect(EntersTapped(
        unlessCondition = Conditions.Any(
            Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Island")),
            Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Swamp"))
        )
    ))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Composite(
            listOf(
                AbilityCost.Mana(ManaCost.parse("{3}{G}")),
                AbilityCost.Tap
            )
        )
        effect = LibraryPatterns.surveil(2)
        description = "Surveil 2."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "259"
        artist = "Bruce Brenneise"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f0ff90d-7312-44df-afc5-29c768fa7758.jpg?1743205023"
    }
}
