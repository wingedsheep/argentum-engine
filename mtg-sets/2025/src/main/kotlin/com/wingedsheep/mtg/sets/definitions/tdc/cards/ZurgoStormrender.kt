package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mobilize
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Zurgo Stormrender
 * {R}{W}{B}
 * Legendary Creature — Orc Warrior
 * 3/3
 * Mobilize 1
 * Whenever a creature token you control leaves the battlefield, draw a card if it was attacking.
 * Otherwise, each opponent loses 1 life.
 *
 * The leave ability is authored as its two mutually exclusive halves, split on the token's
 * last-known combat status (CR 603.10a / 608.2h). A resolution-time "if it was attacking" check
 * can't be used here: a token that left the battlefield ceases to exist (CR 704.5s) before the
 * ability resolves, leaving nothing to read. The trigger gate instead reads the battlefield-exit
 * snapshot's frozen `wasAttacking`, which can never change afterwards — so each departing token
 * fires exactly one of the two halves, the same outcome as the printed branch.
 */
val ZurgoStormrender = card("Zurgo Stormrender") {
    manaCost = "{R}{W}{B}"
    colorIdentity = "RWB"
    typeLine = "Legendary Creature — Orc Warrior"
    power = 3
    toughness = 3
    oracleText = "Mobilize 1 (Whenever this creature attacks, create a tapped and attacking 1/1 red " +
        "Warrior creature token. Sacrifice it at the beginning of the next end step.)\n" +
        "Whenever a creature token you control leaves the battlefield, draw a card if it was attacking. " +
        "Otherwise, each opponent loses 1 life."

    mobilize(1)

    triggeredAbility {
        trigger = Triggers.a(
            (GameObjectFilter.Creature and GameObjectFilter.Token).youControl().attacking()
        ).leaves()
        effect = Effects.DrawCards(1)
        description = "Whenever a creature token you control leaves the battlefield, draw a card if it was attacking."
    }

    triggeredAbility {
        trigger = Triggers.a(
            (GameObjectFilter.Creature and GameObjectFilter.Token).youControl()
                .withStatePredicate(StatePredicate.Not(StatePredicate.IsAttacking))
        ).leaves()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Whenever a creature token you control leaves the battlefield, if it wasn't attacking, " +
            "each opponent loses 1 life."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "10"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc8eb325-75c9-4331-830d-3f7d62d77290.jpg?1783907186"
        ruling("2025-04-04", "If Zurgo Stormrender leaves the battlefield at the same time as one or more creature tokens you control, its last ability will trigger once for each of those creature tokens.")
        ruling("2025-04-04", "Although the Warrior tokens enter as attacking creatures, they were never declared as attacking creatures. Abilities that trigger whenever a creature attacks won't trigger when the tokens enter attacking.")
    }
}
