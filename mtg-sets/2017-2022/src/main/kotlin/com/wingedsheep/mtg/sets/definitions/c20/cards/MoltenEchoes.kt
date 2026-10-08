package com.wingedsheep.mtg.sets.definitions.c20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Molten Echoes
 * {2}{R}{R}
 * Enchantment
 *
 * As this enchantment enters, choose a creature type.
 * Whenever a nontoken creature you control of the chosen type enters, create a token that's a copy
 * of that creature. That token gains haste. Exile it at the beginning of the next end step.
 *
 * The chosen type is Bloodline Pretender's shape: an [EntersWithChoice] creature-type replacement
 * read by `withChosenSubtype()` in the trigger filter. The copy is Nahiri, the Unforgiving's
 * "create a token that's a copy of it. That token gains haste. Exile it at the beginning of the next
 * end step" — `CreateTokenCopyOfTarget` with a haste rider and `exileAtStep = END` — pointed at the
 * triggering creature, the shape Necroduality and Esoteric Duplicator already use. If the creature
 * leaves the battlefield before the trigger resolves, the copy is made from its last-known copiable
 * values (first ruling; CR 608.2h).
 */
val MoltenEchoes = card("Molten Echoes") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose a creature type.\n" +
        "Whenever a nontoken creature you control of the chosen type enters, create a token that's a copy " +
        "of that creature. That token gains haste. Exile it at the beginning of the next end step."

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.nontoken().youControl().withChosenSubtype()).enters()
        effect = Effects.CreateTokenCopyOfTarget(
            target = EffectTarget.TriggeringEntity,
            addedKeywords = setOf(Keyword.HASTE),
            exileAtStep = Step.END,
        )
        description = "Whenever a nontoken creature you control of the chosen type enters, create a token " +
            "that's a copy of that creature. That token gains haste. Exile it at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8ca1dbc4-2177-477e-88e2-d07cefb542e4.jpg?1783931213"
        ruling(
            "2020-04-17",
            "You must choose an existing creature type, such as Human or Warrior. Card types such as artifact " +
                "and supertypes such as legendary can't be chosen."
        )
        ruling(
            "2020-04-17",
            "If the nontoken creature leaves the battlefield before Molten Echoes's triggered ability resolves, " +
                "the token that's created uses the creature's last known information from before it left."
        )
        ruling(
            "2020-04-17",
            "Any enters-the-battlefield abilities of the copied creature will trigger when the token enters the " +
                "battlefield. Any \"As [this creature] enters the battlefield\" or \"[This creature] enters the " +
                "battlefield with\" abilities of the copied creature will also work."
        )
    }
}
