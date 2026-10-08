package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

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
 * The meld trigger: "your first main phase" is the precombat main step. The whole intervening "if" — own
 * and control both — is the trigger condition, so a stolen Vanille or Fang never offers the payment. "You may pay {3}{B}{G}. If you do" is [Effects.MayPay]
 * gating the meld into [RagnarokDivineDeliverance] (paired with [FangFearlessLCie]).
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
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            run(Patterns.Library.mill(2))
            val vanilleGraveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Permanent))
            val vanilleReturned = chooseExactly(
                1,
                from = vanilleGraveyard,
                showAllCards = true,
                prompt = "Return a permanent card from your graveyard to your hand"
            )
            toHand(vanilleReturned)
        }
        description = "When Vanille enters, mill two cards, then return a permanent card from your " +
            "graveyard to your hand."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        interveningIf = Conditions.All(
            Conditions.SourceMatches(GameObjectFilter.Any.ownedByYou().youControl()),
            Conditions.YouControl(Filters.Creature.named("Fang, Fearless l'Cie").ownedByYou())
        )
        effect = Effects.MayPay(
            ManaCost.parse("{3}{B}{G}"),
            then = Effects.Meld(Filters.Creature.named("Fang, Fearless l'Cie"), into = "Ragnarok, Divine Deliverance")
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "211"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91226c1a-63a0-494e-bcf0-77c2d6f49213.jpg?1782686437"
    }
}
