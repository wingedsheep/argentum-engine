package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.effects.CounterKindSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Exotic Pets
 * {1}{W}{U}
 * Instant
 * Create two 1/1 blue Fish creature tokens with "This token can't be blocked." Then for each kind
 * of counter among creatures you control, put a counter of that kind on either of those tokens.
 *
 * The kinds are read after the Fish exist (they carry none, so they add nothing) and snapshotted
 * before the first placement. Per kind, the controller picks one of the two Fish on the
 * battlefield; the counter is placed through the ordinary counter path.
 */
val ExoticPets = card("Exotic Pets") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Instant"
    oracleText = "Create two 1/1 blue Fish creature tokens with \"This token can't be blocked.\" " +
        "Then for each kind of counter among creatures you control, put a counter of that kind on " +
        "either of those tokens."

    spell {
        effect = Effects.Pipeline {
            run(
                Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.BLUE),
                    creatureTypes = setOf("Fish"),
                    count = 2,
                    staticAbilities = listOf(CantBeBlocked(GroupFilter.source())),
                    imageUri = "https://cards.scryfall.io/normal/front/3/b/3b2e726b-f124-43f7-87e4-bb06bbe8e3d7.jpg?1783922964",
                )
            )
            forEachCounterKindAmong(GroupFilter.AllCreaturesYouControl) {
                val fish = chooseExactly(
                    1,
                    from = CollectionSlot.CreatedTokens,
                    prompt = "Choose a Fish to get a {counter} counter",
                    useTargetingUI = true,
                )
                run(Effects.AddCountersOfChosenKind(fish.asTarget(0), kindFrom = CounterKindSource.ITERATION))
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "185"
        artist = "Drew Baker"
        flavorText = "Investigators observed that Altonio's beloved fish were thriving after his disappearance."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/902f1ed8-5c10-45e4-8f2f-182e800faab4.jpg?1783923088"
    }
}
