package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Scout the City
 * {1}{G}
 * Sorcery
 *
 * Choose one —
 * • Look Around — Mill three cards. You may put a permanent card from among them into
 *   your hand. You gain 3 life.
 * • Bring Down — Destroy target creature with flying.
 */
val ScoutTheCity = card("Scout the City") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n• Look Around — Mill three cards. You may put a permanent card from among them into your hand. You gain 3 life. (To mill three cards, put the top three cards of your library into your graveyard.)\n• Bring Down — Destroy target creature with flying."

    spell {
        modal(chooseCount = 1) {
            mode("Look Around — Mill three cards. You may put a permanent card from among them into your hand. You gain 3 life") {
                effect = Effects.Composite(
                    // Mill three: gather top 3, move to graveyard
                    GatherCardsEffect(
                        source = CardSource.TopOfLibrary(DynamicAmount.Fixed(3)),
                        storeAs = "milled"
                    ),
                    MoveCollectionEffect(
                        from = "milled",
                        destination = CardDestination.ToZone(Zone.GRAVEYARD)
                    ),
                    // You may put a permanent card from among the milled cards into your hand
                    SelectFromCollectionEffect(
                        from = "milled",
                        selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                        filter = GameObjectFilter.Permanent,
                        storeSelected = "selected",
                        showAllCards = true,
                        prompt = "You may put a permanent card into your hand",
                        selectedLabel = "Put in hand",
                        remainderLabel = "Leave in graveyard"
                    ),
                    MoveCollectionEffect(
                        from = "selected",
                        destination = CardDestination.ToZone(Zone.HAND)
                    ),
                    Effects.GainLife(3)
                )
            }
            mode("Bring Down — Destroy target creature with flying") {
                val t = target("target", TargetCreature(filter = TargetFilter.Creature.withKeyword(Keyword.FLYING)))
                effect = Effects.Move(t, Zone.GRAVEYARD, byDestruction = true)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Rafater"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90b9504d-d23d-402f-8b16-1964ebd8f6b9.jpg?1783905324"
    }
}
