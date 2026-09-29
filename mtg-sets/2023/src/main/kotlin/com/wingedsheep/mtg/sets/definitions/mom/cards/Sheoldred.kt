package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ReturnFace
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sheoldred // The True Scriptures — March of the Machine #125
 * {3}{B}{B} · Legendary Creature — Phyrexian Praetor 4/5 // Enchantment — Saga
 *
 * Chapter I's "for each opponent, destroy up to one target creature or planeswalker that player
 * controls" is the one-per-player distribution (Kaya, Spirits' Justice): up to one target per
 * opponent, no two sharing a controller. Chapter III gathers creature cards from every graveyard and
 * moves them onto the battlefield under the Saga controller's control (Rise of the Dark Realms), then
 * the Saga exiles itself and returns front face up.
 */
private val TheTrueScriptures = card("The True Scriptures") {
    manaCost = ""
    colorIndicator = "B"
    colorIdentity = "B"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — For each opponent, destroy up to one target creature or planeswalker that player controls.\n" +
        "II — Each opponent discards three cards, then mills three cards.\n" +
        "III — Put all creature cards from all graveyards onto the battlefield under your control. " +
        "Exile this Saga, then return it to the battlefield (front face up)."

    sagaChapter(1) {
        targets(
            TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()),
            optional = true,
            dynamicMaxCount = DynamicAmounts.playerCount(Player.EachOpponent),
            differentControllers = true,
        )
        effect = Effects.ForEachTarget(Effects.Destroy(EffectTarget.ContextTarget(0)))
    }

    sagaChapter(2) {
        effect = Effects.EachOpponentDiscards(3) then
            Patterns.Library.mill(3, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    sagaChapter(3) {
        effect = Effects.Pipeline {
            val creatureCards = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.Each,
                    filter = GameObjectFilter.Creature
                )
            )
            move(creatureCards, CardDestination.ToZone(Zone.BATTLEFIELD))
        } then Effects.ExileAndReturnTransformed(EffectTarget.Self, ReturnFace.FRONT)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "125"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/back/b/f/bf2249e6-af74-4b88-8eb7-144ce8fa7f6b.jpg?1783917007"
    }
}

private val OpponentHasEightInGraveyard = Conditions.CompareAmounts(
    DynamicAmounts.greatestAmongPlayers(
        DynamicAmounts.count(Player.You, Zone.GRAVEYARD),
        Player.EachOpponent
    ),
    ComparisonOperator.GTE,
    8
)

private val SheoldredFront = card("Sheoldred") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Phyrexian Praetor"
    power = 4
    toughness = 5
    oracleText = "Menace\n" +
        "When Sheoldred enters, each opponent sacrifices a nontoken creature or planeswalker of their choice.\n" +
        "{4}{B}: Exile Sheoldred, then return it to the battlefield transformed under its owner's control. " +
        "Activate only as a sorcery and only if an opponent has eight or more cards in their graveyard."
    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Sacrifice(
            GameObjectFilter.CreatureOrPlaneswalker.nontoken(),
            count = 1,
            target = EffectTarget.PlayerRef(Player.EachOpponent),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{4}{B}")
        timing = TimingRule.SorcerySpeed
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(OpponentHasEightInGraveyard))
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "125"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf2249e6-af74-4b88-8eb7-144ce8fa7f6b.jpg?1783917007"
        ruling(
            "2023-04-14",
            "You can activate Sheoldred's last ability if any opponent has eight or more cards in their " +
                "graveyard, not necessarily all of them."
        )
        ruling(
            "2023-04-14",
            "Once you've activated Sheoldred's last ability, it doesn't matter what happens to the number " +
                "of cards in opponents' graveyards. The ability will resolve even if no opponent has eight " +
                "or more cards in their graveyard by that time."
        )
    }
}

val Sheoldred: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = SheoldredFront,
    backFace = TheTrueScriptures,
)
