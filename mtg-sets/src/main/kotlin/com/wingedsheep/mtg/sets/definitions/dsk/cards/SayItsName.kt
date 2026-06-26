package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Say Its Name
 * {1}{G}
 * Sorcery
 * Mill three cards. Then you may return a creature or land card from your graveyard to your hand.
 * Exile this card and two other cards named Say Its Name from your graveyard: Search your graveyard,
 * hand, and/or library for a card named Altanak, the Thrice-Called and put it onto the battlefield.
 * If you search your library this way, shuffle. Activate only as a sorcery.
 *
 * Spell half:
 *  - "Mill three cards." → [Patterns.Library.mill].
 *  - "Then you may return a creature or land card from your graveyard to your hand." modeled as a
 *    resolution-time Gather → Select(ChooseUpTo 1) → Move(hand) pipeline rather than a cast-time
 *    target, because the milled cards only reach the graveyard during resolution and must be eligible
 *    to return (CR 608.2). The "you may" falls out of [SelectionMode.ChooseUpTo] of 1 (the player can
 *    select zero).
 *
 * Graveyard-activated ability (CR 113.6: an activated ability that functions from the graveyard):
 *  - Cost: exile this card plus two other cards named "Say Its Name" from your graveyard — a flat
 *    [Costs.ExileFromGraveyard] of 3 cards filtered to the card's own name. The source card itself is
 *    one valid choice, matching "this card and two other cards named Say Its Name" (3 total).
 *  - Effect: search graveyard, hand, and library ([CardSource.FromMultipleZones]) for a card named
 *    "Altanak, the Thrice-Called", choose up to one, and put it onto the battlefield. The library is
 *    always among the searched zones, so we always [ShuffleLibraryEffect] afterward ("If you search
 *    your library this way, shuffle").
 *  - [TimingRule.SorcerySpeed] enforces "Activate only as a sorcery."
 */
val SayItsName = card("Say Its Name") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Mill three cards. Then you may return a creature or land card from your graveyard to your hand.\n" +
        "Exile this card and two other cards named Say Its Name from your graveyard: Search your " +
        "graveyard, hand, and/or library for a card named Altanak, the Thrice-Called and put it onto " +
        "the battlefield. If you search your library this way, shuffle. Activate only as a sorcery."

    spell {
        effect = Effects.Composite(
            Patterns.Library.mill(3),
            GatherCardsEffect(
                source = CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.You,
                    filter = GameObjectFilter.CreatureOrLand
                ),
                storeAs = "sayItsNameReturn"
            ),
            SelectFromCollectionEffect(
                from = "sayItsNameReturn",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                storeSelected = "sayItsNameChosen",
                prompt = "You may return a creature or land card from your graveyard to your hand"
            ),
            MoveCollectionEffect(
                from = "sayItsNameChosen",
                destination = CardDestination.ToZone(Zone.HAND)
            )
        )
    }

    // Exile this card and two other cards named Say Its Name from your graveyard: search graveyard,
    // hand, and/or library for Altanak, the Thrice-Called and put it onto the battlefield.
    activatedAbility {
        cost = Costs.ExileFromGraveyard(3, GameObjectFilter.Any.named("Say Its Name"))
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.FromMultipleZones(
                    zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                    player = Player.You,
                    filter = GameObjectFilter.Any.named("Altanak, the Thrice-Called")
                ),
                storeAs = "altanak"
            ),
            SelectFromCollectionEffect(
                from = "altanak",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                storeSelected = "altanakChosen",
                prompt = "Search for Altanak, the Thrice-Called to put onto the battlefield"
            ),
            MoveCollectionEffect(
                from = "altanakChosen",
                destination = CardDestination.ToZone(Zone.BATTLEFIELD)
            ),
            // If you search your library this way, shuffle. The library is always searched, so always shuffle.
            ShuffleLibraryEffect()
        )
        description = "Exile this card and two other cards named Say Its Name from your " +
            "graveyard: Search your graveyard, hand, and/or library for a card named Altanak, the " +
            "Thrice-Called and put it onto the battlefield. If you search your library this way, " +
            "shuffle. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "197"
        artist = "Sam Wolfe Connelly"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94c58683-b5f2-4863-9562-6f6be1ec21fe.jpg?1726286600"
    }
}
