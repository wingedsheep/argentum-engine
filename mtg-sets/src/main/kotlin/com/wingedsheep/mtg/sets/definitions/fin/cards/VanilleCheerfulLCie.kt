package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Vanille, Cheerful l'Cie
 * {3}{G}
 * Legendary Creature — Human Cleric
 * 3/2
 * When Vanille enters, mill two cards, then return a permanent card from your graveyard to your hand.
 * At the beginning of your first main phase, if you both own and control Vanille and a creature named
 * Fang, Fearless l'Cie, you may pay {3}{B}{G}. If you do, exile them, then meld them into Ragnarok,
 * Divine Deliverance.
 *
 * MELD OMITTED: meld is a blocked mechanic in this engine (see Brisela, Voice of Nightmares and the
 * partner card [FangFearlessLCie], which likewise omits its meld linkage). Following that precedent,
 * Vanille is authored with only its enters-the-battlefield ability; the meld trigger is intentionally
 * not wired. The meld result, [RagnarokDivineDeliverance], is still defined as a standalone card so it
 * exists in the corpus for when meld is supported.
 *
 * The ETB is a mandatory two-step: mill two ([Patterns.Library.mill]) so freshly-milled permanents
 * become eligible, then a resolution-time choice of one permanent card in your graveyard to move to
 * your hand (Gather → Select(1) → Move). If your graveyard holds no permanent card the selection
 * picks nothing and the ability finishes with nothing returned.
 */
val VanilleCheerfulLCie = card("Vanille, Cheerful l'Cie") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Cleric"
    power = 3
    toughness = 2
    oracleText = "When Vanille enters, mill two cards, then return a permanent card from your " +
        "graveyard to your hand.\n" +
        "At the beginning of your first main phase, if you both own and control Vanille and a " +
        "creature named Fang, Fearless l'Cie, you may pay {3}{B}{G}. If you do, exile them, then " +
        "meld them into Ragnarok, Divine Deliverance."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            Patterns.Library.mill(2),
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Permanent),
                storeAs = "vanilleGraveyard",
            ),
            SelectFromCollectionEffect(
                from = "vanilleGraveyard",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                storeSelected = "vanilleReturned",
                showAllCards = true,
                prompt = "Return a permanent card from your graveyard to your hand",
            ),
            MoveCollectionEffect(
                from = "vanilleReturned",
                destination = CardDestination.ToZone(Zone.HAND, Player.You),
            ),
        )
        description = "When Vanille enters, mill two cards, then return a permanent card from your " +
            "graveyard to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "211"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91226c1a-63a0-494e-bcf0-77c2d6f49213.jpg?1782686437"
    }
}
