package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Dreamstalker Manticore
 * {2}{R}
 * Enchantment Creature — Manticore
 * 4/2
 * Whenever you cast your first spell during each opponent's turn, this creature deals 1 damage to
 * any target.
 *
 * Arena Trickster's trigger — `Triggers.you.castsNth(1)` (the caster's per-turn cast count, so a
 * spell cast before the Manticore arrived still uses up the "first", per the ruling) restricted by
 * [Conditions.IsOpponentsTurn] — paying off with 1 damage to [Targets.Any].
 */
val DreamstalkerManticore = card("Dreamstalker Manticore") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment Creature — Manticore"
    power = 4
    toughness = 2
    oracleText = "Whenever you cast your first spell during each opponent's turn, this creature deals " +
        "1 damage to any target."

    triggeredAbility {
        trigger = Triggers.you.castsNth(1)
        triggerRestriction = Conditions.IsOpponentsTurn
        val victim = target(Targets.Any)
        effect = Effects.DealDamage(1, victim)
        description = "Whenever you cast your first spell during each opponent's turn, this creature " +
            "deals 1 damage to any target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "131"
        artist = "Chris Rallis"
        flavorText = "Purphoros forged the manticore's tail from bronze, its teeth from silver, and its " +
            "heart from hate."
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee10eaa0-dbe0-45ee-9a7d-395ec5d27e08.jpg?1783931554"
        ruling("2020-01-24", "An ability that triggers when you cast a spell resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2020-01-24", "This ability triggers only on your very first spell during an opponent's turn, not the first spell after the card is on the battlefield. If you cast a spell before it's on the battlefield (including if you cast this card somehow during an opponent's turn), the ability won't trigger.")
        ruling("2020-01-24", "If you have more than one opponent, this ability can trigger once during each of those opponents' turns.")
        ruling("2020-01-24", "In a Two-Headed Giant game, this ability triggers no more than once during each opposing team's turn.")
    }
}
