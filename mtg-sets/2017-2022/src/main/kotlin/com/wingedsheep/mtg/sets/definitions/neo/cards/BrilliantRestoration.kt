package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Brilliant Restoration — Kamigawa: Neon Dynasty #7 (canonical printing)
 * {3}{W}{W}{W}{W} · Sorcery
 *
 * Return all artifact and enchantment cards from your graveyard to the battlefield.
 *
 * Open the Vaults shape restricted to your own graveyard: one gather-then-move batch, so every
 * returned card enters simultaneously and a returning Aura chooses its host among permanents
 * already on the battlefield (an Aura with no legal host stays in the graveyard).
 */
val BrilliantRestoration = card("Brilliant Restoration") {
    manaCost = "{3}{W}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Return all artifact and enchantment cards from your graveyard to the battlefield."

    spell {
        effect = Effects.Pipeline {
            val returning = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.You,
                    filter = GameObjectFilter.Artifact or GameObjectFilter.Enchantment
                )
            )
            move(returning, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Wylie Beckert"
        flavorText = "\"We have done our best to heal this land together, but now I must entrust the future " +
            "to my sister. She will carry our dreams onward into the sunrise.\"\n—Michiko Konda's journal"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64f7a506-0aa1-4c2d-b322-e0e9179d05fb.jpg?1783923925"
    }
}
