package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Connive // Concoct (GRN 222) — split-layout spell (CR 709).
 *
 * Connive {2}{U/B}{U/B} — Sorcery
 *   Gain control of target creature with power 2 or less.
 *
 * Concoct {3}{U}{B} — Sorcery
 *   Surveil 3, then return a creature card from your graveyard to the battlefield.
 */
val ConniveConcoct = card("Connive // Concoct") {
    layout = CardLayout.SPLIT
    colorIdentity = "UB"

    face("Connive") {
        manaCost = "{2}{U/B}{U/B}"
        typeLine = "Sorcery"
        oracleText = "Gain control of target creature with power 2 or less."

        spell {
            val creature = target(TargetFilter.Creature.powerAtMost(2))
            effect = Effects.GainControl(creature)
        }
    }

    face("Concoct") {
        manaCost = "{3}{U}{B}"
        typeLine = "Sorcery"
        oracleText = "Surveil 3, then return a creature card from your graveyard to the battlefield."

        spell {
            // The creature card is chosen after surveilling, so one just surveilled away is eligible.
            effect = Effects.Surveil(3) then Effects.Pipeline {
                val creatures = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature))
                val chosen = chooseExactly(
                    1,
                    from = creatures,
                    prompt = "Choose a creature card to return to the battlefield"
                )
                move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/890ac54c-6fd7-4e46-8ce4-8926c6975f60.jpg?1783934112"
        ruling("2018-10-05", "No player may take actions between the time you surveil and the time you return a creature card to the battlefield.")
        ruling("2018-10-05", "The creature card you return with Concoct may be one that you just surveilled into your graveyard.")
        ruling("2018-10-05", "Connive's effect lasts indefinitely. It doesn't wear off during the cleanup step, and it doesn't expire if the creature's power becomes greater after Connive has resolved.")
    }
}
