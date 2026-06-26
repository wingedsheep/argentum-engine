package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hope Estheim
 * {W}{U}
 * Legendary Creature — Human Wizard
 * 2/2
 *
 * Lifelink
 * At the beginning of your end step, each opponent mills X cards, where X is the amount of
 * life you gained this turn.
 *
 * Implementation notes:
 *  - The mill amount is the life *you* gained this turn ([DynamicAmounts.lifeGainedThisTurn],
 *    backed by the LIFE_GAINED turn tracker). If you gained no life, X = 0 and each opponent
 *    mills nothing (the gather of zero cards is a no-op).
 *  - "Each opponent mills X" mills each opponent's own library top via a single mill pipeline
 *    targeted at [Player.EachOpponent] (the gather fans out over each opponent in turn order).
 *    The count is *not* wrapped in a per-player iteration so `Player.You` in
 *    [DynamicAmounts.lifeGainedThisTurn] keeps resolving to the ability's controller rather than
 *    being rebound to the milling opponent.
 */
val HopeEstheim = card("Hope Estheim") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Wizard"
    oracleText = "Lifelink\n" +
        "At the beginning of your end step, each opponent mills X cards, where X is the amount " +
        "of life you gained this turn."
    power = 2
    toughness = 2

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.YourEndStep
        effect = Patterns.Library.mill(
            count = DynamicAmounts.lifeGainedThisTurn(),
            target = EffectTarget.PlayerRef(Player.EachOpponent),
        )
        description = "At the beginning of your end step, each opponent mills X cards, where X " +
            "is the amount of life you gained this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "226"
        artist = "Fariba Khamseh"
        flavorText = "\"If we have the power to destroy Cocoon, then we have the power to save it.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fbdb68cc-5516-481a-94c5-59f6c69b8a17.jpg?1748706615"
    }
}
