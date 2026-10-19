package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shigeki, Jukai Visionary — Kamigawa: Neon Dynasty #206 (canonical printing)
 * {1}{G} · Legendary Enchantment Creature — Snake Druid · 1/3 · Rare
 *
 * {1}{G}, {T}, Return Shigeki to its owner's hand: Reveal the top four cards of your library. You
 * may put a land card from among them onto the battlefield tapped. Put the rest into your
 * graveyard.
 * Channel — {X}{X}{G}{G}, Discard this card: Return X target nonlegendary cards from your
 * graveyard to your hand.
 *
 * - Returning Shigeki is part of the **cost** ([Costs.ReturnSelfToHand], Maze's End's shape), so
 *   the ability still resolves with Shigeki gone, and it can't be activated the turn Shigeki
 *   enters (the {T} needs it untapped and free of summoning sickness).
 * - The reveal is Relentless Pursuit's gather → optional split: up to one land card goes onto the
 *   battlefield tapped, everything else (including any land not chosen) into the graveyard.
 * - The channel ability is Distorting Wake's "X target …" shape — exactly X targets — on a
 *   from-hand activation with `{X}{X}` in the cost (X counted once, paid twice). Unlike the
 *   channel lands it prints no legendary-creature discount.
 */
val ShigekiJukaiVisionary = card("Shigeki, Jukai Visionary") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Enchantment Creature — Snake Druid"
    power = 1
    toughness = 3
    oracleText = "{1}{G}, {T}, Return Shigeki to its owner's hand: Reveal the top four cards of " +
        "your library. You may put a land card from among them onto the battlefield tapped. Put " +
        "the rest into your graveyard.\n" +
        "Channel — {X}{X}{G}{G}, Discard this card: Return X target nonlegendary cards from your " +
        "graveyard to your hand."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.Tap, Costs.ReturnSelfToHand)
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(4), revealed = true)
            val (land, rest) = chooseUpToSplit(
                1,
                from = revealed,
                filter = GameObjectFilter.Land,
                prompt = "You may put a land card onto the battlefield tapped",
                selectedLabel = "Put onto battlefield",
                remainderLabel = "Put into graveyard",
                showAllCards = true
            )
            move(land, CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
            toGraveyard(rest)
        }
    }

    // Channel — {X}{X}{G}{G}, Discard this card (from hand): return exactly X nonlegendary cards.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}{X}{G}{G}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        targets(
            TargetFilter(GameObjectFilter.Any.ownedByYou().nonlegendary(), zone = Zone.GRAVEYARD),
            exactly = DynamicAmounts.xValue()
        )
        effect = Effects.Pipeline {
            val shigekiTargets = gather(CardSource.ChosenTargets)
            toHand(shigekiTargets)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "206"
        artist = "Anna Podedworna"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52a16623-7e6d-405b-95db-bf18aeea3f49.jpg?1783923841"
    }
}
