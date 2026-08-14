package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cruel Somnophage // Can't Wake Up
 * {1}{B}
 * Creature — Nightmare
 * * / *
 *
 * Cruel Somnophage's power and toughness are each equal to the number of creature cards in all
 * graveyards.
 *
 * Adventure: Can't Wake Up — {1}{U}, Sorcery — Adventure
 * Target player mills four cards.
 *
 * The star-over-star body is a characteristic-defining ability (CR 604.3): `dynamicStats` wires the
 * same [com.wingedsheep.sdk.scripting.values.DynamicAmount] into base power and base toughness, so
 * it applies in Layer 7a and — per the card's ruling — functions in every zone, not just the
 * battlefield. [Player.Each] makes the count span *all* graveyards rather than only the
 * controller's.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 * caster cast it as the creature spell while it remains in exile.)
 */
val CruelSomnophage = card("Cruel Somnophage") {
    manaCost = "{1}{B}"
    colorIdentity = "BU"
    typeLine = "Creature — Nightmare"
    dynamicStats(
        DynamicAmounts.zone(Player.Each, Zone.GRAVEYARD, GameObjectFilter.Creature).count()
    )
    oracleText = "Cruel Somnophage's power and toughness are each equal to the number of creature " +
        "cards in all graveyards."

    adventure("Can't Wake Up") {
        manaCost = "{1}{U}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Target player mills four cards. (Then exile this card. You may cast the " +
            "creature later from exile.)"
        spell {
            target("target player", Targets.Player)
            effect = Patterns.Library.mill(4, EffectTarget.ContextTarget(0))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39b11ff0-9946-4337-86fb-42e967f3d2e4.jpg?1783915066"
        ruling(
            "2023-09-01",
            "The ability that defines Cruel Somnophage's power and toughness functions in all " +
                "zones, not just the battlefield."
        )
    }
}
