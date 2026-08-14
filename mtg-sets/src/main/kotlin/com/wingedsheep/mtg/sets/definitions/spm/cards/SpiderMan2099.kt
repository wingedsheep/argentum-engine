package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Spider-Man 2099 — Marvel's Spider-Man #150
 * {U}{R} · Legendary Creature — Spider Human Hero · 2/3
 *
 * From the Future — You can't cast Spider-Man 2099 during your first, second, or third turns of
 * the game.
 * Double strike, vigilance
 * At the beginning of your end step, if you've played a land or cast a spell this turn from
 * anywhere other than your hand, Spider-Man 2099 deals damage equal to his power to any target.
 *
 * The end-step intervening-if is the disjunction of the new
 * `Conditions.YouPlayedLandFromNonHandThisTurn` (land half) and
 * `Conditions.YouCastSpellsThisTurn(1, fromZoneOtherThan = Zone.HAND)` (cast half).
 */
val SpiderMan2099 = card("Spider-Man 2099") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Spider Human Hero"
    power = 2
    toughness = 3
    oracleText = "From the Future — You can't cast Spider-Man 2099 during your first, second, or " +
        "third turns of the game.\n" +
        "Double strike, vigilance\n" +
        "At the beginning of your end step, if you've played a land or cast a spell this turn from " +
        "anywhere other than your hand, Spider-Man 2099 deals damage equal to his power to any target."

    keywords(Keyword.DOUBLE_STRIKE, Keyword.VIGILANCE)

    // From the Future — can be cast only after your third turn.
    spell {
        castOnlyIf(Conditions.Not(Conditions.ControllerTurnsTakenAtMost(3)))
    }

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.Any(
            Conditions.YouPlayedLandFromNonHandThisTurn,
            Conditions.YouCastSpellsThisTurn(atLeast = 1, fromZoneOtherThan = Zone.HAND)
        )
        val target = target("any target", Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.sourcePower(), target)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "150"
        artist = "Toni Infante"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a72c7e7-34f5-4cb0-9959-35516e398e49.jpg?1783905310"
    }
}
