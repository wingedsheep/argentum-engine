package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Into the Fire
 * {2}{R}
 * Sorcery
 * Choose one —
 * • Into the Fire deals 2 damage to each creature, planeswalker, and battle.
 * • Put any number of cards from your hand on the bottom of your library, then draw that many
 *   cards plus one.
 *
 * Mode 2 is a Gather → Select(any number) → Move → Draw pipeline; the draw counts the chosen
 * collection by entity id, so it survives the cards having left the hand. Zero cards chosen
 * still draws one.
 */
val IntoTheFire = card("Into the Fire") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Into the Fire deals 2 damage to each creature, planeswalker, and battle.\n" +
        "• Put any number of cards from your hand on the bottom of your library, then draw that " +
        "many cards plus one."

    spell {
        modal(chooseCount = 1) {
            mode("Into the Fire deals 2 damage to each creature, planeswalker, and battle") {
                effect = Patterns.Group.dealDamageToAll(
                    amount = 2,
                    filter = GroupFilter(GameObjectFilter.CreaturePlaneswalkerOrBattle),
                )
            }
            mode("Put any number of cards from your hand on the bottom of your library, then draw that many cards plus one") {
                effect = Effects.Pipeline {
                    val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                    val chosen = chooseAnyNumber(hand)
                    toLibraryBottom(chosen)
                    run(Effects.DrawCards(DynamicAmounts.distinctEntitiesIn(chosen) + 1))
                }
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "144"
        artist = "Grzegorz Rutkowski"
        flavorText = "The rest of the Gatewatch retreated. Chandra and Wrenn went in blazing."
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b36eeb4-9967-46aa-931c-fceed59fe49a.jpg?1783916711"
    }
}
