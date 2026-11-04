package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.div
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Zodiark, Umbral God
 * {B}{B}{B}{B}{B}
 * Legendary Creature — God
 * 5/5
 *
 * Indestructible
 * When Zodiark enters, each player sacrifices half the non-God creatures they control of their
 * choice, rounded down.
 * Whenever a player sacrifices another creature, put a +1/+1 counter on Zodiark.
 *
 * Modeling:
 *  - The enter trigger is a per-player edict: `Effects.Sacrifice(target = Each,
 *    countPerPlayer = true)` reads the count once per player with `Player.You` bound to *that*
 *    player, and every player's sacrifice is one simultaneous event (CR 101.4). Each player
 *    sacrifices, of their own choice, a dynamic count of `Divide(<their non-God creatures>, 2,
 *    roundUp = false)` — the same shape Rush of Dread uses for "half … rounded up", flipped to
 *    round down. The non-God filter is `Creature.notSubtype(GOD)`, so a player's own Gods (and
 *    Zodiark) are never sacrificed and don't count toward the half.
 *  - The counter trigger uses the "a player sacrifices" scope
 *    ([EventPattern.PermanentsSacrificedEvent.sacrificedBy] = [Player.Each], ANY binding): it fires for any
 *    player's sacrifices, not just Zodiark's controller's. Following the engine's batching
 *    convention for sacrifice triggers, it fires once per sacrificing player per batch. The filter
 *    is `Creature`; "another" is satisfied in practice because Zodiark is indestructible and, as a
 *    God, is excluded from its own edict — the only way it would count itself is if some other
 *    effect sacrifices it, at which point the counter is moot.
 */
val ZodiarkUmbralGod = card("Zodiark, Umbral God") {
    manaCost = "{B}{B}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — God"
    power = 5
    toughness = 5
    oracleText = "Indestructible\n" +
        "When Zodiark enters, each player sacrifices half the non-God creatures they control of " +
        "their choice, rounded down.\n" +
        "Whenever a player sacrifices another creature, put a +1/+1 counter on Zodiark."

    keywords(Keyword.INDESTRUCTIBLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Sacrifice(
            filter = GameObjectFilter.Creature.notSubtype(Subtype.GOD),
            count = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature.notSubtype(Subtype.GOD)
            ).count() / 2,
            target = EffectTarget.PlayerRef(Player.Each),
            countPerPlayer = true
        )
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.sacrifices(GameObjectFilter.Creature, batch = true)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "128"
        artist = "AKAGI"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9ba292d5-5139-42ea-950d-0a638445277f.jpg?1782686502"
    }
}
