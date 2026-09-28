package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Assimilate Essence — March of the Machine #47
 * {1}{U} · Instant
 *
 * Counter target creature or battle spell unless its controller pays {4}. If they do, you incubate 2.
 *
 * The incubate rider is the `onPaid` half of `CounterUnlessPays`: it runs only when the spell's
 * controller actually pays, and the incubating player is the caster of Assimilate Essence.
 */
val AssimilateEssence = card("Assimilate Essence") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target creature or battle spell unless its controller pays {4}. " +
        "If they do, you incubate 2. (Create an Incubator token with two +1/+1 counters on it and " +
        "\"{2}: Transform this token.\" It transforms into a 0/0 Phyrexian artifact creature.)"

    spell {
        target(TargetFilter(GameObjectFilter.Creature or GameObjectFilter.Battle, zone = Zone.STACK))
        effect = Effects.CounterUnlessPays("{4}", onPaid = Effects.Incubate(2))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "47"
        artist = "Konstantin Porubov"
        flavorText = "\"You have no viable escape vector. Cease your resistance and accept perfection.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6ee12ba-0e6c-485a-a1ff-61726ed72fea.jpg?1783917045"
    }
}
