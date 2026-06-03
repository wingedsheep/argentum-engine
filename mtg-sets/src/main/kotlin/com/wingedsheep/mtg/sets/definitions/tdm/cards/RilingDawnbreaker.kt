package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Riling Dawnbreaker // Signaling Roar — Tarkir: Dragonstorm #21
 * {4}{W} · Creature — Dragon · 3/4
 *
 * Flying, vigilance
 * At the beginning of combat on your turn, another target creature you control gets +1/+0
 * until end of turn.
 *
 * Omen: Signaling Roar — {1}{W}, Sorcery — Omen
 * Create a 2/2 white Soldier creature token. (Then shuffle this card into its owner's library.)
 *
 * (Omen, Tarkir: Dragonstorm: casting the Omen face shuffles this card into its owner's library
 * on resolution instead of putting it in the graveyard. From every zone other than the stack the
 * card is just the Dragon — see [com.wingedsheep.sdk.model.CardLayout.OMEN].)
 */
val RilingDawnbreaker = card("Riling Dawnbreaker") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dragon"
    power = 3
    toughness = 4
    oracleText = "Flying, vigilance\n" +
        "At the beginning of combat on your turn, another target creature you control gets +1/+0 until end of turn."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.BeginCombat
        val t = target(
            "another target creature you control",
            TargetCreature(filter = TargetFilter.OtherCreatureYouControl)
        )
        effect = Effects.ModifyStats(power = 1, toughness = 0, target = t)
    }

    // Omen: Signaling Roar — Sorcery. Create a 2/2 white Soldier creature token.
    omen("Signaling Roar") {
        manaCost = "{1}{W}"
        typeLine = "Sorcery — Omen"
        oracleText = "Create a 2/2 white Soldier creature token. (Then shuffle this card into its owner's library.)"
        spell {
            effect = CreateTokenEffect(
                count = 1,
                power = 2,
                toughness = 2,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Soldier"),
                name = "Soldier",
                imageUri = "https://cards.scryfall.io/normal/front/7/d/7ddd5153-2d16-4a4e-b9bc-f20313d322da.jpg?1743176203"
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "21"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/312f7072-3bf8-449f-bfb7-93727ef26c66.jpg?1743204036"
    }
}
