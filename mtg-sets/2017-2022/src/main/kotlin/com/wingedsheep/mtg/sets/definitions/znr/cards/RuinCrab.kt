package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ruin Crab
 * {U}
 * Creature — Crab
 * 0/3
 * Landfall — Whenever a land you control enters, each opponent mills three cards.
 *
 * Landfall trigger (`Triggers.a(Land.youControl()).enters()`) into the mill recipe with
 * [Player.EachOpponent] as its player argument — one pipeline mills every opponent.
 */
val RuinCrab = card("Ruin Crab") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Crab"
    power = 0
    toughness = 3
    oracleText = "Landfall — Whenever a land you control enters, each opponent mills three cards. " +
        "(To mill a card, a player puts the top card of their library into their graveyard.)"

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Patterns.Library.mill(3, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Landfall — Whenever a land you control enters, each opponent mills three cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "75"
        artist = "Simon Dominic"
        flavorText = "To an explorer, a priceless treasure. To the crab, a rock to call home."
        imageUri = "https://cards.scryfall.io/normal/front/7/3/738ce273-c938-42d2-83b2-c4f5f4000b0b.jpg?1783929390"
        ruling("2024-11-08", "A landfall ability triggers whenever a land you control enters for any reason. It triggers whenever you play a land, as well as whenever a spell or ability puts a land onto the battlefield under your control.")
        ruling("2024-11-08", "A landfall ability doesn't trigger if a permanent already on the battlefield becomes a land.")
    }
}
