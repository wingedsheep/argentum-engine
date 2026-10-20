package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Invoke Prejudice — Legends #62
 * {U}{U}{U}{U} · Enchantment · Rare
 *
 * Whenever an opponent casts a creature spell that doesn't share a color with a creature you
 * control, counter that spell unless that player pays {X}, where X is its mana value.
 *
 * - "that doesn't share a color with a creature you control" qualifies the trigger event, so it is
 *   a `triggerRestriction` (checked once, as the spell is cast — 2004 ruling), not an
 *   intervening-"if": creatures you gain or lose before the trigger resolves don't matter. It reads
 *   "you control no creature that shares a color with the triggering spell"; a colorless creature
 *   spell shares a color with nothing, so it always triggers, as does any creature spell while you
 *   control no creatures.
 * - The toll is Power Sink's shape aimed at the trigger: the caster ([Player.TriggeringPlayer])
 *   may pay {X} — X being the spell's mana value on the stack, an {X} in its cost included — and
 *   otherwise the triggering spell is countered. An unaffordable toll isn't offered.
 */
val InvokePrejudice = card("Invoke Prejudice") {
    manaCost = "{U}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever an opponent casts a creature spell that doesn't share a color with a " +
        "creature you control, counter that spell unless that player pays {X}, where X is its mana value."

    triggeredAbility {
        trigger = Triggers.anOpponent.casts(GameObjectFilter.Creature)
        triggerRestriction = Conditions.YouControl(
            GameObjectFilter.Creature.sharingColorWith(EffectTarget.TriggeringEntity),
            negate = true,
        )
        effect = Effects.MayPay(
            cost = Effects.PayDynamicMana(
                amount = DynamicAmounts.triggeringManaValue(),
                payer = Player.TriggeringPlayer,
            ),
            then = Effects.Nothing,
            otherwise = Effects.CounterTriggeringSpell(),
            decisionMaker = EffectTarget.PlayerRef(Player.TriggeringPlayer),
        )
        description = "Whenever an opponent casts a creature spell that doesn't share a color with a " +
            "creature you control, counter that spell unless that player pays {X}, where X is its mana value."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "62"
        artist = "Harold McNeill"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/903d9fde-d7da-4a0e-a337-b63023c6d74b.jpg?1783948074"
        ruling("2004-10-04", "In a multi-player game, it affects all opposing players.")
        ruling("2004-10-04", "This ability triggers when the spell is cast.")
    }
}
