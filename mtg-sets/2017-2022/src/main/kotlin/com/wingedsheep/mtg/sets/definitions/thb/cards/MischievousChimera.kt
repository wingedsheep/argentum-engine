package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mischievous Chimera
 * {U}{R}
 * Enchantment Creature — Chimera
 * 2/2
 * Flying
 * Whenever you cast your first spell during each opponent's turn, this creature deals 1 damage to
 * each opponent. Scry 1.
 *
 * Dreamstalker Manticore's trigger — `Triggers.you.castsNth(1)` gated by
 * [Conditions.IsOpponentsTurn], so a spell cast before the Chimera arrived still uses up the
 * "first" (ruling) — paying off with 1 damage to each opponent, then a single scry 1.
 */
val MischievousChimera = card("Mischievous Chimera") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Enchantment Creature — Chimera"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Whenever you cast your first spell during each opponent's turn, this creature deals 1 damage " +
        "to each opponent. Scry 1."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.castsNth(1)
        triggerRestriction = Conditions.IsOpponentsTurn
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.Scry(1)
        description = "Whenever you cast your first spell during each opponent's turn, this creature " +
            "deals 1 damage to each opponent. Scry 1."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "223"
        artist = "Antonio José Manzanedo"
        flavorText = "Some curiosities are best left alone."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8a3df0e-4ee8-458b-adf5-56aca20493e9.jpg?1783931521"
        ruling("2020-01-24", "You scry 1 just once, no matter how many opponents are dealt damage.")
        ruling("2020-01-24", "An ability that triggers when you cast a spell resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2020-01-24", "This ability triggers only on your very first spell during an opponent's turn, not the first spell after the card is on the battlefield. If you cast a spell before it's on the battlefield (including if you cast this card somehow during an opponent's turn), the ability won't trigger.")
        ruling("2020-01-24", "If you have more than one opponent, this ability can trigger once during each of those opponents' turns.")
    }
}
