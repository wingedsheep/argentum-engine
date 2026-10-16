package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Open the Vaults
 * {4}{W}{W}
 * Sorcery
 * Return all artifact and enchantment cards from all graveyards to the battlefield under their
 * owners' control. (Auras with nothing to enchant remain in graveyards.)
 *
 * One batch move with `underOwnersControl`: each returned Aura's owner chooses what it enchants
 * (CR 303.4f), never one of the cards entering alongside it, and an Aura with no legal host stays
 * in the graveyard.
 */
val OpenTheVaults = card("Open the Vaults") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Return all artifact and enchantment cards from all graveyards to the battlefield " +
        "under their owners' control. (Auras with nothing to enchant remain in graveyards.)"

    spell {
        effect = Effects.Pipeline {
            val returning = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.Each,
                    filter = GameObjectFilter.Artifact or GameObjectFilter.Enchantment
                )
            )
            move(returning, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "21"
        artist = "Brian Despain"
        flavorText = "When the vaults are empty, everyone is rich."
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ffb29703-96de-475a-9499-b3cd2a2d6566.jpg?1783942401"
        ruling(
            "2020-08-07",
            "If an Aura is put onto the battlefield without being cast, the Aura's controller-to-be chooses what " +
                "it will enchant as it enters the battlefield. An Aura put onto the battlefield this way doesn't " +
                "target anything (so it could be attached to an opponent's permanent with hexproof, for example), " +
                "but the Aura's enchant ability restricts what it can be attached to. If the Aura can't legally be " +
                "attached to anything, it remains in its current zone."
        )
        ruling(
            "2020-08-07",
            "If an Aura can be returned to the battlefield with Open the Vaults, it must be, even if its owner " +
                "doesn't like what it would enchant."
        )
        ruling(
            "2020-08-07",
            "If multiple players have Auras in their graveyards, the player whose turn it is announces what their " +
                "Auras will enchant, then each other player in turn order does the same, then all artifacts and " +
                "enchantments (including both Auras and non-Auras) enter the battlefield at the same time."
        )
        ruling(
            "2020-08-07",
            "An Aura put onto the battlefield this way can't enchant an artifact or enchantment that's also being " +
                "put onto the battlefield with Open the Vaults."
        )
    }
}
