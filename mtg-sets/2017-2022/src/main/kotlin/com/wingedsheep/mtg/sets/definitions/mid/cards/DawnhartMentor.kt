package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Dawnhart Mentor (Innistrad: Midnight Hunt #179)
 * {2}{G}
 * Creature — Human Warlock
 * 0/4
 * When this creature enters, create a 1/1 white Human creature token.
 * Coven — {5}{G}: Target creature you control gets +3/+3 and gains trample until end of turn.
 * Activate only if you control three or more creatures with different powers.
 */
val DawnhartMentor = card("Dawnhart Mentor") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warlock"
    oracleText = "When this creature enters, create a 1/1 white Human creature token.\n" +
        "Coven — {5}{G}: Target creature you control gets +3/+3 and gains trample until end of turn. " +
        "Activate only if you control three or more creatures with different powers."
    power = 0
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human"),
            imageUri = "https://cards.scryfall.io/normal/front/b/7/b7667345-e11b-4cad-ac4c-84eb1c5656c5.jpg?1783925225",
        )
    }

    activatedAbility {
        cost = Costs.Mana("{5}{G}")
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.ModifyStats(3, 3, t) then
            Effects.GrantKeyword(Keyword.TRAMPLE, t)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.CompareAmounts(
                    left = DynamicAmounts.battlefield(
                        Player.You,
                        GameObjectFilter.Creature,
                    ).distinctValues(CardNumericProperty.POWER),
                    operator = ComparisonOperator.GTE,
                    right = 3,
                ),
            ),
        )
        description = "Coven — {5}{G}: Target creature you control gets +3/+3 and gains trample " +
            "until end of turn. Activate only if you control three or more creatures with different powers."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "179"
        artist = "Fariba Khamseh"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/234a09dd-a202-4487-899f-348744143ba5.jpg?1783925582"
        ruling("2021-09-24", "A creature has different power from another if their powers are different numbers. For example, a 1/1 creature and a 2/1 creature have different powers.")
        ruling("2021-09-24", "For three creatures to have different powers from one another, each of their powers needs to be different. A 1/1 creature, a 2/1 creature, and another 2/1 creature aren't three creatures with different powers, even though both 2/1 creatures have different power than the 1/1 creature.")
    }
}
