package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Kaito, Cunning Infiltrator
 * {1}{U}{U}
 * Legendary Planeswalker — Kaito
 * Loyalty 3
 *
 * Whenever a creature you control deals combat damage to a player, put a loyalty counter on Kaito.
 * +1: Up to one target creature you control can't be blocked this turn. Draw a card, then discard a card.
 * −2: Create a 2/1 blue Ninja creature token.
 * −9: You get an emblem with "Whenever a player casts a spell, you create a 2/1 blue Ninja creature token."
 *
 * Modeling notes:
 *  - The static-loyalty trigger is the Fynn/Ultimecia shape: a `dealsDamage` trigger with an ANY
 *    binding and a `Creature.youControl()` source filter, so it fires for *any* creature you
 *    control (including Kaito himself if some effect animates him), once per damaging creature per
 *    combat-damage event (CR 603.2).
 *  - The +1 target is genuinely optional (`optional = true`, count 1). Choosing no target still
 *    loots — a composite keeps resolving past a sub-effect that can't do anything (CR 608.2). But
 *    choosing a target that has become illegal by resolution counters the whole ability, so no
 *    draw/discard happens; that's the printed ruling and falls out of normal targeting rules.
 *  - The −9 emblem is a *triggered*-ability emblem, so it uses
 *    [Effects.CreateGlobalTriggeredAbility] with [com.wingedsheep.sdk.scripting.Duration.Permanent]
 *    (the Sarkhan/Sephiroth shape) rather than [Effects.CreatePermanentEmblem], which only carries
 *    static P/T + keyword modifications. Global grants aren't attached to an entity and aren't
 *    zone-checked, so the emblem survives Kaito leaving the battlefield.
 *  - Per the printed ruling, the emblem's trigger goes on the stack above the spell that caused it,
 *    so the token arrives before that spell resolves. That's automatic: the trigger is put on the
 *    stack the next time a player would receive priority, i.e. on top of the still-unresolved spell.
 */
val KaitoCunningInfiltrator = card("Kaito, Cunning Infiltrator") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Kaito"
    startingLoyalty = 3
    oracleText = "Whenever a creature you control deals combat damage to a player, put a loyalty " +
        "counter on Kaito.\n" +
        "+1: Up to one target creature you control can't be blocked this turn. Draw a card, then " +
        "discard a card.\n" +
        "−2: Create a 2/1 blue Ninja creature token.\n" +
        "−9: You get an emblem with \"Whenever a player casts a spell, you create a 2/1 blue Ninja " +
        "creature token.\""

    // Both the −2 and the emblem make the same token.
    val ninjaToken = Effects.CreateToken(
        power = 2,
        toughness = 1,
        colors = setOf(Color.BLUE),
        creatureTypes = setOf("Ninja"),
        imageUri = "https://cards.scryfall.io/normal/front/a/e/aeec04b1-475c-4e55-b72f-327ea5258146.jpg?1783908590",
    )

    // Whenever a creature you control deals combat damage to a player, put a loyalty counter on Kaito.
    triggeredAbility {
        trigger = Triggers.dealsDamage(
            damageType = DamageType.Combat,
            recipient = RecipientFilter.AnyPlayer,
            sourceFilter = GameObjectFilter.Creature.youControl(),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.AddCounters(Counters.LOYALTY, 1, EffectTarget.Self)
        description = "Whenever a creature you control deals combat damage to a player, put a " +
            "loyalty counter on Kaito."
    }

    // +1: Up to one target creature you control can't be blocked this turn.
    //     Draw a card, then discard a card.
    loyaltyAbility(+1) {
        val creature = target(
            "up to one target creature you control",
            TargetCreature(optional = true, filter = TargetFilter.CreatureYouControl),
        )
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature) then
            Patterns.Hand.loot()
    }

    // −2: Create a 2/1 blue Ninja creature token.
    loyaltyAbility(-2) {
        effect = ninjaToken
    }

    // −9: You get an emblem with "Whenever a player casts a spell, you create a 2/1 blue Ninja
    //     creature token."
    loyaltyAbility(-9) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.AnyPlayerCastsSpell.event,
                binding = Triggers.AnyPlayerCastsSpell.binding,
                effect = ninjaToken,
                descriptionOverride = "Whenever a player casts a spell, you create a 2/1 blue " +
                    "Ninja creature token.",
            ),
            descriptionOverride = "Whenever a player casts a spell, you create a 2/1 blue Ninja " +
                "creature token.",
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "44"
        artist = "Evyn Fong"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5dabdea9-2015-49b9-853d-4f7e1262eab3.jpg?1783909117"

        ruling("2024-11-08", "You don't have to choose a target for Kaito's first loyalty ability. However, if you do and that target is illegal when the ability tries to resolve, it won't resolve and none of its effects will happen. You won't draw or discard. The loyalty counter that was added to Kaito to pay the cost of the ability will remain on Kaito, however.")
        ruling("2024-11-08", "The ability of the emblem created by Kaito's last ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered or otherwise leaves the stack without resolving.")
    }
}
