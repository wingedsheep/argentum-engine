package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Laid to Rest — Innistrad: Crimson Vow #207
 * {3}{G} · Enchantment · Uncommon
 * Artist: Colin Boyer
 *
 * Whenever a Human you control dies, draw a card.
 * Whenever a creature you control with a +1/+1 counter on it dies, you gain 2 life.
 *
 * Both abilities are `Triggers.<subject>.leaves(to, excludeTo, excludeSacrifice)` with ANY binding, to = GRAVEYARD, and a
 * `youControl()` filter — the first narrowed by `withSubtype(HUMAN)`, the second by
 * `withCounter(PLUS_ONE_PLUS_ONE)`. The engine evaluates the trigger filter against
 * last-known-information for zone-change triggers (CR 603.10), so both the Human type and the
 * +1/+1 counter are read from the dying creature's captured state, not its already-gone live
 * state (same pattern as Explorer's Cache). The two triggers are independent — a Human with a
 * +1/+1 counter dying fires both (draw a card AND gain 2 life).
 */
val LaidToRest = card("Laid to Rest") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever a Human you control dies, draw a card.\n" +
        "Whenever a creature you control with a +1/+1 counter on it dies, you gain 2 life."

    // Whenever a Human you control dies, draw a card.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.youControl().withSubtype(Subtype.HUMAN)).dies()
        effect = Effects.DrawCards(1)
        description = "Whenever a Human you control dies, draw a card."
    }

    // Whenever a creature you control with a +1/+1 counter on it dies, you gain 2 life.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().withCounter(CounterType.PLUS_ONE_PLUS_ONE)).dies()
        effect = Effects.GainLife(2)
        description = "Whenever a creature you control with a +1/+1 counter on it dies, you gain 2 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "Colin Boyer"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/141f7948-3140-40f2-95dd-ab6c79f2a821.jpg?1783924809"
    }
}
