package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Cleaving Skyrider — Dominaria United #12
 *
 * X counts every attacking creature on the battlefield, whoever controls it, so the count is
 * `battlefield(Player.Each, Creature.attacking())` read on resolution.
 */
val CleavingSkyrider = card("Cleaving Skyrider") {
    manaCost = "{2}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 2
    oracleText = "Flash\nKicker {2}{R} (You may pay an additional {2}{R} as you cast this spell.)\nFlying\n" +
        "When this creature enters, if it was kicked, it deals X damage to any target, where X is the number of attacking creatures."

    keywords(Keyword.FLASH, Keyword.FLYING)
    keywordAbility(KeywordAbility.kicker("{2}{R}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasKicked
        val victim = target(Targets.Any)
        effect = Effects.DealDamage(
            DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Creature.attacking()).count(),
            victim
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "12"
        artist = "Joshua Raphael"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/6544a289-3b27-4980-8ed7-22f50ae92c1a.jpg?1783921369"
    }
}
