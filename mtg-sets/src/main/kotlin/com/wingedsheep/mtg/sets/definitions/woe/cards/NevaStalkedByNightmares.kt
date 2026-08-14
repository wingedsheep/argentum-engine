package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Neva, Stalked by Nightmares
 * {2}{W}{B}
 * Legendary Creature — Human Noble
 * 2/2
 *
 * Menace
 * When Neva enters, return target creature or enchantment card from your graveyard to your hand.
 * Whenever an enchantment you control is put into a graveyard from the battlefield, put a +1/+1
 * counter on Neva, then scry 1.
 *
 * Two triggers with nothing in common but the card they're printed on:
 *
 * 1. The enters trigger is a plain regrowth over the union filter
 *    [GameObjectFilter.CreatureOrEnchantment], scoped to cards you own in [Zone.GRAVEYARD]. It's a
 *    mandatory target — with an empty (or creature-and-enchantment-free) graveyard the ability
 *    simply never goes on the stack, per CR 603.3d.
 *
 * 2. The enchantment-death trigger uses the generic `leavesBattlefield` factory with an
 *    [TriggerBinding.ANY] binding, exactly as [WarehouseTabby] does, so it catches *every* route
 *    to the graveyard — destroyed, sacrificed, or a Role token replaced by another Role. Enchantment
 *    tokens do reach the graveyard before ceasing to exist, so Roles falling off count (WOE ruling).
 *
 * The counter and the scry are ordered, not simultaneous ("then"): [Effects.Composite] runs them in
 * sequence. If Neva has already left the battlefield the counter finds nothing to land on and the
 * scry still happens — which is also why an Aura attached to Neva when Neva itself leaves doesn't
 * trigger this ability at all unless the same effect destroyed both.
 */
val NevaStalkedByNightmares = card("Neva, Stalked by Nightmares") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Noble"
    power = 2
    toughness = 2
    oracleText = "Menace\n" +
        "When Neva enters, return target creature or enchantment card from your graveyard to your " +
        "hand.\n" +
        "Whenever an enchantment you control is put into a graveyard from the battlefield, put a " +
        "+1/+1 counter on Neva, then scry 1."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val card = target(
            "target creature or enchantment card in your graveyard",
            TargetObject(
                filter = TargetFilter(
                    baseFilter = GameObjectFilter.CreatureOrEnchantment.ownedByYou(),
                    zone = Zone.GRAVEYARD,
                ),
            ),
        )
        effect = Effects.ReturnToHand(card)
        description = "When Neva enters, return target creature or enchantment card from your " +
            "graveyard to your hand."
    }

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Enchantment.youControl(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY,
        )
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            Patterns.Library.scry(1),
        )
        description = "Whenever an enchantment you control is put into a graveyard from the " +
            "battlefield, put a +1/+1 counter on Neva, then scry 1."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "209"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6f925ab-ade7-4da8-a791-185e098d18f2.jpg?1783915070"

        ruling(
            "2023-09-01",
            "Enchantment tokens (such as Roles) that are sacrificed, destroyed, or would otherwise go " +
                "to the graveyard are put into their owner's graveyard before ceasing to exist. If you " +
                "controlled the token, Neva's ability will trigger."
        )
        ruling(
            "2023-09-01",
            "If Neva leaves the battlefield while it has an Aura you control attached to it or at the " +
                "same time as another creature with an Aura you control attached to it, its ability " +
                "won't trigger for those enchantments unless they were destroyed by the same effect " +
                "that caused Neva to leave the battlefield."
        )
    }
}
