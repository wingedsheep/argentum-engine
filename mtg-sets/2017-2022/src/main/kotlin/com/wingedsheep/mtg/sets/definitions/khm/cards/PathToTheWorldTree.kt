package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Path to the World Tree — Kaldheim #186 (canonical printing)
 * {1}{G} · Enchantment
 *
 * When this enchantment enters, search your library for a basic land card, reveal it, put it
 * into your hand, then shuffle.
 * {2}{W}{U}{B}{R}{G}, Sacrifice this enchantment: You gain 2 life and draw two cards. Target
 * opponent loses 2 life. This enchantment deals 2 damage to up to one target creature. You create
 * a 2/2 green Bear creature token.
 *
 * The opponent target
 * is mandatory, so the ability always has a target and only fizzles when every chosen target is
 * illegal on resolution (the ruling below).
 */
val PathToTheWorldTree = card("Path to the World Tree") {
    manaCost = "{1}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, search your library for a basic land card, reveal it, " +
        "put it into your hand, then shuffle.\n" +
        "{2}{W}{U}{B}{R}{G}, Sacrifice this enchantment: You gain 2 life and draw two cards. Target " +
        "opponent loses 2 life. This enchantment deals 2 damage to up to one target creature. You " +
        "create a 2/2 green Bear creature token."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}{U}{B}{R}{G}"), Costs.SacrificeSelf)
        val opponent = target(Targets.Opponent)
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.GainLife(2) then
            Effects.DrawCards(2) then
            Effects.LoseLife(2, opponent) then
            Effects.DealDamage(2, creature) then
            Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Bear"),
                imageUri = "https://cards.scryfall.io/normal/front/e/c/ec53abaa-b316-496d-9351-318a9dd9de4d.jpg?1783928079"
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "186"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/606da75d-382a-47a8-9739-644438594700.jpg?1783928208"
        ruling(
            "2021-02-05",
            "The last ability of Path to the World Tree always targets an opponent and may target a creature. " +
                "As the ability tries to resolve, if all its targets are illegal, the ability won't resolve and " +
                "none of its effects will happen. You won't do any of the cool stuff that ability tells you to do."
        )
    }
}
