package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bloodline Pretender
 * {3}
 * Artifact Creature — Shapeshifter
 * 2/2
 *
 * Changeling (This card is every creature type.)
 * As this creature enters, choose a creature type.
 * Whenever another creature you control of the chosen type enters, put a +1/+1 counter on this creature.
 *
 * [EntersWithChoice] stores the creature type as the Pretender enters; `withChosenSubtype()` on the
 * trigger's subject reads that stored choice (same shape as Dawn-Blessed Pennant). With no stored
 * choice the filter matches nothing, so the trigger can't fire — as the second ruling requires.
 */
val BloodlinePretender = card("Bloodline Pretender") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Shapeshifter"
    power = 2
    toughness = 2
    oracleText = "Changeling (This card is every creature type.)\n" +
        "As this creature enters, choose a creature type.\n" +
        "Whenever another creature you control of the chosen type enters, put a +1/+1 counter on this creature."

    keywords(Keyword.CHANGELING)

    // As this creature enters, choose a creature type.
    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    // Whenever another creature you control of the chosen type enters, put a +1/+1 counter on this creature.
    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl().withChosenSubtype()).enters()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "235"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb8a16f6-55c1-40eb-998f-592bf31916b1.jpg?1783928187"
        ruling(
            "2021-02-05",
            "If Bloodline Pretender enters the battlefield at the same time as another creature, you can " +
                "choose one of that creature's creature types and have Bloodline Pretender's last ability " +
                "trigger for that other creature."
        )
        ruling(
            "2021-02-05",
            "If Bloodline Pretender is somehow on the battlefield without a chosen creature type, its last " +
                "ability can't trigger, even if a creature with no creature types enters the battlefield."
        )
        ruling(
            "2021-02-05",
            "You must choose an existing creature type, such as Vampire or Druid. You can't choose card " +
                "types (e.g., artifact) or supertypes (e.g., snow)."
        )
    }
}
