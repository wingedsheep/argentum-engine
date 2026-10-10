package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Balrog of Moria
 * {4}{B}{B}{R}
 * Legendary Creature — Avatar Demon
 * 8/8
 *
 * The dies trigger works from the graveyard so "exile it" can reference Self (Undead Butler's
 * shape). "When you do" is a real reflexive trigger whose targets are chosen as it goes on the
 * stack; "for each opponent, … up to one target creature that player controls" is the corpus's
 * one-per-opponent distribution (Kaya, Spirits' Justice / Hapatra): up to one creature per
 * opponent, no two sharing a controller.
 */
val TheBalrogOfMoria = card("The Balrog of Moria") {
    manaCost = "{4}{B}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Avatar Demon"
    power = 8
    toughness = 8
    oracleText = "Trample, haste\n" +
        "When The Balrog of Moria dies, you may exile it. When you do, for each opponent, exile up to " +
        "one target creature that player controls.\n" +
        "Cycling {3}{R} ({3}{R}, Discard this card: Draw a card.)\n" +
        "When you cycle this card, create two Treasure tokens."

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.dies()
        triggerZone = Zone.GRAVEYARD
        effect = Effects.ReflexiveTrigger(
            action = Effects.Exile(EffectTarget.Self),
            optional = true,
            descriptionOverride = "You may exile The Balrog of Moria. When you do, for each opponent, " +
                "exile up to one target creature that player controls."
        ) {
            targets(
                TargetFilter.CreatureOpponentControls,
                optional = true,
                dynamicMaxCount = DynamicAmounts.playerCount(Player.EachOpponent),
                differentControllers = true,
            )
            effect = Effects.ForEachTarget(Effects.Exile(EffectTarget.ContextTarget(0)))
        }
    }

    keywordAbility(KeywordAbility.cycling("{3}{R}"))

    triggeredAbility {
        trigger = Triggers.self.isCycled()
        effect = Effects.CreateTreasure(2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Rudy Siswanto"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a3fcfdc-f2cf-42d8-8bb4-7308bc12746e.jpg?1783916023"
    }
}
