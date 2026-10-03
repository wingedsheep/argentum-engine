package com.wingedsheep.mtg.sets.definitions.c21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Angel of the Ruins — Commander 2021 #11 (reprinted in Modern Horizons 3 and Bloomburrow Commander)
 * {5}{W}{W} · Artifact Creature — Angel · 5/7
 *
 * Flying
 * When this creature enters, exile up to two target artifacts and/or enchantments.
 * Plainscycling {2}
 *
 * "Up to two" is an optional two-slot target requirement fanned out with
 * [Effects.ForEachTarget] over `ContextTarget(0)` — Force of Vigor's shape with exile instead of destroy.
 * The targets are not restricted by controller, so the Angel may exile your own permanents (or itself,
 * being an artifact).
 */
val AngelOfTheRuins = card("Angel of the Ruins") {
    manaCost = "{5}{W}{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Angel"
    power = 5
    toughness = 7
    oracleText = "Flying\n" +
        "When this creature enters, exile up to two target artifacts and/or enchantments.\n" +
        "Plainscycling {2} ({2}, Discard this card: Search your library for a Plains card, reveal it, " +
        "put it into your hand, then shuffle.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(
            TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Enchantment),
            count = 2,
            optional = true,
        )
        effect = Effects.ForEachTarget(
            Effects.Exile(EffectTarget.ContextTarget(0))
        )
        description = "When this creature enters, exile up to two target artifacts and/or enchantments."
    }

    keywordAbility(KeywordAbility.typecycling("Plains", ManaCost.parse("{2}")))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Viko Menezes"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea96229a-5c33-4f79-97d4-059947dd7617.jpg?1783927610"
    }
}
