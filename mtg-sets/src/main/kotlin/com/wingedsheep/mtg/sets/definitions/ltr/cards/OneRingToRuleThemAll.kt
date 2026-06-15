package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * One Ring to Rule Them All
 * {2}{B}{B}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — The Ring tempts you, then each player mills cards equal to your Ring-bearer's power.
 * II — Destroy all nonlegendary creatures.
 * III — Each opponent loses 1 life for each creature card in that player's graveyard.
 *
 * Chapter I composes [Effects.TheRingTemptsYou] with a mill-each-player whose amount is
 * `EntityProperty(RingBearer(Player.You), Power)` — the power of the Saga controller's
 * designated Ring-bearer (0 if none). The mill applies to every player ([Player.Each]) while
 * the amount stays anchored to *your* Ring-bearer, because [EntityReference.RingBearer] reads
 * the referenced player's bearer rather than the player being milled.
 *
 * Chapter II is a board wipe over `Creature.nonlegendary()` (legendary creatures survive).
 *
 * Chapter III runs per opponent ([Player.EachOpponent]): each loses life equal to the number
 * of creature cards in *that* player's graveyard. [Effects.ForEachPlayer] rebinds the loop's
 * controller to each opponent, so `Player.You` inside resolves to the opponent being processed
 * — hence `Count(Player.You, GRAVEYARD, Creature)` counts that opponent's own graveyard.
 */
val OneRingToRuleThemAll = card("One Ring to Rule Them All") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — The Ring tempts you, then each player mills cards equal to your Ring-bearer's power.\n" +
        "II — Destroy all nonlegendary creatures.\n" +
        "III — Each opponent loses 1 life for each creature card in that player's graveyard."

    sagaChapter(1) {
        effect = Effects.Composite(
            Effects.TheRingTemptsYou(),
            Patterns.Library.mill(
                count = DynamicAmount.EntityProperty(
                    EntityReference.RingBearer(Player.You),
                    EntityNumericProperty.Power
                ),
                target = EffectTarget.PlayerRef(Player.Each)
            )
        )
    }

    sagaChapter(2) {
        effect = Effects.DestroyAll(GameObjectFilter.Creature.nonlegendary())
    }

    sagaChapter(3) {
        effect = Effects.ForEachPlayer(
            players = Player.EachOpponent,
            effects = listOf(
                Effects.LoseLife(
                    amount = DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature),
                    target = EffectTarget.Controller
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "102"
        artist = "L J Koh"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb2dc2e0-f393-4442-818b-d3b860bfffd0.jpg?1688569235"
    }
}
