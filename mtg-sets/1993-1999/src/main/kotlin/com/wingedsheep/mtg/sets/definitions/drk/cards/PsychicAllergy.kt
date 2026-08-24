package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Psychic Allergy
 * {3}{U}{U}
 * Enchantment
 * As this enchantment enters, choose a color.
 * At the beginning of each opponent's upkeep, this enchantment deals X damage to that player,
 * where X is the number of nontoken permanents of the chosen color they control.
 * At the beginning of your upkeep, destroy this enchantment unless you sacrifice two Islands.
 *
 * Jihad's colour machinery: `EntersWithChoice(ChoiceType.COLOR)` stamps the choice on the permanent,
 * and `sharingChosenColorWithSource()` is the filter that reads it back — the *permanent's* stored
 * colour, not a colour chosen during some later resolution.
 *
 * X is counted on the upkeep player's own battlefield, so the amount is scoped to
 * `Player.TriggeringPlayer`, which for a step trigger is the player whose upkeep it is. Counting it
 * against the Allergy's controller instead would aim a real number at the wrong board.
 *
 * The upkeep tax is the standard `PayOrSufferEffect`; two Islands is a fixed-count sacrifice, so a
 * controller down to one Island can't pay and the Allergy goes.
 */
val PsychicAllergy = card("Psychic Allergy") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose a color.\n" +
        "At the beginning of each opponent's upkeep, this enchantment deals X damage to that " +
        "player, where X is the number of nontoken permanents of the chosen color they control.\n" +
        "At the beginning of your upkeep, destroy this enchantment unless you sacrifice two Islands."

    replacementEffect(EntersWithChoice(ChoiceType.COLOR))

    triggeredAbility {
        trigger = Triggers.EachOpponentUpkeep
        effect = Effects.DealDamage(
            // `Count`, not `AggregateZone`: only `Count` special-cases the battlefield. It scans
            // `state.getBattlefield()` and keeps what the upkeep player *controls* (read off
            // projection), where `AggregateZone` looks up `ZoneKey(player, BATTLEFIELD)` — keyed by
            // **owner** — against an empty projection. That would miscount a stolen permanent on
            // both halves at once, and read printed colours rather than projected ones.
            DynamicAmount.Count(
                Player.TriggeringPlayer,
                Zone.BATTLEFIELD,
                GameObjectFilter.Permanent.sharingChosenColorWithSource().nontoken(),
            ),
            EffectTarget.PlayerRef(Player.TriggeringPlayer),
        )
        description = "At the beginning of each opponent's upkeep, this enchantment deals X " +
            "damage to that player, where X is the number of nontoken permanents of the chosen " +
            "color they control."
    }

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = PayOrSufferEffect(
            cost = Costs.pay.Sacrifice(GameObjectFilter.Land.withSubtype(Subtype.ISLAND), count = 2),
            suffer = Effects.Destroy(EffectTarget.Self),
        )
        description = "At the beginning of your upkeep, destroy this enchantment unless you " +
            "sacrifice two Islands."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "34"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fec3275e-4491-43a8-9f23-d7b48177c103.jpg?1783947941"
    }
}
