package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tal Shiar Operative
 * {1}{B}
 * Creature — Romulan Officer Spy
 * 2/2
 *
 * Whenever this creature attacks, choose one —
 * • Target creature you control gains deathtouch until end of turn.
 * • Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)
 */
val TalShiarOperative = card("Tal Shiar Operative") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Romulan Officer Spy"
    colorIdentity = "B"
    oracleText = "Whenever this creature attacks, choose one —\n" +
        "• Target creature you control gains deathtouch until end of turn.\n" +
        "• Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"
    power = 2
    toughness = 2
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = ModalEffect.chooseOne(
            mode("Target creature you control gains deathtouch until end of turn.") {
                val creature = target(TargetFilter.CreatureYouControl)
                effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, creature)
            },
            Mode(effect = Effects.Surveil(1), description = "Surveil 1.")
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "131"
        artist = "Irina Nordsol"
        flavorText = "\"What gave me away? My emotions or the disruptor?\""
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebe9c7bd-5528-41fd-9c51-2306086c7887.jpg"
    }
}
