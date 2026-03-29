package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Starscape Cleric
 * {1}{B}
 * Creature — Bat Cleric
 * 2/1
 * Offspring {2}{B}
 * Flying
 * This creature can't block.
 * Whenever you gain life, each opponent loses 1 life.
 */
val StarscapeCleric = card("Starscape Cleric") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Bat Cleric"
    oracleText = "Offspring {2}{B} (You may pay an additional {2}{B} as you cast this spell. If you do, when this creature enters, create a 1/1 token copy of it.)\nFlying\nThis creature can't block.\nWhenever you gain life, each opponent loses 1 life."
    power = 2
    toughness = 1

    // Offspring modeled as Kicker
    keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{2}{B}")))

    // Offspring ETB: create token copy when kicked
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = Effects.CreateTokenCopyOfSelf(overridePower = 1, overrideToughness = 1)
    }

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CantBlock()
    }

    triggeredAbility {
        trigger = Triggers.YouGainLife
        effect = LoseLifeEffect(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Omar Rayyan"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53a938a7-0154-4350-87cb-00da24ec3824.jpg?1721426535"
    }
}
