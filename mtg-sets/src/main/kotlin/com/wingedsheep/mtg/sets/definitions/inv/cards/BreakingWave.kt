package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.TapUntapCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Breaking Wave
 * {2}{U}{U}
 * Sorcery
 * You may cast this spell as though it had flash if you pay {2} more to cast it.
 * Simultaneously untap all tapped creatures and tap all untapped creatures.
 *
 * The "pay {2} more to cast as though it had flash" clause is the Ghitu Fire pattern:
 * [KeywordAbility.flashKicker] — paying the extra cost unlocks instant-speed casting
 * without otherwise changing the spell.
 *
 * "Simultaneously" matters: a naive untap-then-tap (or tap-then-untap) would re-flip the
 * creatures it just touched. Instead both groups are snapshotted up front with two
 * [GatherCardsEffect] steps (tapped creatures, untapped creatures) before any tapping
 * happens, then the snapshots are applied via [TapUntapCollectionEffect]. Because the
 * collections are captured before mutation, the swap is atomic.
 */
val BreakingWave = card("Breaking Wave") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "You may cast this spell as though it had flash if you pay {2} more to cast it. " +
        "(You may cast it any time you could cast an instant.)\n" +
        "Simultaneously untap all tapped creatures and tap all untapped creatures."

    keywordAbility(KeywordAbility.flashKicker("{2}"))

    spell {
        effect = Effects.Pipeline {
            gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Creature.tapped(),
                    player = Player.Each,
                ),
                name = "breakingWave_tapped",
            )
            gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Creature.untapped(),
                    player = Player.Each,
                ),
                name = "breakingWave_untapped",
            )
            run(TapUntapCollectionEffect(collectionName = "breakingWave_tapped", tap = false))
            run(TapUntapCollectionEffect(collectionName = "breakingWave_untapped", tap = true))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "48"
        artist = "Carl Critchlow"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b39cd77-97aa-4099-8405-366f82079758.jpg?1562900378"
    }
}
