package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wildest Dreams {X}{X}{G}
 * Sorcery
 *
 * Return X target cards from your graveyard to your hand. Exile Wildest Dreams.
 *
 * "X target cards" is exactly X targets (`exactly = xValue`, the Distorting Wake / Shigeki shape);
 * {X}{X} is paid twice but X is still the one announced value. "Exile Wildest Dreams." is
 * [selfExile] — the spell goes to exile instead of the graveyard as it resolves. Per the ruling, a
 * Wildest Dreams whose targets are all illegal doesn't resolve and goes to the graveyard instead:
 * the fizzle path never reads the self-exile flag.
 */
val WildestDreams = card("Wildest Dreams") {
    manaCost = "{X}{X}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Return X target cards from your graveyard to your hand. Exile Wildest Dreams."

    spell {
        selfExile()
        targets(
            TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD),
            exactly = DynamicAmounts.xValue()
        )
        effect = Effects.Pipeline {
            val wildestDreamsTargets = gather(CardSource.ChosenTargets)
            toHand(wildestDreamsTargets)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "174"
        artist = "Daniel Ljunggren"
        flavorText = "\"I opened my mind to the immensity of the wonders around me. It was only then " +
            "that I could see the Great Conduit.\"\n—Rashmi, aether-seer"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9fedd63c-22e4-4c36-8a7a-a167a070678f.jpg?1783937172"
        ruling(
            "2016-09-20",
            "If each target card is an illegal target as Wildest Dreams resolves, Wildest Dreams " +
                "doesn't resolve and is put into its owner's graveyard. It's not exiled."
        )
    }
}
