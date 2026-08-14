package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Virtue of Courage // Embereth Blaze
 * {3}{R}{R}
 * Enchantment
 * Whenever a source you control deals noncombat damage to an opponent, you may exile that many
 * cards from the top of your library. You may play those cards this turn.
 *
 * Adventure: Embereth Blaze — {1}{R}, Instant — Adventure
 * Embereth Blaze deals 2 damage to any target.
 *
 * "A source you control" is the Niv-Mizzet, Visionary shape — a `DealsDamageEvent` with
 * `sourceFilter = GameObjectFilter.Any.youControl()` and `TriggerBinding.ANY`, narrowed to
 * noncombat damage aimed at an opponent. Any object you control counts, not just permanents, so
 * the enchantment sees burn spells and its own Adventure half as readily as a pinger.
 *
 * "That many" reads [ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT] off the trigger context, and the
 * exile-plus-permission half is the standard impulse recipe (`Patterns.Exile.impulse`) taking that
 * dynamic count. The "you may" is an explicit [Gate.MayDecide] rather than `optional = true` on the
 * trigger purely for the prompt: the gate's `descriptionOverride` is what the player is asked, and
 * the pipeline's own description would read as gather/move/grant plumbing.
 *
 * Note the front face is an **enchantment**, not a creature; CR 715.3d puts no type restriction on
 * the card exiled by a resolving Adventure, so this is the same `adventure { }` shape as the
 * creature adventurers (cf. Virtue of Persistence).
 */
val VirtueOfCourage = card("Virtue of Courage") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a source you control deals noncombat damage to an opponent, you may " +
        "exile that many cards from the top of your library. You may play those cards this turn."

    triggeredAbility {
        trigger = Triggers.dealsDamage(
            damageType = DamageType.NonCombat,
            recipient = RecipientFilter.Opponent,
            sourceFilter = GameObjectFilter.Any.youControl(),
            binding = TriggerBinding.ANY
        )
        effect = GatedEffect(
            gate = Gate.MayDecide(),
            then = Patterns.Exile.impulse(
                DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT)
            ),
            // Becomes the yes/no prompt text — the pipeline's auto-description would read as
            // gather/move/grant plumbing.
            descriptionOverride = "You may exile that many cards from the top of your library. " +
                "You may play those cards this turn."
        )
        description = "Whenever a source you control deals noncombat damage to an opponent, you " +
            "may exile that many cards from the top of your library. You may play those cards " +
            "this turn."
    }

    adventure("Embereth Blaze") {
        manaCost = "{1}{R}"
        typeLine = "Instant — Adventure"
        oracleText = "Embereth Blaze deals 2 damage to any target. (Then exile this card. You may " +
            "cast the enchantment later from exile.)"
        spell {
            val anyTarget = target("any target", Targets.Any)
            effect = Effects.DealDamage(2, anyTarget)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "157"
        artist = "Piotr Dura"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b0e6daf-0dec-4718-af79-b7ce137c3135.jpg?1783915088"

        ruling(
            "2023-09-01",
            "Combat damage is the damage that's dealt automatically by attacking and blocking " +
                "creatures. Any other damage is noncombat damage, even if it's dealt during a " +
                "combat phase by an attacking or blocking creature."
        )
        ruling(
            "2023-09-01",
            "You pay all costs and follow all normal timing rules for a card played this way. " +
                "For example, if the exiled card is a land card, you may play it only during your " +
                "main phase while the stack is empty."
        )
        ruling(
            "2023-09-01",
            "If a spell is cast as an Adventure, its controller exiles it instead of putting it " +
                "into its owner's graveyard as it resolves. For as long as it remains exiled, " +
                "that player may cast it as a permanent spell."
        )
    }
}
