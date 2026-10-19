package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Kura, the Boundless Sky — Kamigawa: Neon Dynasty #200 (canonical printing)
 * {3}{G}{G} · Legendary Creature — Dragon Spirit · 4/4
 *
 * Flying, deathtouch
 * When Kura dies, choose one —
 * • Search your library for up to three land cards, reveal them, put them into your hand, then shuffle.
 * • Create an X/X green Spirit creature token, where X is the number of lands you control.
 *
 * Ao, the Dawn Sky's modal dies trigger. Mode one is Seek the Horizon's up-to-three revealed search
 * widened to any land; mode two is Abzan Monument's X/X [Effects.CreateDynamicToken] with X read at
 * resolution from [DynamicAmounts.landsYouControl].
 */
val KuraTheBoundlessSky = card("Kura, the Boundless Sky") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Dragon Spirit"
    power = 4
    toughness = 4
    oracleText = "Flying, deathtouch\n" +
        "When Kura dies, choose one —\n" +
        "• Search your library for up to three land cards, reveal them, put them into your hand, then shuffle.\n" +
        "• Create an X/X green Spirit creature token, where X is the number of lands you control."

    keywords(Keyword.FLYING, Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.Land,
                    count = 3,
                    destination = SearchDestination.HAND,
                    shuffleAfter = true,
                    reveal = true
                ),
                "Search your library for up to three land cards, reveal them, put them into your hand, " +
                    "then shuffle.",
            ),
            Mode.noTarget(
                Effects.CreateDynamicToken(
                    dynamicPower = DynamicAmounts.landsYouControl(),
                    dynamicToughness = DynamicAmounts.landsYouControl(),
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Spirit"),
                    imageUri = "https://cards.scryfall.io/normal/front/d/2/d237377d-b79b-4f27-a142-e2c1756e0d94.jpg?1783923712"
                ),
                "Create an X/X green Spirit creature token, where X is the number of lands you control.",
            ),
        )
        description = "When Kura dies, choose one — • Search your library for up to three land cards, reveal " +
            "them, put them into your hand, then shuffle. • Create an X/X green Spirit creature token, where X " +
            "is the number of lands you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "200"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06b25752-26be-4189-bc8b-828db6f0e612.jpg?1783923843"
    }
}
