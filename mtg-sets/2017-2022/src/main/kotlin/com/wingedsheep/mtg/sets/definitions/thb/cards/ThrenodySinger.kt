package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Threnody Singer
 * {1}{U}
 * Creature — Siren
 * 1/3
 * Flash
 * Flying
 * When this creature enters, target creature an opponent controls gets -X/-0 until end of turn,
 * where X is your devotion to blue.
 *
 * X is counted as the trigger resolves (ruling) — the Singer's own {U} included while it's still on
 * the battlefield — then locked for the turn.
 */
val ThrenodySinger = card("Threnody Singer") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Siren"
    power = 1
    toughness = 3
    oracleText = "Flash\n" +
        "Flying\n" +
        "When this creature enters, target creature an opponent controls gets -X/-0 until end of turn, " +
        "where X is your devotion to blue. (Each {U} in the mana costs of permanents you control counts " +
        "toward your devotion to blue.)"

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ModifyStats(
            power = -DynamicAmounts.devotionTo(Color.BLUE),
            toughness = DynamicAmounts.fixed(0),
            target = creature,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "75"
        artist = "PINDURSKI"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/242cdffb-5037-4d49-b194-4b60274a8758.jpg?1783931574"
        ruling(
            "2020-01-24",
            "The value of X is determined only as the triggered ability resolves. Once that happens, the " +
                "value of X won't change later in the turn even if your devotion to blue changes."
        )
        ruling(
            "2020-01-24",
            "If you put an Aura on an opponent's permanent, you still control the Aura, and mana symbols in " +
                "its mana cost count towards your devotion."
        )
    }
}
