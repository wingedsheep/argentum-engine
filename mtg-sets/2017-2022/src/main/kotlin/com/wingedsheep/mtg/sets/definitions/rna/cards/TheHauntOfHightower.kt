package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Haunt of Hightower — Ravnica Allegiance #273
 * {4}{B}{B} · Legendary Creature — Vampire · Mythic
 *
 * Flying, lifelink
 * Whenever The Haunt of Hightower attacks, defending player discards a card.
 * Whenever a card is put into an opponent's graveyard from anywhere, put a +1/+1 counter on The
 * Haunt of Hightower.
 *
 * The attack trigger is Shrieking Specter's; the graveyard trigger is Vulturous Zombie's: a per-card
 * zone change to the graveyard with no `from` zone, nontoken (a token isn't a card), keyed on the
 * card's **owner** being an opponent — one counter per card, so a mill of five grows it by five.
 */
val TheHauntOfHightower = card("The Haunt of Hightower") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Vampire"
    power = 3
    toughness = 3
    oracleText = "Flying, lifelink\n" +
        "Whenever The Haunt of Hightower attacks, defending player discards a card.\n" +
        "Whenever a card is put into an opponent's graveyard from anywhere, put a +1/+1 counter " +
        "on The Haunt of Hightower."

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Discard(1, EffectTarget.PlayerRef(Player.DefendingPlayer))
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Any
                    .withCardPredicate(CardPredicate.IsNontoken)
                    .ownedByOpponent()).changesZone(to = Zone.GRAVEYARD)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever a card is put into an opponent's graveyard from anywhere, put a " +
            "+1/+1 counter on The Haunt of Hightower."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "273"
        artist = "Lius Lasahido"
        flavorText = "It drains vitality from those it touches."
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61a908e8-6952-46c0-94ec-3962b7a4caef.jpg?1783933608"
        ruling(
            "2019-01-25",
            "If The Haunt of Hightower is attacking a planeswalker, the controller of the " +
                "planeswalker is the defending player."
        )
        ruling(
            "2019-01-25",
            "The Haunt of Hightower's last ability won't trigger when a token is put into an " +
                "opponent's graveyard."
        )
    }
}
