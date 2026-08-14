package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider

/**
 * Pyromancer's Goggles
 * {5}
 * Legendary Artifact
 *
 * {T}: Add {R}. When that mana is spent to cast a red instant or sorcery spell, copy that spell
 * and you may choose new targets for the copy.
 *
 * A mana ability carrying a [ManaSpellRider] — the rider machinery exists precisely so the
 * "what happens to the spell this mana pays for" half doesn't have to be modeled as a separate
 * ability. The {R} itself is **unrestricted** ("The mana produced by Pyromancer's Goggles can be
 * spent on anything, not just a red instant or sorcery spell"), so the rider rides along on an
 * `AnySpend` pool entry and simply no-ops when the mana pays for something else.
 *
 * How the rulings map onto [ManaSpellRider.CopySpellWhenSpent]:
 *  - *"Any red instant or sorcery spell you spend the mana on will be copied, not just one that
 *    requires targets"* — the rider is a plain filter match (`InstantOrSorcery` + red), with no
 *    target requirement of its own.
 *  - *"The delayed triggered ability will trigger whether Pyromancer's Goggles is still on the
 *    battlefield or not"* — falls out for free: the trigger is built at payment time with the
 *    **spell** as its source, so the Goggles' fate is irrelevant.
 *  - *"If more than one red mana produced by a Pyromancer's Goggles is spent … That many copies
 *    will be created"* — the consumed-rider collection is a list, not a set, so two spent
 *    rider-carrying mana queue two independent copy triggers.
 *  - *"The copy resolves before the original spell"* — the copy trigger goes on the stack above
 *    the spell it copies (CR 707.10).
 *  - *"The copy is created on the stack, so it's not 'cast'"*, same modes, same X, no additional
 *    costs re-paid — all inherent to `CopyTargetSpellEffect`.
 */
val PyromancersGoggles = card("Pyromancer's Goggles") {
    manaCost = "{5}"
    colorIdentity = "R"
    typeLine = "Legendary Artifact"
    oracleText = "{T}: Add {R}. When that mana is spent to cast a red instant or sorcery spell, " +
        "copy that spell and you may choose new targets for the copy."

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddMana(
            color = Color.RED,
            riders = setOf(
                ManaSpellRider.CopySpellWhenSpent(
                    GameObjectFilter.InstantOrSorcery.withColor(Color.RED)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "236"
        artist = "James Paick"
        flavorText = "\"I hope to meet Jaya Ballard someday. I think we'd get along.\"\n—Chandra Nalaar"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/1163ce9f-cf22-422e-a4b5-0240b88e2816.jpg?1783938309"
        ruling(
            "2024-11-08",
            "The mana produced by Pyromancer's Goggles can be spent on anything, not just a red " +
                "instant or sorcery spell.",
        )
        ruling(
            "2024-11-08",
            "Any red instant or sorcery spell you spend the mana on will be copied, not just one " +
                "that requires targets.",
        )
        ruling(
            "2024-11-08",
            "The delayed triggered ability will trigger whether Pyromancer's Goggles is still on " +
                "the battlefield or not.",
        )
        ruling(
            "2024-11-08",
            "If more than one red mana produced by a Pyromancer's Goggles is spent to cast a single " +
                "red instant or sorcery spell, the delayed triggered ability associated with each " +
                "mana spent will trigger. That many copies will be created. It doesn't matter if " +
                "this red mana was produced by one Pyromancer's Goggles or by multiple Pyromancer's " +
                "Goggles.",
        )
        ruling(
            "2024-11-08",
            "A copy is created even if the spell cast with the red mana produced by Pyromancer's " +
                "Goggles has been countered or otherwise left the stack without resolving by the " +
                "time that ability resolves. The copy resolves before the original spell.",
        )
        ruling(
            "2024-11-08",
            "The copy is created on the stack, so it's not \"cast.\" Abilities that trigger when a " +
                "player casts a spell won't trigger.",
        )
        ruling(
            "2024-11-08",
            "The copy will have the same targets as the spell it's copying unless you choose new " +
                "ones. You may change any number of the targets, including all of them or none of " +
                "them. The new targets must be legal.",
        )
        ruling(
            "2024-11-08",
            "If the copied spell is modal (that is, it says \"Choose one –\" or the like), the copy " +
                "will have the same mode or modes. You can't choose a different one.",
        )
        ruling(
            "2024-11-08",
            "If the copied spell has an X whose value was determined as it was cast, the copy has " +
                "the same value of X.",
        )
        ruling(
            "2024-11-08",
            "You can't choose to pay any additional costs for the copy. However, effects based on " +
                "any additional costs that were paid for the original spell are copied as though " +
                "those same costs were paid for the copy too.",
        )
    }
}
