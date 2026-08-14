package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ocelot Pride
 * {W}
 * Creature — Cat
 * 1/1
 *
 * First strike, lifelink
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the
 * game.)
 * At the beginning of your end step, if you gained life this turn, create a 1/1 white Cat
 * creature token. Then if you have the city's blessing, for each token you control that entered
 * this turn, create a token that's a copy of it.
 *
 * Ascend is [Keyword.ASCEND] and nothing else. On a permanent it's a *static* ability (CR
 * 702.131b, "**any time** you control ten or more permanents…"), which the engine handles for
 * every ascend permanent at once — see
 * [com.wingedsheep.engine.mechanics.sba.player.AscendCitysBlessingCheck]. Writing it as an
 * enters-the-battlefield trigger instead would sample the count once, on the turn a one-mana 1/1 is
 * least likely to see ten permanents, and never look again.
 *
 * The end-step ability is a genuine intervening-if (CR 603.4, [triggerCondition]): it doesn't
 * trigger at all unless you gained life *before* the end step began, and it re-checks the same
 * condition at resolution — modeled the same way as Resplendent Angel's near-identical ability.
 * The "Then if…" clause is a separate, resolution-time-only conditional chained after the token
 * creation ([ConditionalEffect]), not a second intervening-if — per the 2024-06-07 ruling, the
 * Cat token created earlier in the *same* resolution has already entered this turn by the time
 * this clause is reached, so [GroupFilter]'s `enteredThisTurn()` correctly picks it up alongside
 * any other token gained this turn. [Effects.ForEachInGroup] snapshots the matching tokens before
 * creating any copies (see [com.wingedsheep.mtg.sets.definitions.soi.cards.SecondHarvest]), so the
 * freshly-minted copies aren't themselves re-iterated within this one resolution — the doubling
 * compounds turn over turn, not infinitely within a single end step.
 */
val OcelotPride = card("Ocelot Pride") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat"
    power = 1
    toughness = 1
    oracleText = "First strike, lifelink\n" +
        "Ascend (If you control ten or more permanents, you get the city's blessing for the " +
        "rest of the game.)\n" +
        "At the beginning of your end step, if you gained life this turn, create a 1/1 white " +
        "Cat creature token. Then if you have the city's blessing, for each token you control " +
        "that entered this turn, create a token that's a copy of it."

    // Ascend needs no ability here: CR 702.131b makes it a static ability, and the engine's
    // 702.131b state-based action grants the city's blessing to anyone controlling a permanent
    // with Keyword.ASCEND once they control ten permanents.
    keywords(Keyword.FIRST_STRIKE, Keyword.LIFELINK, Keyword.ASCEND)

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.YouGainedLifeThisTurn
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Cat"),
            imageUri = "https://cards.scryfall.io/normal/front/7/4/74bacab2-a4c6-4ba5-a208-6bd09ae4cf9f.jpg?1783911119"
        ).then(
            ConditionalEffect(
                condition = Conditions.YouHaveCitysBlessing,
                effect = Effects.ForEachInGroup(
                    filter = GroupFilter(GameObjectFilter.Token.youControl().enteredThisTurn()),
                    effect = Effects.CreateTokenCopyOfTarget(target = EffectTarget.Self)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "38"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89cf6f57-230f-497e-a14e-ad1e8737fd42.jpg?1783911298"

        ruling(
            "2024-06-07",
            "Once you have the city's blessing, you have it for the rest of the game, even if " +
                "you lose control of some or all your permanents. The city's blessing isn't a " +
                "permanent itself and can't be removed by any effect."
        )
        ruling(
            "2024-06-07",
            "Ocelot Pride doesn't need to have been on the battlefield when you gained life. " +
                "For example, if a creature with lifelink deals combat damage on your turn and " +
                "you cast Ocelot Pride during your second main phase, its last ability will " +
                "trigger at the beginning of your end step."
        )
        ruling(
            "2024-06-07",
            "If the creature token created by Ocelot Pride's last ability is your tenth " +
                "permanent, you'll get the city's blessing before the ability would check to " +
                "see if you have the city's blessing."
        )
        ruling("2024-06-07", "Ocelot Pride's last ability doesn't target any of the tokens.")
    }
}
