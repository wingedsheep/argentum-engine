package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Stinging Lionfish
 * {1}{U}
 * Enchantment Creature — Fish
 * 2/1
 *
 * Whenever you cast your first spell during each opponent's turn, you may tap or untap target
 * nonland permanent.
 *
 * "Your first spell during each opponent's turn" is the first spell you cast *this turn*, on a turn
 * that is an opponent's: `Triggers.you.castsNth(1)` reads the caster's per-turn cast count (which
 * includes spells cast before the Lionfish arrived, per the ruling), and
 * [Conditions.IsOpponentsTurn] as the trigger restriction keeps it off your own turn. Each opponent's
 * turn resets the count, so it can trigger once per opponent's turn in multiplayer.
 *
 * "You may tap or untap" is the Teller of Tales idiom: `optional = true` plus a two-[Mode] modal
 * with `countsAsModalSpell = false`, the direction chosen on resolution over the target locked in
 * when the trigger went on the stack.
 *
 * Canonical printing: Theros Beyond Death (earliest real printing). Reprinted in Jumpstart 2022.
 */
val StingingLionfish = card("Stinging Lionfish") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment Creature — Fish"
    power = 2
    toughness = 1
    oracleText = "Whenever you cast your first spell during each opponent's turn, you may tap or " +
        "untap target nonland permanent."

    triggeredAbility {
        trigger = Triggers.you.castsNth(1)
        triggerRestriction = Conditions.IsOpponentsTurn
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.Modal(
            modes = listOf(
                Mode.noTarget(Effects.Tap(permanent), "Tap that permanent"),
                Mode.noTarget(Effects.Untap(permanent), "Untap that permanent")
            ),
            chooseCount = 1,
            countsAsModalSpell = false
        )
        optional = true
        description = "Whenever you cast your first spell during each opponent's turn, you may tap " +
            "or untap target nonland permanent."
    }

    metadata {
        ruling("2020-01-24", "An ability that triggers when you cast a spell resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2020-01-24", "This ability triggers only on your very first spell during an opponent's turn, not the first spell after the card is on the battlefield. If you cast a spell before it's on the battlefield (including if you cast this card somehow during an opponent's turn), the ability won't trigger.")
        ruling("2020-01-24", "If you have more than one opponent, this ability can trigger once during each of those opponents' turns.")
        ruling("2020-01-24", "In a Two-Headed Giant game, this ability triggers no more than once during each opposing team's turn.")
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "Christopher Burdett"
        flavorText = "Starfish are its favorite prey."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/0162a0b8-a2d1-4664-a445-331aee6d5175.jpg?1783931577"
    }
}
