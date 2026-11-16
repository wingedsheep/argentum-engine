package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Lurking Arynx
 * {4}{G}
 * Creature — Cat Beast
 * 3/5
 *
 * Formidable — {2}{G}: Target creature blocks this creature this turn if able. Activate only if
 * creatures you control have total power 8 or greater.
 *
 * Rampant Elephant's [Effects.ForceBlock] behind Atarka Beastbreaker's formidable restriction
 * (checked only on activation). Each activation adds its own requirement, so several creatures can
 * be forced to block it.
 */
val LurkingArynx = card("Lurking Arynx") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat Beast"
    oracleText = "Formidable — {2}{G}: Target creature blocks this creature this turn if able. " +
        "Activate only if creatures you control have total power 8 or greater."
    power = 3
    toughness = 5

    activatedAbility {
        val creature = target(TargetFilter.Creature)
        cost = Costs.Mana("{2}{G}")
        effect = Effects.ForceBlock(creature)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.CompareAmounts(
                    DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).sumPower(),
                    ComparisonOperator.GTE,
                    8
                )
            )
        )
        description = "Formidable — {2}{G}: Target creature blocks this creature this turn if able."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "192"
        artist = "Carl Frank"
        flavorText = "Once it has your scent, it will hunt you from the Gurmag Swamp to Ayagor."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f59bc0b-88de-4580-bfc8-5af911d9ee99.jpg?1783938578"
        ruling("2015-02-25", "You may activate Lurking Arynx's ability multiple times in a turn to force multiple creatures to block it if able.")
        ruling("2015-02-25", "Activating Lurking Arynx's ability doesn't force you to attack with it that turn. If it doesn't attack, the creatures affected by its ability can block other attacking creatures or not block at all.")
        ruling("2015-02-25", "If a creature affected by Lurking Arynx's ability can't legally block it (perhaps because Lurking Arynx has gained flying), that creature can block other attacking creatures or not block at all.")
        ruling("2015-02-25", "Some formidable abilities are activated abilities that require creatures you control to have total power 8 or greater. Once you activate these abilities, it doesn't matter what happens to the total power of creatures you control.")
    }
}
