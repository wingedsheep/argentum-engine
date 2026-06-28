package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Choco, Seeker of Paradise
 * {1}{G}{W}{U}
 * Legendary Creature — Bird
 * 3/5
 *
 * Whenever one or more Birds you control attack, look at that many cards from the top of
 * your library. You may put one of them into your hand. Then put any number of land cards
 * from among them onto the battlefield tapped and the rest into your graveyard.
 * Landfall — Whenever a land you control enters, Choco gets +1/+0 until end of turn.
 *
 * Modeling notes:
 *  - The attack trigger is a once-per-combat group trigger ([Triggers.YouAttackWithFilter])
 *    keyed on Birds you control — it fires once no matter how many Birds attack, not once
 *    per Bird.
 *  - "Look at that many cards" — the count is the number of attacking Birds you control,
 *    read at resolution via [DynamicAmount.AggregateBattlefield] over attacking Birds. This
 *    matches the established convention for attacker-count cards (Goblin Piledriver,
 *    Shaleskin Bruiser).
 *  - The hand/battlefield/graveyard distribution is built from the atomic
 *    GatherCards → SelectFromCollection → MoveCollection pipeline (cf. Ignis Scientia,
 *    Portent of Calamity): "look at" is a private gather; the optional one-to-hand pick is a
 *    ChooseUpTo(1); the land pile is a ChooseAnyNumber filtered to lands (showAllCards so the
 *    player sees every looked-at card); everything not chosen falls through to the graveyard.
 */
val ChocoSeekerOfParadise = card("Choco, Seeker of Paradise") {
    manaCost = "{1}{G}{W}{U}"
    colorIdentity = "GWU"
    typeLine = "Legendary Creature — Bird"
    power = 3
    toughness = 5
    oracleText = "Whenever one or more Birds you control attack, look at that many cards from the top of your library. " +
        "You may put one of them into your hand. Then put any number of land cards from among them onto the " +
        "battlefield tapped and the rest into your graveyard.\n" +
        "Landfall — Whenever a land you control enters, Choco gets +1/+0 until end of turn."

    // Whenever one or more Birds you control attack, look at that many cards...
    triggeredAbility {
        trigger = Triggers.YouAttackWithFilter(GameObjectFilter.Creature.withSubtype(Subtype.BIRD))
        effect = Effects.Composite(
            listOf(
                // Look at that many cards from the top of your library.
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(
                        count = DynamicAmount.AggregateBattlefield(
                            Player.You,
                            GameObjectFilter.Creature.withSubtype(Subtype.BIRD).attacking()
                        ),
                        player = Player.You
                    ),
                    storeAs = "looked"
                ),
                // You may put one of them into your hand.
                SelectFromCollectionEffect(
                    from = "looked",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    storeSelected = "toHand",
                    storeRemainder = "remaining",
                    showAllCards = true,
                    prompt = "You may put one of them into your hand",
                    selectedLabel = "Put into your hand",
                    remainderLabel = "Keep among them"
                ),
                MoveCollectionEffect(
                    from = "toHand",
                    destination = CardDestination.ToZone(Zone.HAND, Player.You)
                ),
                // Then put any number of land cards from among them onto the battlefield tapped...
                SelectFromCollectionEffect(
                    from = "remaining",
                    selection = SelectionMode.ChooseAnyNumber,
                    filter = GameObjectFilter.Land,
                    showAllCards = true,
                    storeSelected = "toBattlefield",
                    storeRemainder = "toGraveyard",
                    prompt = "Put any number of land cards onto the battlefield tapped",
                    selectedLabel = "Onto the battlefield tapped",
                    remainderLabel = "Into your graveyard"
                ),
                MoveCollectionEffect(
                    from = "toBattlefield",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD, Player.You, ZonePlacement.Tapped)
                ),
                // ...and the rest into your graveyard.
                MoveCollectionEffect(
                    from = "toGraveyard",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.You)
                )
            )
        )
    }

    // Landfall — Whenever a land you control enters, Choco gets +1/+0 until end of turn.
    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = ModifyStatsEffect(1, 0, EffectTarget.Self)
        description = "Landfall — Whenever a land you control enters, Choco, Seeker of Paradise gets +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "215"
        artist = "Miho Midorikawa"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/409c305a-52dc-4538-8e72-efcd568eaf49.jpg?1748706564"
    }
}
