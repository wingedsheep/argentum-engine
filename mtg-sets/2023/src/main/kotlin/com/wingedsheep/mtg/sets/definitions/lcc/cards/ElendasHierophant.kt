package com.wingedsheep.mtg.sets.definitions.lcc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elenda's Hierophant
 * {2}{W}
 * Creature — Vampire Cleric
 * 1/1
 * Flying
 * Whenever you gain life, put a +1/+1 counter on this creature.
 * When this creature dies, create X 1/1 white Vampire creature tokens with lifelink, where X is its
 * power.
 *
 * Twinblade Paladin's life-gain growth plus Elenda, the Dusk Rose's death trigger: `sourcePower()`
 * falls back to the battlefield-exit snapshot, so X is its power as it last existed on the
 * battlefield.
 */
val ElendasHierophant = card("Elenda's Hierophant") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Vampire Cleric"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "Whenever you gain life, put a +1/+1 counter on this creature.\n" +
        "When this creature dies, create X 1/1 white Vampire creature tokens with lifelink, where X is its power."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever you gain life, put a +1/+1 counter on this creature."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            count = DynamicAmounts.sourcePower(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Vampire"),
            keywords = setOf(Keyword.LIFELINK),
            imageUri = "https://cards.scryfall.io/normal/front/0/4/0484390e-1167-4407-84de-7ddc726e8926.jpg?1783913608"
        )
        description = "When this creature dies, create X 1/1 white Vampire creature tokens with " +
            "lifelink, where X is its power."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Miranda Meeks"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d12f728a-1bbb-4bee-96d4-d721292997ba.jpg?1783913913"
        ruling("2023-11-10", "The second ability of Elenda's Hierophant triggers just once for each life-gaining event, no matter how much life was gained.")
        ruling("2023-11-10", "If Elenda's Hierophant is dealt lethal damage at the same time that you gain life, it won't receive a counter from its second ability in time to save it.")
    }
}
