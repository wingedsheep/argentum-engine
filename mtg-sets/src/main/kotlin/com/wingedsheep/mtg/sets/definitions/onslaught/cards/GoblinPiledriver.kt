package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Goblin Piledriver
 * {1}{R}
 * Creature — Goblin Warrior
 * 1/2
 * Protection from blue
 * Whenever Goblin Piledriver attacks, it gets +2/+0 until end of turn for each other attacking Goblin.
 */
val GoblinPiledriver = card("Goblin Piledriver") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Goblin Warrior"
    power = 1
    toughness = 2
    oracleText = "Protection from blue\nWhenever Goblin Piledriver attacks, it gets +2/+0 until end of turn for each other attacking Goblin."

    keywordAbility(KeywordAbility.ProtectionFromColor(Color.BLUE))

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.ModifyStats(
            power = DynamicAmount.Multiply(
                DynamicAmount.Subtract(
                    DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature.withSubtype("Goblin").attacking()),
                    DynamicAmount.Fixed(1)
                ),
                2
            ),
            toughness = DynamicAmount.Fixed(0),
            target = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "205"
        artist = "Matt Cavotta"
        flavorText = "Throwing out a Piledriver is never wrong."
        imageUri = "https://cards.scryfall.io/large/front/f/6/f6c4df1f-f148-42ec-8e22-e7114216927d.jpg?1562953490"
    }
}
