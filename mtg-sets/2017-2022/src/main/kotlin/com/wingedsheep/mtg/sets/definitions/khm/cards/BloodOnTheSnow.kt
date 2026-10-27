package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Blood on the Snow
 * {4}{B}{B}
 * Snow Sorcery
 * Choose one —
 * • Destroy all creatures.
 * • Destroy all planeswalkers.
 * Then return a creature or planeswalker card with mana value X or less from your graveyard to
 * the battlefield, where X is the amount of {S} spent to cast this spell.
 *
 * The return happens whichever mode was chosen, so it is the shared tail of both modes. It doesn't
 * target: the card is chosen on resolution, after the destruction, so a creature or planeswalker the
 * spell just destroyed is eligible. X is the snow mana spent on the cast (`SnowManaSpent`), 0 for a
 * copy or a cast without snow mana; the cap reads the card's mana value in the graveyard ({X} = 0).
 */
private val returnCreatureOrPlaneswalker = Effects.Pipeline {
    val eligible = gather(
        CardSource.FromZone(
            Zone.GRAVEYARD,
            Player.You,
            GameObjectFilter.CreatureOrPlaneswalker.manaValueAtMostDynamic(DynamicAmounts.snowManaSpent())
        )
    )
    val chosen = chooseExactly(
        1,
        from = eligible,
        prompt = "Return a creature or planeswalker card with mana value X or less to the battlefield"
    )
    move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD))
}

val BloodOnTheSnow = card("Blood on the Snow") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Snow Sorcery"
    oracleText = "Choose one —\n" +
        "• Destroy all creatures.\n" +
        "• Destroy all planeswalkers.\n" +
        "Then return a creature or planeswalker card with mana value X or less from your graveyard " +
        "to the battlefield, where X is the amount of {S} spent to cast this spell. " +
        "({S} is mana from a snow source.)"

    spell {
        modal(chooseCount = 1) {
            mode("Destroy all creatures") {
                effect = Effects.DestroyAll(GameObjectFilter.Creature) then returnCreatureOrPlaneswalker
            }
            mode("Destroy all planeswalkers") {
                effect = Effects.DestroyAll(GameObjectFilter.Planeswalker) then returnCreatureOrPlaneswalker
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "79"
        artist = "Martina Fačková"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8606f40-0af4-443b-a413-a88dc3e8f32e.jpg?1783928254"
        ruling("2021-02-05", "You choose a mode for Blood on the Snow as you cast it. The last part of the effect (returning a card to the battlefield) happens no matter which mode you choose.")
        ruling("2021-02-05", "Blood on the Snow doesn't target any creature or planeswalker card in your graveyard. You choose which one to return to the battlefield, if any, as Blood on the Snow resolves. Notably, you could return one of the creatures or planeswalkers that was just destroyed by Blood on the Snow.")
        ruling("2021-02-05", "If a card in a graveyard has {X} in its mana cost, X is considered to be 0.")
        ruling("2021-02-05", "If you cast Blood on the Snow without spending snow mana, you can return only a creature or planeswalker card with mana value 0.")
        ruling("2021-02-05", "If Blood on the Snow is copied, no mana was spent to cast the copy, so you can return only a creature or planeswalker card with mana value 0.")
    }
}
