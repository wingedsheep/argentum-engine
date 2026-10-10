package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop

/**
 * Aesi, Tyrant of Gyre Strait
 * {4}{G}{U}
 * Legendary Creature — Serpent
 * 5/5
 * You may play an additional land on each of your turns.
 * Landfall — Whenever a land you control enters, you may draw a card.
 *
 * The extra land drop is the same cumulative [GrantAdditionalLandDrop] static Exploration uses;
 * landfall is `Triggers.a(Land.youControl()).enters()` with a bare "you may" around the draw.
 */
val AesiTyrantOfGyreStrait = card("Aesi, Tyrant of Gyre Strait") {
    manaCost = "{4}{G}{U}"
    colorIdentity = "UG"
    typeLine = "Legendary Creature — Serpent"
    power = 5
    toughness = 5
    oracleText = "You may play an additional land on each of your turns.\n" +
        "Landfall — Whenever a land you control enters, you may draw a card."

    staticAbility {
        ability = GrantAdditionalLandDrop(count = 1)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.May(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "365"
        artist = "Viktor Titov"
        flavorText = "\"To sail through those waters is to offer oneself as tribute.\"\n—Captain Hoyrik"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d607b003-6b48-429c-a7fd-45b8dd1bb4f9.jpg?1783928734"
        ruling("2020-11-10", "Aesi's ability is cumulative with other effects that let you play additional lands, such as the one from Exploration.")
        ruling("2024-11-08", "A landfall ability triggers whenever a land you control enters for any reason. It triggers whenever you play a land, as well as whenever a spell or ability puts a land onto the battlefield under your control.")
        ruling("2024-11-08", "A landfall ability doesn't trigger if a permanent already on the battlefield becomes a land.")
    }
}
