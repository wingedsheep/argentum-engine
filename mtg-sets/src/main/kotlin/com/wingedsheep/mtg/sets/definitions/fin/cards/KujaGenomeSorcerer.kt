package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.events.SourceFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kuja, Genome Sorcerer // Trance Kuja, Fate Defied — Final Fantasy #232
 * {2}{B}{R} · Legendary Creature — Human Mutant Wizard 3/4
 * // Legendary Creature — Avatar Wizard 4/6
 *
 * Front — Kuja, Genome Sorcerer:
 *   At the beginning of your end step, create a tapped 0/1 black Wizard creature token with
 *   "Whenever you cast a noncreature spell, this token deals 1 damage to each opponent."
 *   Then if you control four or more Wizards, transform Kuja.
 *
 * Back — Trance Kuja, Fate Defied:
 *   Flare Star — If a Wizard you control would deal damage to a permanent or player, it deals
 *   double that damage instead.
 *
 * The token carries its noncreature-cast trigger via [CreateTokenEffect.triggeredAbilities]
 * (Sidequest: Raise a Chocobo's pattern). The "Then if" transform check is a
 * [ConditionalEffect] *after* the token creation in the same resolution, so the freshly
 * created Wizard counts toward the four. Flare Star is the Gratuitous Violence shape
 * ([DoubleDamage]) restricted to Wizards you control; per the official ruling the doubled
 * damage is still dealt by the original source, which the replacement preserves.
 */
private val TranceKujaFateDefied = card("Trance Kuja, Fate Defied") {
    manaCost = ""
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Avatar Wizard"
    oracleText = "Flare Star — If a Wizard you control would deal damage to a permanent or " +
        "player, it deals double that damage instead."
    power = 4
    toughness = 6

    // Flare Star — If a Wizard you control would deal damage to a permanent or player,
    // it deals double that damage instead.
    replacementEffect(
        DoubleDamage(
            appliesTo = EventPattern.DamageEvent(
                source = SourceFilter.Matching(
                    GameObjectFilter.Creature.withSubtype("Wizard").youControl()
                ),
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "232"
        artist = "Joshua Raphael"
        imageUri = "https://cards.scryfall.io/normal/back/0/0/008782d2-72b0-4554-b1ce-2db99969a4d8.jpg?1782686418"
    }
}

private val KujaGenomeSorcererFront = card("Kuja, Genome Sorcerer") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Human Mutant Wizard"
    oracleText = "At the beginning of your end step, create a tapped 0/1 black Wizard creature " +
        "token with \"Whenever you cast a noncreature spell, this token deals 1 damage to each " +
        "opponent.\" Then if you control four or more Wizards, transform Kuja."
    power = 3
    toughness = 4

    // At the beginning of your end step, create a tapped 0/1 black Wizard creature token with
    // "Whenever you cast a noncreature spell, this token deals 1 damage to each opponent."
    // Then if you control four or more Wizards, transform Kuja.
    triggeredAbility {
        trigger = Triggers.YourEndStep
        effect = Effects.Composite(
            CreateTokenEffect(
                power = 0,
                toughness = 1,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Wizard"),
                tapped = true,
                imageUri = "https://cards.scryfall.io/normal/front/1/8/187fe54c-7d0c-4225-9d46-3affbead897d.jpg?1782725378",
                triggeredAbilities = listOf(
                    TriggeredAbility.create(
                        trigger = Triggers.YouCastNoncreature.event,
                        binding = TriggerBinding.ANY,
                        effect = DealDamageEffect(1, EffectTarget.PlayerRef(Player.EachOpponent)),
                    ),
                ),
            ),
            ConditionalEffect(
                condition = Conditions.YouControlAtLeast(
                    4,
                    GameObjectFilter.Creature.withSubtype("Wizard")
                ),
                effect = TransformEffect(EffectTarget.Self),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "232"
        artist = "Joshua Raphael"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/008782d2-72b0-4554-b1ce-2db99969a4d8.jpg?1782686418"

        ruling(
            "2025-06-06",
            "The Wizard token's ability resolves before the spell that caused it to trigger. " +
                "It resolves even if that spell is countered or otherwise leaves the stack."
        )
        ruling(
            "2025-06-06",
            "The damage is dealt by the same source as the original source of damage. The " +
                "doubled damage isn't dealt by Trance Kuja unless it was the original source " +
                "of damage."
        )
    }
}

val KujaGenomeSorcerer: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = KujaGenomeSorcererFront,
    backFace = TranceKujaFateDefied,
)
