package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ambuscade Shaman
 * {2}{B}
 * Creature — Orc Shaman
 * 2/2
 *
 * Whenever this creature or another creature you control enters, that creature gets +2/+2 until end of turn.
 * Dash {3}{B} (You may cast this spell for its dash cost. If you do, it gains haste, and it's returned from the battlefield to its owner's hand at the beginning of the next end step.)
 *
 * "This creature or another creature you control" is `Triggers.a(...)` (self included), and
 * "that creature" is [EffectTarget.TriggeringEntity] — so creatures entering together each get
 * their own trigger, per the 2015-02-25 ruling.
 */
val AmbuscadeShaman = card("Ambuscade Shaman") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Orc Shaman"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature or another creature you control enters, that creature gets +2/+2 until end of turn.\n" +
        "Dash {3}{B} (You may cast this spell for its dash cost. If you do, it gains haste, and it's returned from the battlefield to its owner's hand at the beginning of the next end step.)"

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.ModifyStats(2, 2, EffectTarget.TriggeringEntity)
        description = "Whenever this creature or another creature you control enters, that creature gets +2/+2 until end of turn."
    }

    dash = "{3}{B}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "Anthony Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b9a91f8-5f9f-46a2-bd80-1ca014591de9.jpg?1783938602"
        ruling("2015-02-25", "If Ambuscade Shaman enters the battlefield at the same time as other creatures you control, its ability will trigger for each of those creatures.")
    }
}
