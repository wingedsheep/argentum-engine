package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Farewell — Kamigawa: Neon Dynasty #13 (canonical printing)
 * {4}{W}{W} · Sorcery
 *
 * Choose one or more —
 * • Exile all artifacts.
 * • Exile all creatures.
 * • Exile all enchantments.
 * • Exile all graveyards.
 *
 * "One or more" is `chooseCount = 4, minChooseCount = 1` — any non-empty subset, no mode twice.
 * Chosen modes resolve in printed order (per the ruling), so an artifact creature exiled by the
 * first mode is simply gone by the time the second gathers its creatures. Each mode is the
 * Gather -> exile pipeline the battlefield sweeps and Rest in Peace (every graveyard) use.
 */
val Farewell = card("Farewell") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Choose one or more —\n" +
        "• Exile all artifacts.\n" +
        "• Exile all creatures.\n" +
        "• Exile all enchantments.\n" +
        "• Exile all graveyards."

    spell {
        modal(chooseCount = 4, minChooseCount = 1) {
            mode("Exile all artifacts.") {
                effect = Effects.Pipeline {
                    exile(gather(GameObjectFilter.Artifact))
                }
            }
            mode("Exile all creatures.") {
                effect = Effects.Pipeline {
                    exile(gather(GameObjectFilter.Creature))
                }
            }
            mode("Exile all enchantments.") {
                effect = Effects.Pipeline {
                    exile(gather(GameObjectFilter.Enchantment))
                }
            }
            mode("Exile all graveyards.") {
                effect = Effects.Pipeline {
                    exile(gather(CardSource.FromZone(Zone.GRAVEYARD, Player.Each, GameObjectFilter.Any)))
                }
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1068723-d1ef-4007-97d9-b10dccdbade4.jpg?1783923923"
        ruling("2022-02-18", "If you choose more than one mode for Farewell, you perform the actions in the order written.")
    }
}
