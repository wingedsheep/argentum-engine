package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Blight-Breath Catoblepas
 * {4}{B}{B}
 * Creature — Beast
 * 3/2
 * When this creature enters, target creature an opponent controls gets -X/-X until end of turn,
 * where X is your devotion to black.
 *
 * X is [DynamicAmounts.devotionTo] black, read as the trigger resolves (ruling) — the Catoblepas's
 * own {B}{B} counts while it is still on the battlefield — and locked into the -X/-X from then on.
 */
val BlightBreathCatoblepas = card("Blight-Breath Catoblepas") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, target creature an opponent controls gets -X/-X until end of " +
        "turn, where X is your devotion to black. (Each {B} in the mana costs of permanents you control " +
        "counts toward your devotion to black.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val victim = target(TargetFilter.Creature.opponentControls())
        val x = DynamicAmounts.devotionTo(Color.BLACK)
        effect = Effects.ModifyStats(power = -x, toughness = -x, target = victim)
        description = "When this creature enters, target creature an opponent controls gets -X/-X until " +
            "end of turn, where X is your devotion to black."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "86"
        artist = "Antonio José Manzanedo"
        flavorText = "Wherever it walks, famine follows behind."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/7865c079-1d91-48d4-852d-d104b6e0c157.jpg?1783931570"
        ruling(
            "2020-01-24",
            "The value of X is determined only as the triggered ability resolves. Once that happens, the " +
                "value of X won't change later in the turn even if your devotion to black changes."
        )
        ruling(
            "2020-01-24",
            "If you put an Aura on an opponent's permanent, you still control the Aura, and mana symbols in " +
                "its mana cost count towards your devotion."
        )
    }
}
