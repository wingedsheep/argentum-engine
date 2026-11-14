package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Acolyte of the Inferno
 * {2}{R}
 * Creature — Human Monk
 * 3/1
 *
 * Renown 1 (When this creature deals combat damage to a player, if it isn't renowned, put a +1/+1 counter on it and it becomes renowned.)
 * Whenever this creature becomes blocked by a creature, it deals 2 damage to that creature.
 *
 * `Triggers.self.becomesBlocked(by = Creature)` fires once per blocker with that blocker as the
 * triggering entity (Sylvan Basilisk's shape), so each creature in a gang block is dealt 2 damage
 * by its own trigger, before combat damage. The Acolyte is the damage source.
 */
val AcolyteOfTheInferno = card("Acolyte of the Inferno") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Monk"
    oracleText = "Renown 1 (When this creature deals combat damage to a player, if it isn't renowned, put a +1/+1 counter on it and it becomes renowned.)\n" +
        "Whenever this creature becomes blocked by a creature, it deals 2 damage to that creature."
    power = 3
    toughness = 1

    keywordAbility(KeywordAbility.renown(1))

    triggeredAbility {
        trigger = Triggers.self.becomesBlocked(by = GameObjectFilter.Creature)
        effect = Effects.DealDamage(2, EffectTarget.TriggeringEntity)
        description = "Whenever this creature becomes blocked by a creature, it deals 2 damage to that creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "Joseph Meehan"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/186581c7-81a6-4c27-b8a9-ff7f7a4b1d40.jpg?1783938334"
        ruling(
            "2015-06-22",
            "Acolyte of the Inferno's last ability will trigger once for each creature that blocks it. Each of those creatures will be dealt 2 damage."
        )
        ruling(
            "2015-06-22",
            "Acolyte of the Inferno's last ability triggers and resolves before combat damage is dealt. If that causes each creature blocking Acolyte of the Inferno to be destroyed, Acolyte of the Inferno will remain blocked and neither deal nor be dealt combat damage."
        )
    }
}
