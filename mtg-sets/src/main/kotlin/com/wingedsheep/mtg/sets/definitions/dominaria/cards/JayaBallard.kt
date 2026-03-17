package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Jaya Ballard
 * {2}{R}{R}{R}
 * Legendary Planeswalker — Jaya
 * Starting Loyalty: 5
 *
 * +1: Add {R}{R}{R}. Spend this mana only to cast instant or sorcery spells.
 * +1: Discard up to three cards, then draw that many cards.
 * −8: You get an emblem with "You may cast instant and sorcery spells from your graveyard.
 *     If a spell cast this way would be put into your graveyard, exile it instead."
 *
 * Note: The "spend this mana only to cast instant or sorcery spells" restriction on the first
 * ability is not yet enforced — the engine does not support mana spending restrictions.
 *
 * Note: The −8 emblem ability is not yet implemented — it requires engine support for graveyard
 * casting permission as a static emblem ability and a replacement effect to exile spells cast
 * from graveyard instead of returning them there.
 */
val JayaBallard = card("Jaya Ballard") {
    manaCost = "{2}{R}{R}{R}"
    typeLine = "Legendary Planeswalker — Jaya"
    startingLoyalty = 5
    oracleText = "+1: Add {R}{R}{R}. Spend this mana only to cast instant or sorcery spells.\n+1: Discard up to three cards, then draw that many cards.\n\u22128: You get an emblem with \"You may cast instant and sorcery spells from your graveyard. If a spell cast this way would be put into your graveyard, exile it instead.\""

    // +1: Add {R}{R}{R}
    // TODO: "Spend this mana only to cast instant or sorcery spells" restriction not yet enforced
    loyaltyAbility(+1) {
        description = "+1: Add {R}{R}{R}. Spend this mana only to cast instant or sorcery spells."
        effect = AddManaEffect(Color.RED, 3)
    }

    // +1: Discard up to three cards, then draw that many cards
    loyaltyAbility(+1) {
        description = "+1: Discard up to three cards, then draw that many cards."
        effect = CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.You),
                    storeAs = "hand"
                ),
                SelectFromCollectionEffect(
                    from = "hand",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(3)),
                    storeSelected = "toDiscard"
                ),
                MoveCollectionEffect(
                    from = "toDiscard",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD),
                    moveType = MoveType.Discard
                ),
                DrawCardsEffect(DynamicAmount.VariableReference("toDiscard_count"))
            )
        )
    }

    // −8: Emblem — not yet implemented (requires new engine mechanics)
    // TODO: Implement graveyard casting emblem when engine supports:
    //  - Static emblem abilities (not just triggered)
    //  - Graveyard casting permission for instant/sorcery spells
    //  - Replacement effect: exile instead of graveyard for spells cast this way
    loyaltyAbility(-8) {
        description = "\u22128: You get an emblem with \"You may cast instant and sorcery spells from your graveyard. If a spell cast this way would be put into your graveyard, exile it instead.\""
        effect = CompositeEffect(emptyList())
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "132"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa25fedc-6038-48c8-b42a-817ac186492e.jpg?1562746099"
        ruling("2018-06-08", "Because it's a loyalty ability, Jaya's first ability isn't a mana ability. It can be activated only any time you could cast a sorcery.")
        ruling("2018-04-27", "Mana produced by Jaya's first ability can be spent among any number of instant and/or sorcery spells.")
        ruling("2018-04-27", "You choose how many cards to discard while Jaya's second ability is resolving. You can choose to discard zero cards this way (and then draw zero cards) if you wish.")
        ruling("2018-04-27", "Jaya's emblem doesn't grant you permission to do anything with instant and sorcery cards in your graveyard except cast them.")
    }
}
