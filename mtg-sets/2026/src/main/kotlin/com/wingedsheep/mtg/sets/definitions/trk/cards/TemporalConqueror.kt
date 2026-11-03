package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Temporal Conqueror
 * {2}{G}
 * Creature — Na'kuhl Warrior
 * 2/3
 *
 * When this creature enters, mill three cards. Then return a land card from your graveyard to
 * your hand. If you can't, create a Treasure token.
 *
 * Mill first, then gather every land card in your graveyard (freshly milled ones included). The
 * return is mandatory and untargeted, so it is a resolution-time `chooseExactly(1)`; when the
 * graveyard holds no land card the `orElse` branch creates the Treasure ("if you can't").
 */
val TemporalConqueror = card("Temporal Conqueror") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Na'kuhl Warrior"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, mill three cards. Then return a land card from your " +
        "graveyard to your hand. If you can't, create a Treasure token. (To mill three cards, put " +
        "the top three cards from your library into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            run(Patterns.Library.mill(3))
            val lands = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, Filters.Land))
            ifNotEmpty(lands) {
                val chosen = chooseExactly(
                    1,
                    from = lands,
                    prompt = "Return a land card from your graveyard to your hand",
                    selectedLabel = "Return to hand",
                    remainderLabel = "Leave in graveyard"
                )
                toHand(chosen)
            } orElse {
                run(Effects.CreateTreasure(1))
            }
        }
        description = "When this creature enters, mill three cards. Then return a land card from " +
            "your graveyard to your hand. If you can't, create a Treasure token."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "219"
        artist = "Douglas Shuler"
        flavorText = "The Na'kuhl owed their empire to the unique strategy of engaging enemies " +
            "along a four-dimensional front."
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0bb51a7-1ae2-45d0-8be9-5454a3c8f751.jpg?1793361377"
    }
}
