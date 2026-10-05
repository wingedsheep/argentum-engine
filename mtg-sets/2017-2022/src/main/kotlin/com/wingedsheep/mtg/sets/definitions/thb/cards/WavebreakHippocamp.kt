package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wavebreak Hippocamp
 * {2}{U}
 * Enchantment Creature — Horse Fish
 * 2/2
 *
 * Whenever you cast your first spell during each opponent's turn, draw a card.
 *
 * Same trigger shape as Stinging Lionfish: `Triggers.you.castsNth(1)` reads the caster's per-turn
 * cast count (so spells cast before the Hippocamp arrived still count, per the ruling), and
 * [Conditions.IsOpponentsTurn] as the trigger restriction keeps it off your own turn.
 *
 * Canonical printing: Theros Beyond Death (earliest real printing). Reprinted in Jumpstart 2022
 * and Foundations Jumpstart.
 */
val WavebreakHippocamp = card("Wavebreak Hippocamp") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment Creature — Horse Fish"
    power = 2
    toughness = 2
    oracleText = "Whenever you cast your first spell during each opponent's turn, draw a card."

    triggeredAbility {
        trigger = Triggers.you.castsNth(1)
        triggerRestriction = Conditions.IsOpponentsTurn
        effect = Effects.DrawCards(1)
        description = "Whenever you cast your first spell during each opponent's turn, draw a card."
    }

    metadata {
        ruling("2020-01-24", "An ability that triggers when you cast a spell resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2020-01-24", "This ability triggers only on your very first spell during an opponent's turn, not the first spell after the card is on the battlefield. If you cast a spell before it's on the battlefield (including if you cast this card somehow during an opponent's turn), the ability won't trigger.")
        ruling("2020-01-24", "If you have more than one opponent, this ability can trigger once during each of those opponents' turns.")
        ruling("2020-01-24", "In a Two-Headed Giant game, this ability triggers no more than once during each opposing team's turn.")
        rarity = Rarity.RARE
        collectorNumber = "80"
        artist = "Caio Monteiro"
        flavorText = "Tritons search for omens in its wake."
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d900dff5-1196-443f-b9b0-b8e75c67c868.jpg?1783931573"
    }
}
