package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Defend the Hearth — Theros #156
 * {1}{G} · Instant
 *
 * Prevent all combat damage that would be dealt to players this turn.
 *
 * Same shape as Commencement of Festivities: a combat-only prevention shield on each player
 * (`ForEachPlayer` rebinds `Player.You`), so combat damage to creatures and planeswalkers is
 * still dealt.
 */
val DefendTheHearth = card("Defend the Hearth") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Prevent all combat damage that would be dealt to players this turn."

    spell {
        effect = Effects.ForEachPlayer(
            Player.Each,
            Effects.PreventDamage(target = EffectTarget.PlayerRef(Player.You), combatOnly = true)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "156"
        artist = "Raymond Swanland"
        flavorText = "The defenders said not a word. Their shields, spears, and stances were warning enough."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3e4c4c3-db3c-49d4-8080-1c93470f8b4d.jpg?1783939746"
    }
}
