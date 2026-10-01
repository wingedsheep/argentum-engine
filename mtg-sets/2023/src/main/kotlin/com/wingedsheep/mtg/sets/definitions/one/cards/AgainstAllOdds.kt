package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Against All Odds
 * {3}{W}
 * Sorcery
 * Choose one or both —
 * • Exile target artifact or creature you control, then return it to the battlefield under its owner's control.
 * • Return target artifact or creature card with mana value 3 or less from your graveyard to the battlefield.
 */
val AgainstAllOdds = card("Against All Odds") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Choose one or both —\n" +
        "• Exile target artifact or creature you control, then return it to the battlefield under its owner's control.\n" +
        "• Return target artifact or creature card with mana value 3 or less from your graveyard to the battlefield."

    spell {
        // "Choose one or both" is the count (CR 700.2), not a third "both" mode.
        modal(chooseCount = 2, minChooseCount = 1) {
            mode("Exile target artifact or creature you control, then return it to the battlefield") {
                val t = target(TargetFilter((GameObjectFilter.Artifact or GameObjectFilter.Creature).youControl()))
                effect = Effects.Exile(t) then Effects.Move(t, Zone.BATTLEFIELD)
            }
            mode("Return target artifact or creature card with mana value 3 or less from your graveyard to the battlefield") {
                val t = target(
                    TargetFilter(
                        (GameObjectFilter.Artifact or GameObjectFilter.Creature).manaValueAtMost(3).ownedByYou(),
                        zone = Zone.GRAVEYARD,
                    )
                )
                effect = Effects.PutOntoBattlefieldFromGraveyard(t)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "1"
        artist = "Rudy Siswanto"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3cd8dd4e-6892-49d7-8fae-97d04f9f6c84.jpg?1783918086"
    }
}
