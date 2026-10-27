package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Perpetual Timepiece
 * {2}
 * Artifact
 * {T}: Mill two cards. (Put the top two cards of your library into your graveyard.)
 * {2}, Exile this artifact: Shuffle any number of target cards from your graveyard into your library.
 *
 * The shuffle is unconditional so that activating with zero targets still shuffles your library
 * (2016-09-20 ruling); if targets were chosen and all became illegal, the ability doesn't resolve.
 */
val PerpetualTimepiece = card("Perpetual Timepiece") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "{T}: Mill two cards. (Put the top two cards of your library into your graveyard.)\n" +
        "{2}, Exile this artifact: Shuffle any number of target cards from your graveyard into your library."

    activatedAbility {
        cost = Costs.Tap
        effect = Patterns.Library.mill(2)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.ExileSelf)
        targets(TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD), unlimited = true)
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.LIBRARY)
        ) then Effects.ShuffleLibrary()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "Cliff Childs"
        flavorText = "Do not be so caught up in keeping track of time that you forget to make use of it."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbc87a27-93d9-4ce0-a022-0309242c1e89.jpg?1783937150"
        ruling(
            "2016-09-20",
            "You can activate Perpetual Timepiece's second ability with no targets just to shuffle your " +
                "library. If you choose any targets and they all become illegal targets, the ability won't " +
                "resolve and you won't shuffle your library."
        )
    }
}
