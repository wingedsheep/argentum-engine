package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Alert Heedbonder
 * {1}{G/W}{G/W}
 * Creature — Human Scout
 * 2/4
 * Vigilance
 * At the beginning of your end step, you gain 1 life for each creature you control with vigilance.
 *
 * The count is taken as the trigger resolves, over projected keywords, so it includes the
 * Heedbonder itself while it's still on the battlefield.
 */
val AlertHeedbonder = card("Alert Heedbonder") {
    manaCost = "{1}{G/W}{G/W}"
    colorIdentity = "GW"
    typeLine = "Creature — Human Scout"
    power = 2
    toughness = 4
    oracleText = "Vigilance\n" +
        "At the beginning of your end step, you gain 1 life for each creature you control with vigilance."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.GainLife(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature.withKeyword(Keyword.VIGILANCE)).count()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Randy Vargas"
        flavorText = "Considering the zealous armies, vile poachers, and titanic monsters, it's always wise to keep one eye on the horizon."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15a2ccae-c322-4e99-a094-a68fc5d52ea3.jpg?1783931012"
        ruling("2020-04-17", "The number of creatures you control with vigilance is determined as Alert Heedbonder's last ability resolves. If it's still on the battlefield, the count will include Alert Heedbonder itself.")
    }
}
