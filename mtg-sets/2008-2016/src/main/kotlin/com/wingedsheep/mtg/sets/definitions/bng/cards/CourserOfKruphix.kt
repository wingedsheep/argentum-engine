package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary
import com.wingedsheep.sdk.scripting.RevealTopOfLibrary

/**
 * Courser of Kruphix
 * {1}{G}{G}
 * Enchantment Creature — Centaur
 * 2/4
 * Play with the top card of your library revealed.
 * You may play lands from the top of your library.
 * Landfall — Whenever a land you control enters, you gain 1 life.
 *
 * The public reveal is [RevealTopOfLibrary]; the lands-only play permission is
 * [PlayLandsAndCastFilteredFromTopOfLibrary] with `spellFilter = null` (no spell is castable),
 * the same pair Ka-Zar of the Savage Land uses minus the reveal. A land played from the top still
 * uses the turn's land play.
 */
val CourserOfKruphix = card("Courser of Kruphix") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Centaur"
    power = 2
    toughness = 4
    oracleText = "Play with the top card of your library revealed.\n" +
        "You may play lands from the top of your library.\n" +
        "Landfall — Whenever a land you control enters, you gain 1 life."

    staticAbility { ability = RevealTopOfLibrary }

    staticAbility {
        ability = PlayLandsAndCastFilteredFromTopOfLibrary(spellFilter = null)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.GainLife(1)
        description = "Landfall — Whenever a land you control enters, you gain 1 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "119"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da5a807f-58e8-4d92-a61c-47bb9b28977f.jpg?1783939536"
        ruling(
            "2021-03-19",
            "Playing a land from the top of your library counts as your land play for the turn.",
        )
        ruling(
            "2021-03-19",
            "Courser of Kruphix doesn't change when you can play lands. You can do so only during your " +
                "main phase when you have priority and the stack is empty.",
        )
    }
}
