package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Commencement of Festivities — Kaladesh #148
 * {1}{G} · Instant
 *
 * Prevent all combat damage that would be dealt to players this turn.
 *
 * "To players" is a combat-only prevention shield on each player: `ForEachPlayer` rebinds
 * `Player.You` to the iterated player, so `PlayerRef(Player.You)` names that player as the
 * shield's recipient. Combat damage to creatures and planeswalkers is still dealt. (A plain
 * `EffectTarget.Controller` target would lower to the Fog-style global shield instead.)
 */
val CommencementOfFestivities = card("Commencement of Festivities") {
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
        collectorNumber = "148"
        artist = "Zack Stella"
        flavorText = "Never before have there been so many brilliant minds and wondrous inventions in one place. Denizens from every corner of Kaladesh will flock to the once-in-a-lifetime Inventors' Fair."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1724602-2ae1-40a4-aeb0-51a483a6948c.jpg"
    }
}
