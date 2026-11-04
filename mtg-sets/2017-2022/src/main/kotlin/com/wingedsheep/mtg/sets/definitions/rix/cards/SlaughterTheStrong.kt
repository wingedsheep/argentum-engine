package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Slaughter the Strong
 * {1}{W}{W}
 * Sorcery
 * Each player chooses any number of creatures they control with total power 4 or less, then
 * sacrifices all other creatures they control.
 *
 * Same shape as Destined Confrontation: per player (APNAP), gather their creatures, keep a subset
 * capped by [SelectionRestriction.TotalPowerAtMost] (projected power), sacrifice the remainder.
 */
val SlaughterTheStrong = card("Slaughter the Strong") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Each player chooses any number of creatures they control with total power 4 or " +
        "less, then sacrifices all other creatures they control."

    spell {
        effect = Effects.ForEachPlayer(
            players = Player.Each,
            Effects.Pipeline {
                val creatures = gather(
                    CardSource.ControlledPermanents(
                        player = Player.You,
                        filter = GameObjectFilter.Creature
                    )
                )
                val (_, sacrificed) = chooseAnyNumberSplit(
                    from = creatures,
                    restrictions = listOf(SelectionRestriction.TotalPowerAtMost(4)),
                    selectedLabel = "Keep",
                    remainderLabel = "Sacrifice",
                    prompt = "Choose any number of creatures you control with total power 4 or less. " +
                        "The rest are sacrificed.",
                    useTargetingUI = true,
                    alwaysPrompt = true
                )
                sacrifice(sacrificed)
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Randy Vargas"
        flavorText = "\"Cut the beasts out from under them and their strength is nothing.\"\n—Vona of Iedo, Butcher of Magan"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/4217ab21-181e-4c32-97c3-d8bd441287e0.jpg?1783935331"
    }
}
