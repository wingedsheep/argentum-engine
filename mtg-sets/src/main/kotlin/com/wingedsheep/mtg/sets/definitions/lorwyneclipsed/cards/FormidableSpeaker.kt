package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Formidable Speaker
 * {2}{G}
 * Creature — Elf Druid
 * 2/4
 *
 * When this creature enters, you may discard a card. If you do, search your library
 * for a creature card, reveal it, put it into your hand, then shuffle.
 * {1}, {T}: Untap another target permanent.
 */
val FormidableSpeaker = card("Formidable Speaker") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Elf Druid"
    power = 2
    toughness = 4
    oracleText = "When this creature enters, you may discard a card. If you do, search your library for a creature card, reveal it, put it into your hand, then shuffle.\n" +
        "{1}, {T}: Untap another target permanent."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            effect = IfYouDoEffect(
                action = EffectPatterns.discardCards(1),
                ifYouDo = EffectPatterns.searchLibrary(
                    filter = GameObjectFilter.Creature,
                    count = 1,
                    destination = SearchDestination.HAND,
                    reveal = true,
                    shuffleAfter = true
                )
            ),
            descriptionOverride = "You may discard a card. If you do, search your library for a creature card, reveal it, put it into your hand, then shuffle."
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val targetPermanent = target(
            "another target permanent",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.Permanent, excludeSelf = true))
        )
        effect = Effects.Untap(targetPermanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "176"
        artist = "Aurore Folny"
        flavorText = "Jean-Emmanuel Depraz, World Champion XXIX"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/265522eb-4f6a-40e7-b374-3833fa63c80b.jpg?1765118668"
    }
}
