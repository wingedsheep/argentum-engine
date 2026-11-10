package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reckless Bushwhacker
 * {2}{R}
 * Creature — Goblin Warrior Ally
 * 2/1
 * Surge {1}{R} (You may cast this spell for its surge cost if you or a teammate has cast another spell this turn.)
 * Haste
 * When this creature enters, if its surge cost was paid, other creatures you control get +1/+0 and gain haste until end of turn.
 *
 * The enters trigger's intervening "if" reads [Conditions.SurgeCostWasPaid], the flag the engine
 * stamps on the permanent when it was cast for its surge cost (CR 702.117a).
 */
val RecklessBushwhacker = card("Reckless Bushwhacker") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior Ally"
    power = 2
    toughness = 1
    oracleText = "Surge {1}{R} (You may cast this spell for its surge cost if you or a teammate has cast another spell this turn.)\n" +
        "Haste\n" +
        "When this creature enters, if its surge cost was paid, other creatures you control get +1/+0 and gain haste until end of turn."

    keywordAbility(KeywordAbility.surge("{1}{R}"))
    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.SurgeCostWasPaid
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true),
            Effects.ModifyStats(1, 0, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/0405b1b9-976a-4aaf-bec6-fa006decea74.jpg?1783937905"
    }
}
