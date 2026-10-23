package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyLifeGain
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Honor Troll — Strixhaven: School of Mages #134
 * {2}{G} · Creature — Troll Druid 2/3
 *
 * Vigilance
 * If you would gain life, you gain that much life plus 1 instead.
 * This creature gets +2/+1 as long as you have 25 or more life.
 *
 * Angel of Vitality's shape: a [ModifyLifeGain] replacement (+1 per life-gain event) and a
 * [ConditionalStaticAbility] self pump gated on your life total.
 */
val HonorTroll = card("Honor Troll") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Troll Druid"
    oracleText = "Vigilance\n" +
        "If you would gain life, you gain that much life plus 1 instead.\n" +
        "This creature gets +2/+1 as long as you have 25 or more life."
    power = 2
    toughness = 3

    keywords(Keyword.VIGILANCE)

    replacementEffect(
        ModifyLifeGain(
            multiplier = 1,
            modifier = 1,
            appliesTo = EventPattern.LifeGainEvent(player = Player.You)
        )
    )

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(
                powerBonus = 2,
                toughnessBonus = 1,
                filter = GroupFilter.source()
            ),
            condition = Conditions.CompareAmounts(
                left = DynamicAmounts.lifeTotal(Player.You),
                operator = ComparisonOperator.GTE,
                right = 25
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "134"
        artist = "Jesper Ejsing"
        flavorText = "First in his clan to attend Strixhaven, and now first in his class."
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9c6a6b9-f00c-46a5-8dab-796483e3a262.jpg?1783927343"
        ruling(
            "2021-09-24",
            "If you control two Honor Trolls and you would gain life, you gain that much life plus 2. A third " +
                "Honor Troll has you gain that much life plus 3, and so on.",
        )
        ruling(
            "2021-09-24",
            "The middle ability of Honor Troll applies just once to each life-gaining event, whether it's 1 " +
                "life from Impassioned Orator or 4 life from Dawning Angel. If you gain an amount of life " +
                "\"for each\" of something or \"equal to the number\" of something, that life is gained as " +
                "one event and the ability of Honor Troll applies only once.",
        )
        ruling(
            "2021-09-24",
            "In multiplayer formats where your life total doesn't start at 20, Honor Troll's last ability " +
                "still applies if your life total is 25 or more.",
        )
    }
}
