package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Bushwhack
 * {G}
 * Sorcery
 * Choose one —
 * • Search your library for a basic land card, reveal it, put it into your hand, then shuffle.
 * • Target creature you control fights target creature you don't control. (Each deals damage equal to its power to the other.)
 */
val Bushwhack = card("Bushwhack") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Search your library for a basic land card, reveal it, put it into your hand, then shuffle.\n" +
        "• Target creature you control fights target creature you don't control. (Each deals damage equal to its power to the other.)"

    spell {
        modal(chooseCount = 1) {
            mode("Search your library for a basic land card, reveal it, put it into your hand, then shuffle") {
                effect = EffectPatterns.searchLibrary(
                    filter = GameObjectFilter.BasicLand,
                    count = 1,
                    destination = SearchDestination.HAND,
                    reveal = true,
                    shuffleAfter = true
                )
            }
            mode("Target creature you control fights target creature you don't control") {
                val yourCreature = target("creature you control", TargetCreature(
                    filter = TargetFilter(GameObjectFilter.Creature.youControl())
                ))
                val theirCreature = target("creature you don't control", TargetCreature(
                    filter = TargetFilter(GameObjectFilter.Creature.opponentControls())
                ))
                effect = Effects.Fight(yourCreature, theirCreature)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "174"
        artist = "Artur Nakhodkin"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/712a0640-d9c8-46fc-b38b-bf20a40fa902.jpg?1674421510"
    }
}
