package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop
import com.wingedsheep.sdk.scripting.PermanentsEnterTapped

/**
 * Thalia and The Gitrog Monster
 * {1}{W}{B}{G}
 * Legendary Creature — Human Frog Horror
 * 4/4
 *
 * First strike, deathtouch
 * You may play an additional land on each of your turns.
 * Creatures and nonbasic lands your opponents control enter tapped.
 * Whenever Thalia and The Gitrog Monster attacks, sacrifice a creature or land, then draw a card.
 *
 * The enter-tapped clause is the same pair of [PermanentsEnterTapped] replacements Thalia,
 * Heretic Cathar uses. The attack trigger's sacrifice is mandatory (see ruling) — the controller
 * may sacrifice Thalia and The Gitrog Monster itself, since the text doesn't say "another".
 */
val ThaliaAndTheGitrogMonster = card("Thalia and The Gitrog Monster") {
    manaCost = "{1}{W}{B}{G}"
    colorIdentity = "WBG"
    typeLine = "Legendary Creature — Human Frog Horror"
    power = 4
    toughness = 4
    oracleText = "First strike, deathtouch\n" +
        "You may play an additional land on each of your turns.\n" +
        "Creatures and nonbasic lands your opponents control enter tapped.\n" +
        "Whenever Thalia and The Gitrog Monster attacks, sacrifice a creature or land, then draw a card."

    keywords(Keyword.FIRST_STRIKE, Keyword.DEATHTOUCH)

    staticAbility {
        ability = GrantAdditionalLandDrop(count = 1)
    }

    // Creatures your opponents control enter tapped.
    replacementEffect(
        PermanentsEnterTapped(
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.opponentControls(),
                to = Zone.BATTLEFIELD,
            )
        )
    )

    // Nonbasic lands your opponents control enter tapped.
    replacementEffect(
        PermanentsEnterTapped(
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.NonbasicLand.opponentControls(),
                to = Zone.BATTLEFIELD,
            )
        )
    )

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.SacrificeOwn(GameObjectFilter.CreatureOrLand) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "255"
        artist = "Howard Lyon"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d7ff937-de92-445f-976c-726fef5c91cc.jpg?1783916938"
        ruling(
            "2023-04-14",
            "The effect of Thalia and The Gitrog Monster's ability that allows you to play an additional " +
                "land is cumulative with similar effects. For example, if you control Thalia and The Gitrog " +
                "Monster and another permanent with that ability, you'll be able to play three lands during " +
                "each of your turns."
        )
        ruling(
            "2023-04-14",
            "As the last ability resolves, sacrificing a creature or land isn't optional. If you control a " +
                "creature or land (and if you control Thalia and The Gitrog Monster, you probably do), you " +
                "must sacrifice one."
        )
    }
}
