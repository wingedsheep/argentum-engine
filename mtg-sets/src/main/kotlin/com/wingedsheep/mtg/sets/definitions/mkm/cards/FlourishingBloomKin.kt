package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Flourishing Bloom-Kin — Murders at Karlov Manor #160
 * {1}{G} · Creature — Plant Elemental · 0/0
 *
 * This creature gets +1/+1 for each Forest you control.
 * Disguise {4}{G}
 * When this creature is turned face up, search your library for up to two Forest cards and reveal
 * them. Put one of them onto the battlefield tapped and the other into your hand, then shuffle.
 *
 * A printed 0/0 that stays alive only because of its own static ([GrantDynamicStatsEffect] over
 * [GroupFilter.source]) — it is a continuous effect recomputed in the projection, so playing or
 * losing a Forest resizes it immediately and the state-based-action check kills it the moment you
 * control none. Cast face down for {3} it is a plain 2/2 with ward {2} instead: a face-down permanent
 * has no abilities at all (CR 708.2), so the Forest count isn't applied and the 0/0 body never
 * appears.
 *
 * "Forest cards" is the **subtype**, not `BasicLand` — snow-covered Forests and nonbasic lands with
 * the Forest type (Bayou, Stomping Ground) are all legal finds, so the filter is
 * `Land.withSubtype(FOREST)`.
 *
 * The split search follows the Cultivate shape used by Bloomvine Regent's Claim Territory: gather,
 * `ChooseUpTo(2)` for the find, then a second `ChooseExactly(1)` whose *remainder* goes to hand.
 * That second step is what makes the official ruling fall out for free — find only one Forest and
 * `ChooseExactly(1)` auto-selects it onto the battlefield with an empty remainder, so you never get
 * the option to route the single card to your hand instead.
 *
 * Unlike [BubbleSmuggler]'s `disguiseFaceUpEffect`, this is a genuine **triggered** ability
 * ([Triggers.TurnedFaceUp], as on [EssenceOfAntiquity]): "When … is turned face up" uses the stack,
 * so the flip resolves first and opponents do get a window before the search happens.
 */
val FlourishingBloomKin = card("Flourishing Bloom-Kin") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Elemental"
    // Scryfall's oracle text for this card carries no disguise reminder text (three abilities left
    // the printed card no room), unlike the rest of the cycle — matched verbatim.
    oracleText = "This creature gets +1/+1 for each Forest you control.\n" +
        "Disguise {4}{G}\n" +
        "When this creature is turned face up, search your library for up to two Forest cards and " +
        "reveal them. Put one of them onto the battlefield tapped and the other into your hand, " +
        "then shuffle."
    power = 0
    toughness = 0

    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Land.withSubtype(Subtype.FOREST)
            ).count(),
            toughnessBonus = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Land.withSubtype(Subtype.FOREST)
            ).count()
        )
    }

    disguise = "{4}{G}"

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(
                        Zone.LIBRARY,
                        Player.You,
                        GameObjectFilter.Land.withSubtype(Subtype.FOREST)
                    ),
                    storeAs = "searchable"
                ),
                SelectFromCollectionEffect(
                    from = "searchable",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(2)),
                    storeSelected = "found",
                    prompt = "Search your library for up to two Forest cards"
                ),
                SelectFromCollectionEffect(
                    from = "found",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    storeSelected = "toBattlefield",
                    storeRemainder = "toHand",
                    selectedLabel = "Onto the battlefield tapped",
                    remainderLabel = "Into your hand",
                    prompt = "Choose which Forest enters the battlefield tapped; the other goes to your hand."
                ),
                MoveCollectionEffect(
                    from = "toBattlefield",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped),
                    revealed = true
                ),
                MoveCollectionEffect(
                    from = "toHand",
                    destination = CardDestination.ToZone(Zone.HAND),
                    revealed = true
                ),
                ShuffleLibraryEffect(),
                EmitLibrarySearchedEventEffect
            )
        )
        description = "When this creature is turned face up, search your library for up to two " +
            "Forest cards and reveal them. Put one of them onto the battlefield tapped and the " +
            "other into your hand, then shuffle."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Ben Hill"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5ddcb31e-9301-44f1-b138-0573fbf56a47.jpg?1783912867"

        ruling(
            "2024-02-02",
            "If you find only one Forest card with Flourishing Bloom-Kin's last ability, you'll put " +
                "it onto the battlefield tapped. You won't have the option to put it into your hand."
        )
        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to. Only a face-down permanent can " +
                "be turned face up this way; a face-down spell cannot."
        )
        ruling(
            "2024-02-02",
            "Because the permanent is on the battlefield both before and after it's turned face " +
                "up, turning a permanent face up doesn't cause any enters-the-battlefield " +
                "abilities to trigger."
        )
    }
}
