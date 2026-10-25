package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GrantCantLoseGame
import com.wingedsheep.sdk.scripting.GrantOpponentsCantWinGame
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * The Book of Exalted Deeds
 * {W}{W}{W}
 * Legendary Artifact — Book
 *
 * At the beginning of your end step, if you gained 3 or more life this turn, create a 3/3 white
 * Angel creature token with flying.
 * {W}{W}{W}, {T}, Exile The Book of Exalted Deeds: Put an enlightened counter on target Angel. It
 * gains "You can't lose the game and your opponents can't win the game." Activate only as a sorcery.
 *
 * The gained ability is the Angel's own and has no duration, so it lasts until that Angel leaves
 * the battlefield; its "you" is the Angel's controller. Like Platinum Angel's printed text, the
 * quoted sentence is two statics: [GrantCantLoseGame] and [GrantOpponentsCantWinGame]. The
 * enlightened counter is a marker nothing reads.
 */
val TheBookOfExaltedDeeds = card("The Book of Exalted Deeds") {
    manaCost = "{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Artifact — Book"
    oracleText = "At the beginning of your end step, if you gained 3 or more life this turn, create a 3/3 " +
        "white Angel creature token with flying.\n" +
        "{W}{W}{W}, {T}, Exile The Book of Exalted Deeds: Put an enlightened counter on target Angel. It gains " +
        "\"You can't lose the game and your opponents can't win the game.\" Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouGainedLifeThisTurnAtLeast(3)
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Angel"),
            keywords = setOf(Keyword.FLYING)
        )
        description = "At the beginning of your end step, if you gained 3 or more life this turn, create a " +
            "3/3 white Angel creature token with flying."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}{W}{W}"), Costs.Tap, Costs.ExileSelf)
        timing = TimingRule.SorcerySpeed
        val angel = target(TargetFilter.Permanent.withSubtype("Angel"))
        effect = Effects.AddCounters(CounterType.ENLIGHTENED, 1, angel) then
            Effects.GrantStaticAbility(GrantCantLoseGame, angel, Duration.Permanent) then
            Effects.GrantStaticAbility(GrantOpponentsCantWinGame, angel, Duration.Permanent)
        description = "{W}{W}{W}, {T}, Exile The Book of Exalted Deeds: Put an enlightened counter on target " +
            "Angel. It gains \"You can't lose the game and your opponents can't win the game.\" Activate only " +
            "as a sorcery."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "4"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1899bb1-dd8e-4437-8ea3-4ad637eabf2b.jpg?1783926538"

        ruling(
            "2021-07-23",
            "The enlightenment counter placed by the last ability serves as a memory aid. It isn't connected " +
                "to the granted ability. The Angel keeps that ability even if it loses the counter. Similarly, " +
                "if the counter is somehow moved to another permanent, that permanent doesn't gain the ability."
        )
        ruling(
            "2021-07-23",
            "The Angel won't lose the ability it gains if it stops being an Angel after the ability resolves."
        )
    }
}
