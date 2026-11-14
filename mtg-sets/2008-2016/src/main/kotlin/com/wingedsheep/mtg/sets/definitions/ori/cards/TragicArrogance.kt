package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Tragic Arrogance — Magic Origins #38
 * {3}{W}{W} · Sorcery
 *
 * For each player, you choose from among the permanents that player controls an artifact, a
 * creature, an enchantment, and a planeswalker. Then each player sacrifices all other nonland
 * permanents they control.
 *
 * Mythos of Snapdax's "you choose for each player" shape: for every player (starting with you) the
 * caster ([Chooser.SourceController]) makes four separate one-of choices over that player's
 * permanents, so a multi-typed permanent can be spared as several types at once (ruling). Unlike
 * Cataclysmic Gearhulk the choice is "from among the permanents", not "nonland permanents" — an
 * artifact land is a legal choice for the artifact — while only nonland permanents are sacrificed.
 * Nothing is targeted, and every unchosen nonland permanent is sacrificed together once all the
 * choices are made.
 */
val TragicArrogance = card("Tragic Arrogance") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "For each player, you choose from among the permanents that player controls an artifact, " +
        "a creature, an enchantment, and a planeswalker. Then each player sacrifices all other nonland " +
        "permanents they control."

    spell {
        effect = Effects.Pipeline {
            val categories = listOf(
                GameObjectFilter.Artifact to "an artifact",
                GameObjectFilter.Creature to "a creature",
                GameObjectFilter.Enchantment to "an enchantment",
                GameObjectFilter.Planeswalker to "a planeswalker",
            )
            val collected = forEachPlayerCollecting(Player.EachStartingWithYou) {
                val permanents = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Permanent))
                val kept = categories.map { (category, label) ->
                    chooseExactly(
                        1,
                        from = filter(permanents, category),
                        chooser = Chooser.SourceController,
                        prompt = "Choose $label this player keeps. Their other nonland permanents are sacrificed.",
                        selectedLabel = "Keep",
                        remainderLabel = "Sacrifice",
                        useTargetingUI = true,
                    )
                }
                listOf(filter(permanents, GameObjectFilter.NonlandPermanent)) + kept
            }
            val atRisk = collected.first()
            val doomed = collected.drop(1).fold(atRisk) { remaining, kept -> exclude(remaining, kept) }
            sacrifice(doomed)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "38"
        artist = "Winona Nelson"
        flavorText = "The spear thrown by Kytheon's own hand was the weapon that felled his friends."
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16acf619-0f45-4985-a8ec-074a4ec33fa7.jpg?1783938356"
        ruling("2015-06-22", "None of the chosen permanents are targets of Tragic Arrogance.")
        ruling(
            "2015-06-22",
            "If a permanent has more than one of the affected types, it can count for any of them. For example, " +
                "you could choose an artifact creature as the artifact you're sparing, another creature as the " +
                "creature, and an enchantment creature as the enchantment. Similarly, you could choose an " +
                "enchantment creature as both the creature and the enchantment that you're sparing, even if the " +
                "player controls another creature and/or another enchantment."
        )
    }
}
