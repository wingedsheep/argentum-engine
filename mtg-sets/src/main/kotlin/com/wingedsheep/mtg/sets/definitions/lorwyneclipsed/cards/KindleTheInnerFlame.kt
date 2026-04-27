package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kindle the Inner Flame
 * {3}{R}
 * Kindred Sorcery — Elemental
 *
 * Create a token that's a copy of target creature you control, except it has haste
 * and "At the beginning of the end step, sacrifice this token."
 * Flashback—{1}{R}, Behold three Elementals.
 */
val KindleTheInnerFlame = card("Kindle the Inner Flame") {
    manaCost = "{3}{R}"
    typeLine = "Kindred Sorcery — Elemental"
    oracleText = "Create a token that's a copy of target creature you control, except it has haste and " +
        "\"At the beginning of the end step, sacrifice this token.\"\n" +
        "Flashback—{1}{R}, Behold three Elementals. (You may cast this card from your graveyard for its " +
        "flashback cost. Then exile it. To behold an Elemental, choose an Elemental you control or reveal " +
        "an Elemental card from your hand.)"

    val sacrificeAtEndStep = TriggeredAbility.create(
        trigger = Triggers.EachEndStep.event,
        binding = Triggers.EachEndStep.binding,
        effect = Effects.SacrificeTarget(EffectTarget.Self)
    )

    spell {
        val creature = target("creature you control", Targets.CreatureYouControl)
        effect = Effects.CreateTokenCopyOfTarget(
            target = creature,
            addedKeywords = setOf(Keyword.HASTE),
            triggeredAbilities = listOf(sacrificeAtEndStep)
        )
    }

    keywordAbility(
        KeywordAbility.flashback(
            "{1}{R}",
            AdditionalCost.Behold(filter = Filters.WithSubtype("Elemental"), count = 3)
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Jeff Miracola"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a2adcea-f6b1-4611-b8b8-f19fdee2c571.jpg?1767871968"

        ruling("2025-11-17", "A spell cast using flashback will always be exiled afterward, whether it resolves, is countered, or leaves the stack in some other way.")
        ruling("2025-11-17", "Any enters abilities of the copied creature will trigger when the token enters. Any \"as [this creature] enters\" or \"[this creature] enters with\" abilities of the copied creature will also work.")
        ruling("2025-11-17", "If the copied creature has {X} in its mana cost, X is 0.")
        ruling("2025-11-17", "To behold three Elementals, you can reveal three Elemental cards from your hand, choose three Elementals you control on the battlefield, or reveal and choose any combination that adds up to three. You can't behold the same object more than once to pay this cost.")
    }
}
