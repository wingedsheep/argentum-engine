package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glacier Godmaw
 * {5}{G}{G}
 * Creature — Leviathan
 * Trample
 * When this creature enters, create a Lander token. (It's an artifact with "{2}, {T}, Sacrifice this token: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.")
 * Landfall — Whenever a land you control enters, creatures you control get +1/+1 and gain vigilance and haste until end of turn.
 * 6/6
 */
val GlacierGodmaw = card("Glacier Godmaw") {
    manaCost = "{5}{G}{G}"
    typeLine = "Creature — Leviathan"
    power = 6
    toughness = 6
    oracleText = "Trample\nWhen this creature enters, create a Lander token. (It's an artifact with \"{2}, {T}, Sacrifice this token: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.\")\nLandfall — Whenever a land you control enters, creatures you control get +1/+1 and gain vigilance and haste until end of turn."

    // Trample keyword
    keywords(Keyword.TRAMPLE)

    // ETB: create a Lander token
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateLander()
    }

    // Landfall: creatures you control get +1/+1 and gain vigilance and haste until end of turn
    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = ForEachInGroupEffect(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = CompositeEffect(
                listOf(
                    Effects.ModifyStats(+1, +1, EffectTarget.ContextTarget(0)),
                    Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.ContextTarget(0)),
                    Effects.GrantKeyword(Keyword.HASTE, EffectTarget.ContextTarget(0))
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "188"
        artist = "Bruce Brenneise"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3291c51-e963-4970-813d-9a06a47aa71e.jpg?1752947322"
    }
}
