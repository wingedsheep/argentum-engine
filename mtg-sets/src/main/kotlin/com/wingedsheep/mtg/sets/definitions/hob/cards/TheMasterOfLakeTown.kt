package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The Master of Lake-town — The Hobbit #77
 * {1}{B}{B} · Legendary Creature — Human Advisor · Rare
 * 3/2
 *
 * Deathtouch
 * Whenever a player loses life, that player mills that many cards. (Damage causes loss of life.)
 * When The Master of Lake-town dies, draw a card for each graveyard with seven or more cards in it.
 *
 * Modeling notes:
 *  - The mill trigger is [Triggers.AnyPlayerLosesLife], so it fires for *every* player including
 *    its own controller, once per life-loss event. The amount rides on the triggering
 *    [com.wingedsheep.engine.core.LifeChangedEvent] via
 *    [ContextPropertyKey.TRIGGER_LIFE_LOST], and [Player.TriggeringPlayer] sends the mill at the
 *    player who lost the life — not at the controller.
 *  - "Each graveyard with seven or more cards in it" counts *every* player's graveyard, yours
 *    included, so the scope is [Player.Each] rather than [Player.EachOpponent].
 *    [DynamicAmount.CountPlayersWith] rebinds `Player.You` inside the condition to each candidate
 *    player in turn, which makes [Conditions.CardsInGraveyardAtLeast] the per-graveyard test.
 *  - The dies trigger resolves after The Master of Lake-town has already reached the graveyard, so
 *    it counts itself toward its controller's seven.
 */
val TheMasterOfLakeTown = card("The Master of Lake-town") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Advisor"
    power = 3
    toughness = 2
    oracleText = "Deathtouch\n" +
        "Whenever a player loses life, that player mills that many cards. " +
        "(Damage causes loss of life.)\n" +
        "When The Master of Lake-town dies, draw a card for each graveyard with seven or more " +
        "cards in it."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.AnyPlayerLosesLife
        effect = Patterns.Library.mill(
            count = DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_LIFE_LOST),
            target = EffectTarget.PlayerRef(Player.TriggeringPlayer)
        )
        description = "Whenever a player loses life, that player mills that many cards."
    }

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.DrawCards(
            DynamicAmount.CountPlayersWith(
                scope = Player.Each,
                condition = Conditions.CardsInGraveyardAtLeast(7)
            )
        )
        description = "When The Master of Lake-town dies, draw a card for each graveyard with " +
            "seven or more cards in it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "77"
        artist = "Marius Bota"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/3788ada6-34a9-41af-a31c-2d090550e503.jpg?1784632114"
    }
}
