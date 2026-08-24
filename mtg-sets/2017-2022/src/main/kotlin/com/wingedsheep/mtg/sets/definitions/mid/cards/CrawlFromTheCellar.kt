package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Crawl from the Cellar
 * {B}
 * Sorcery
 * Return target creature card from your graveyard to your hand. Put a +1/+1 counter on up to one
 * target Zombie you control.
 * Flashback {3}{B}
 *
 * Two targets: a creature card in your graveyard (required) and — "up to one" — a Zombie you
 * control (optional). The optional counter target is skipped when nothing is chosen (Cruel Revival
 * shape). Flashback is the standard [KeywordAbility.flashback] special action.
 *
 * The second target is a **Zombie permanent**, not a Zombie creature: Oracle's bare tribal noun
 * means every permanent of that type, so a Zombie enchantment or a Zombie land is a legal choice.
 */
val CrawlFromTheCellar = card("Crawl from the Cellar") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return target creature card from your graveyard to your hand. Put a +1/+1 counter " +
        "on up to one target Zombie you control.\n" +
        "Flashback {3}{B} (You may cast this card from your graveyard for its flashback cost. Then " +
        "exile it.)"

    spell {
        val creatureCard = target(
            "creature card in your graveyard",
            TargetObject(filter = TargetFilter.CreatureInYourGraveyard)
        )
        val zombie = target(
            "Zombie you control",
            TargetObject(
                optional = true,
                filter = TargetFilter(GameObjectFilter.Permanent.withSubtype("Zombie").youControl())
            )
        )

        effect = Effects.Move(creatureCard, Zone.HAND)
            .then(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, zombie))
    }

    keywordAbility(KeywordAbility.flashback("{3}{B}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Igor Krstic"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed6c8942-d43e-4456-ac98-3220cb954c65.jpg?1782703672"
    }
}
