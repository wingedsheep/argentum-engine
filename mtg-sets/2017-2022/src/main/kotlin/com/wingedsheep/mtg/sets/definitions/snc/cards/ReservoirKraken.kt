package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reservoir Kraken
 * {2}{U}{U}
 * Creature — Kraken
 * 6/6
 *
 * Trample, ward {2}
 * At the beginning of each combat, if this creature is untapped, any opponent may tap an untapped
 * creature they control. If they do, tap this creature and create a 1/1 blue Fish creature token
 * with "This token can't be blocked."
 *
 * The "if this creature is untapped" clause is an intervening if: checked when the trigger would
 * fire and again on resolution.
 */
val ReservoirKraken = card("Reservoir Kraken") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Kraken"
    power = 6
    toughness = 6
    oracleText = "Trample, ward {2}\n" +
        "At the beginning of each combat, if this creature is untapped, any opponent may tap an " +
        "untapped creature they control. If they do, tap this creature and create a 1/1 blue Fish " +
        "creature token with \"This token can't be blocked.\""

    keywords(Keyword.TRAMPLE)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.SourceIsUntapped
        effect = Effects.AnyPlayerMayPay(
            cost = Costs.pay.Tap(GameObjectFilter.Creature),
            consequence = Effects.Tap(EffectTarget.Self) then
                Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.BLUE),
                    creatureTypes = setOf("Fish"),
                    staticAbilities = listOf(CantBeBlocked(GroupFilter.source())),
                    imageUri = "https://cards.scryfall.io/normal/front/3/b/3b2e726b-f124-43f7-87e4-bb06bbe8e3d7.jpg?1783922964",
                ),
            eligiblePlayers = Player.EachOpponent
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "56"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53cf9152-da16-4289-9a2d-331e7b9cb839.jpg?1783923140"
    }
}
