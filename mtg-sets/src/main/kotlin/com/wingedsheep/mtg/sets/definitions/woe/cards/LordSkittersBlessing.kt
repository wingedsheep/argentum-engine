package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Lord Skitter's Blessing
 * {1}{B}
 * Enchantment
 *
 * When this enchantment enters, create a Wicked Role token attached to target creature you control.
 * At the beginning of your draw step, if you control an enchanted creature, you lose 1 life and you
 * draw an additional card.
 *
 * Two independent triggered abilities, no statics.
 *
 * The enters ability reuses [Effects.CreateRoleToken] — the shared Role machinery that already backs
 * [CharmingScoundrel] and [NotDeadAfterAll], so the +1/+1 and the "each opponent loses 1 life" death
 * trigger come with the token rather than being restated here. Per the WOE Role rulings the target is
 * required: with no legal creature the enters ability is removed from the stack and no Role is created,
 * and the state-based Role-uniqueness action (one Role per permanent per controller, oldest ones
 * binned) is likewise the token's business, not this card's.
 *
 * The draw-step ability is an intervening-'if' clause (CR 603.4), so the check runs twice — once as
 * the step begins, once again on resolution — and "an enchanted creature" is exactly
 * [StatePredicate.IsEnchanted][com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsEnchanted]
 * scoped to creatures you control. Usually that creature is the Role this card just made, but any
 * Aura will do, including an opponent's Aura on your creature (CR 303.4).
 *
 * "You draw an additional card" is modelled as a plain extra draw on the trigger, which is the whole
 * of its meaning here: the ability resolves during the draw step alongside the turn-based draw rather
 * than replacing or modifying it, so a second `DrawCards(1)` is not an approximation. Life loss is
 * ordered before the draw to match the printed order — it matters when the loss is lethal.
 */
val LordSkittersBlessing = card("Lord Skitter's Blessing") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create a Wicked Role token attached to target " +
        "creature you control. (Enchanted creature gets +1/+1. When this token is put into a " +
        "graveyard, each opponent loses 1 life.)\n" +
        "At the beginning of your draw step, if you control an enchanted creature, you lose 1 life " +
        "and you draw an additional card."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "target creature you control",
            TargetCreature(filter = TargetFilter.CreatureYouControl),
        )
        effect = Effects.CreateRoleToken("Wicked Role", creature)
        description = "When this enchantment enters, create a Wicked Role token attached to target " +
            "creature you control."
    }

    triggeredAbility {
        trigger = Triggers.YourDrawStep
        triggerCondition = Conditions.YouControlAtLeast(
            1,
            GameObjectFilter.Creature.youControl().enchanted(),
        )
        effect = Effects.LoseLife(1, EffectTarget.Controller)
            .then(Effects.DrawCards(1))
        description = "At the beginning of your draw step, if you control an enchanted creature, " +
            "you lose 1 life and you draw an additional card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Joseph Weston"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84f343a9-883f-4532-ae66-be6470d67d38.jpg?1783915106"

        ruling(
            "2023-09-01",
            "Roles are colorless enchantment tokens. Each one has the Aura and Role subtypes and " +
                "the enchant creature ability."
        )
        ruling(
            "2023-09-01",
            "If a permanent has more than one Role attached to it controlled by the same player, " +
                "each of those Roles except the one with the most recent timestamp is put into its " +
                "owner's graveyard. This is a state-based action."
        )
        ruling(
            "2023-09-01",
            "A permanent can have multiple Roles attached to it if each one is controlled by a " +
                "different player."
        )
        ruling(
            "2023-09-01",
            "Some spells and abilities that create Role tokens require targets. If each target " +
                "chosen is an illegal target as that spell or ability tries to resolve, it won't " +
                "resolve. The Role token won't be created."
        )
    }
}
